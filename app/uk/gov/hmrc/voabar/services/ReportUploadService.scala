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
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models._
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.xml.{Node, XML}

class ReportUploadService @Inject()(statusRepository: SubmissionStatusRepository,
                          validationService: ValidationService,
                          xmlParser: XmlParser,
                          legacyConnector: LegacyConnector)(implicit executionContext: ExecutionContext) {

  def upload(username: String, password: String, xml: String, uploadReference: String) = {


    val processingResutl = for {
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Pending))
      _ <- EitherT.fromEither[Future](validationService.validate(xml))
      node <- EitherT.fromEither[Future](xmlParser.xmlToNode(xml))
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Verified))
      _ <- EitherT(ebarsUpload(node, username, password, uploadReference))
      _ <- EitherT(statusRepository.updateStatus(uploadReference, Done))
    } yield ("ok")

    processingResutl.value.map {
      case Right(v) => "ok"
      case Left(a) => {
        handleError(uploadReference, a)
        "failed"
      }
    }
  }


  private def handleError(submissionId: String, barError: BarError): Unit = {
    Logger.warn(s"handling error, submissionID: ${submissionId}, Error: ${barError}")

    barError match {
      case BarXmlError(message) => {
        statusRepository.updateStatus(submissionId, Failed)
      }
      case BarValidationError(errors) => statusRepository.updateStatus(submissionId, Failed)
      case BarEbarError(ebarError) => statusRepository.updateStatus(submissionId, Failed)
      case BarMongoError(error, updateWriteResult) => {
        //Something really, really bad, bad bad, we don't have mongo :(
        Logger.warn(s"Mongo exception, cannot updating status or record error, submissionId: ${submissionId}, detail : ${updateWriteResult}")
      }
    }
  }


  private def ebarsUpload(node: Node, username: String, password: String, submissionId: String): Future[Either[BarError, Boolean]] = {

    val nodesToSubmit = xmlParser.oneReportPerBatch(node)

    //TODO - change to Akka Streams to properly limit concurency and back pressure.
    val uploadResults = Future.sequence(nodesToSubmit.map( oneNode => submitOneNode(oneNode, username, password)))

    uploadResults.map { results =>
      if (results.find(_.isFailure).isDefined) {
        Left(BarEbarError("failed to upload everything"))
      } else {
        Right(true)
      }
    }
  }

  def submitOneNode(node: Node, username: String, password: String) = {
    val result = Try {
      val buff = new StringWriter()
      XML.write(buff, node, "UTF-8", true, null)
      val xmlString = buff.toString

      val req = BAReportRequest("uuid", xmlString, username, password)(null) //TODO Fix uuid
      legacyConnector.sendBAReport(req)
    }

    result.get

  }


}