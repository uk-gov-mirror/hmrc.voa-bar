/*
 * Copyright 2018 HM Revenue & Customs
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


import java.io.StringWriter

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import play.api.Logger
import services.EbarsValidator
import uk.gov.hmrc.voabar.connectors.{EmailConnector, LegacyConnector}
import uk.gov.hmrc.voabar.models._
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository
import uk.gov.hmrc.voabar.util._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml.{Node, XML}

class ReportUploadService @Inject()(statusRepository: SubmissionStatusRepository,
                          validationService: ValidationService,
                          xmlParser: XmlParser,
                          legacyConnector: LegacyConnector,
                          emailConnector: EmailConnector)(implicit executionContext: ExecutionContext) {
  val ebarsValidator = new EbarsValidator()

  private def numberReports(node: Node): Int = {
    node \ "BApropertyReport" length
  }

  def upload(username: String, password: String, xml: String, uploadReference: String) = {


    val processingResult = for {
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Pending))
      _ <- EitherT.fromEither[Future](validationService.validate(xml, username))
      node <- EitherT.fromEither[Future](xmlParser.xmlToNode(xml))
      _ <- EitherT(statusRepository.update(uploadReference, Verified, numberReports(node)))
      _ <- EitherT(ebarsUpload(node, username, password, uploadReference))
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Done))
      _ <- EitherT(sendConfirmationEmail(uploadReference, username, password))
    } yield ("ok")

    processingResult.value.map {
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
          Logger.error(errorMsg, ex)
          Right(Unit)
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
          Logger.error(errorMsg)
          Future.successful(Right(Unit))
        },
        reportStatus => sendConfirmationEmail(reportStatus, username, password)
      ))
  }

  private def handleError(submissionId: String, barError: BarError, username: String, password: String): Unit = {
    Logger.warn(s"handling error, submissionID: ${submissionId}, Error: ${barError}")

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
        Future.sequence(errors.map(x => statusRepository.addError(submissionId, x)))
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
        Logger.warn(s"Mongo exception, unable to update status of submission, submissionId: ${submissionId}, detail : ${updateWriteResult}")
      }
      case BarEmailError(emailError) => {
        statusRepository.addError(submissionId, Error(UNKNOWN_ERROR, Seq(emailError)))
        statusRepository.updateStatus(submissionId, Failed)
          .map(_ => sendConfirmationEmail(submissionId, username, password))
      }
    }
  }


  private def ebarsUpload(node: Node, username: String, password: String, submissionId: String): Future[Either[BarError, Boolean]] = {

    val buff = new StringWriter()
    XML.write(buff, node, "UTF-8", true, null)
    val xmlString = buff.toString
    val jaxbElement = ebarsValidator.fromXml(xmlString)
    val jsonString = ebarsValidator.toJson(jaxbElement)

    val req = BAReportRequest(submissionId, jsonString, username, password)(null) //TODO Fix uuid
    legacyConnector.sendBAReport(req).map {
      case Success(value) => Right(true)
      case Failure(exception) => Left(BarEbarError(exception.getMessage))
    }

  }

}