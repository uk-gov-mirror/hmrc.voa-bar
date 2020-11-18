/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.voabar.services

import java.io.ByteArrayInputStream

import cats.data.EitherT
import cats.implicits._
import ebars.xml.BAreports
import javax.inject.Inject
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource
import models.Purpose
import org.w3c.dom.Document
import play.api.Logger
import services.EbarsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voabar.connectors.{EmailConnector, LegacyConnector, UpscanConnector}
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.models._
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository
import uk.gov.hmrc.voabar.util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ReportUploadService @Inject()(statusRepository: SubmissionStatusRepository,
                          validationService: ValidationService,
                          submissionProcessingService: SubmissionProcessingService,
                          legacyConnector: LegacyConnector,
                          emailConnector: EmailConnector, upscanConnector: UpscanConnector)(implicit executionContext: ExecutionContext) {
  val ebarsValidator = new EbarsValidator()

  val logger = Logger(this.getClass)

  def upload(username: String, password: String, xmlUrl: String, uploadReference: String)(implicit headerCarrier: HeaderCarrier) = {

    val baLogin = BaLogin(username, password)

    val processingResult = for {
      submissions <- downloadAndFixXml(xmlUrl)
      _ <- EitherT.fromEither[Future](validationService.validate(submissions, baLogin))
      _ <- EitherT(ebarsUpload(submissions, username, password, uploadReference))
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Done))
      _ <- EitherT(sendConfirmationEmail(uploadReference, username, password))
    } yield ("ok")

    processingResult.value
        .recover {
          case exception: Exception => {
            logger.warn("Unexpected error when processing file, trying to recover", exception)
            Left(UnknownError(exception.getMessage))
          }
        }
      .map {
      case Right(v) => "ok"
      case Left(a) => {
        handleError(uploadReference, a, username, password)
        "failed"
      }
    }
  }


  /**
   * Before we merge all validation from V1 and V2, we are doing multiple conversion
   * between XML, JAXB, DomTree.
   * After merge, please return BAreports
   * @param url
   * @return
   */
  def downloadAndFixXml(url: String)(implicit hc:HeaderCarrier) = {
    import scala.collection.JavaConverters._
    val correctionEngine = new RulesCorrectionEngine

    def parseXml(rawXml: Array[Byte]): Either[BarError, BAreports] = Try {
      val source = new StreamSource(CorrectionInputStream(new ByteArrayInputStream(rawXml)))
      ebarsValidator.fromXml(source)
    }.toEither.leftMap { e =>
      logger.warn(s"Unable to parse XML", e)
      BarXmlError(e.getMessage)
    }

    def fixXml(submission: BAreports):Either[BarError, BAreports] = Try {

      val allReports = ebarsValidator.split(submission)

      allReports.foreach { report =>
        correctionEngine.applyRules(report)
      }

      submission.getBApropertyReport.clear()

      submission.getBApropertyReport.addAll(allReports.map(_.getBApropertyReport.get(0)).toList.asJava)

      FixHeader(submission)
      FixCTaxTrailer(submission)

      submission

    }.toEither.leftMap { e =>
      logger.warn("Unable to automatically fix XML", e)
      UnknownError("Unable to process upload")
    }

    for {
      rawXml <- EitherT(upscanConnector.downloadReport(url))
      submission <- EitherT.fromEither[Future](parseXml(rawXml))
      xml <- EitherT.fromEither[Future](fixXml(submission))
    } yield (xml)

  }

  //TODO - After merge of validation from V1 and V2, please use this method to send all data.
  def upload(username: String, password: String, baReports: BAreports, uploadReference: String)(implicit headerCarrier: HeaderCarrier) = {
    val processingResult = for {
      _ <- EitherT(statusRepository.update(uploadReference, Verified, baReports.getBAreportTrailer.getRecordCount.intValue()))
      _ <- EitherT(ebarsUpload(baReports, username, password, uploadReference))
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Done))
      _ <- EitherT(sendConfirmationEmail(uploadReference, username, password))
    } yield ("ok")

    processingResult.value
      .recover {
        case exception: Exception => {
          logger.warn("Unexpected error when processing file, trying to recover", exception)
          Left(UnknownError(exception.getMessage))
        }
      }
      .map {
        case Right(v) => "ok"
        case Left(a) => {
          handleError(uploadReference, a, username, password)
          "failed"
        }
      }
  }

  private def sendConfirmationEmail(
                                   reportStatus: ReportStatus,
                                   username: String,
                                   password: String
                                   ): Future[Either[BarEmailError, Unit.type]] = {
    emailConnector.sendEmail(
      reportStatus.baCode.getOrElse("Unknown BA"),
      Purpose.CT, // Note: This will need to be dynamic when NDR processing is added to the service
      reportStatus.id,
      username,
      password,
      reportStatus.filename.getOrElse("filename unavailable"),
      reportStatus.created.toString,
      reportStatus.errors.getOrElse(Seq()).map(e => s"${e.code}: ${e.values.mkString("\n")}").mkString("\n"))
      .map(_ => Right(Unit))
      .recover{
        case ex: Throwable => {
          val errorMsg = "Error while sending confirmation message"
          logger.error(errorMsg, ex)
          Left(BarEmailError(ex.getMessage))
        }
      }
  }
  private def sendConfirmationEmail(
                                     baRef: String,
                                     username: String,
                                     password: String): Future[Either[BarError, Unit.type]] = {
    statusRepository.getByReference(baRef).flatMap(_.fold(
        e => {
          val errorMsg = "Error while retrieving report to be send via email"
          logger.error(errorMsg)
          Future.successful(Right(Unit))
        },
        reportStatus => sendConfirmationEmail(reportStatus, username, password)
      ))
  }

  private def handleError(submissionId: String, barError: BarError, username: String, password: String): Unit = {
    logger.warn(s"handling error, submissionID: ${submissionId}, Error: ${barError}")

    barError match {
      case BarXmlError(message) => {
        statusRepository.addError(submissionId, Error(INVALID_XML, Seq(message)))
        statusRepository.updateStatus(submissionId, Failed)
          .map(_ => sendConfirmationEmail(submissionId, username, password))
      }

      case BarXmlValidationError(errors) => {
        Future.sequence(errors.map(x => statusRepository.addError(submissionId, x)))
        statusRepository.updateStatus(submissionId, Failed)
          .map(_ => sendConfirmationEmail(submissionId, username, password))
      }

      case BarValidationError(errors) => {
        Future.sequence(errors.map(x => statusRepository.addError(submissionId, x))) //TODO add errors + flatMap
        statusRepository.updateStatus(submissionId, Failed)
          .map(_ => sendConfirmationEmail(submissionId, username, password))
      }

      case BarEbarError(ebarError) => {
        statusRepository.addError(submissionId, Error(EBARS_UNAVAILABLE, Seq(ebarError)))
        statusRepository.updateStatus(submissionId, Failed)
          .map(_ => sendConfirmationEmail(submissionId, username, password))
      }
      case BarMongoError(error, updateWriteResult) => {
        //Something really, really bad, bad bad, we don't have mongo :(
        logger.warn(s"Mongo exception, unable to update status of submission, submissionId: ${submissionId}, detail : ${updateWriteResult}")
      }
      case BarEmailError(emailError) => {
        statusRepository.addError(submissionId, Error(UNKNOWN_ERROR, Seq(emailError))) //TODO probably put WARNING about email submission
        statusRepository.updateStatus(submissionId, Done)
      }
      case UnknownError(detail) => {
        statusRepository.addError(submissionId, Error(UNKNOWN_ERROR, Seq(detail)))
        statusRepository.updateStatus(submissionId, Failed)
          .map(_ => sendConfirmationEmail(submissionId, username, password))
      }
    }
  }


  private def ebarsUpload(domDocument: Document, username: String, password: String, submissionId: String)(implicit headerCarrier: HeaderCarrier) : Future[Either[BarError, Boolean]] = {
    Future {
      Try {
        ebarsValidator.fromXml(new DOMSource(domDocument)) //blocking operation
      } match {
        case Success(baReports) => ebarsUpload(baReports, username, password, submissionId)
        case Failure(exception) => Future.successful(Left(BarEbarError(exception.getMessage)))
      }
    }.flatten

  }

  private def ebarsUpload(baReports: BAreports, username: String, password: String, submissionId: String)(implicit headerCarrier: HeaderCarrier) : Future[Either[BarError, Boolean]] = {

    val riskyConversion: Either[BarError, String] = Try {
      ebarsValidator.toJson(baReports)
    } match {
      case Success(jsonString) => Right(jsonString)
      case Failure(exception) => Left(BarEbarError(exception.getMessage))
    }
    

    def internall_upload(jsonString: String):Future[Either[BarError, Boolean]] = {
      val req = BAReportRequest(submissionId, jsonString, username, password)
      legacyConnector.sendBAReport(req).map(_ => Right(true)).recover {
        case ex: Exception => Left(BarEbarError(ex.getMessage))
      }
    }

    val resutl = for {
      jsonString <- EitherT.fromEither[Future](riskyConversion)
      result <- EitherT(internall_upload(jsonString))
    } yield(result)

    resutl.value
  }



}