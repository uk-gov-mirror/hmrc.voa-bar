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


import cats.data.EitherT
import play.api.Logger
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models._
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Node

class ReportUploadService(statusRepository: SubmissionStatusRepository,
                          validationService: ValidationService,
                          xmlParser: XmlParser,
                          legacyConnector: LegacyConnector)(implicit executionContext: ExecutionContext) {


  def upload(username: String, password: String, xml: String, uploadReference: String) = {

    val processingResutl = for {
      _ <- EitherT(statusRepository.updateStatus(uploadReference, "validation"))
      validationResult <- EitherT.fromEither(validationService.validate(xml))
      node <- xmlParser.xmlToNode(xml)
      _ <- EitherT(statusRepository.updateStatus(uploadReference, "sending to eBARS"))
      ebarsResult <- EitherT(ebarsUpload(node, username, password, uploadReference))
      _ <- EitherT(statusRepository.updateStatus(uploadReference, "done"))
    } yield ("ok")

    processingResutl.value.map {
      case Right(v) => // do nothing, everything is awesome
      case Left(a) => handleError(uploadReference, a)
    }

  }


  private def handleError(submissionId: String, barError: BarError): Unit = barError match {
    case BarXmlError(message) => {
      statusRepository.updateStatus(submissionId, "xml vadation failed")  //TODO handle this errors ????
    }
    case BarValidationError(errors) => statusRepository.updateStatus(submissionId, "business rules vation failed")
    case BarEbarError(ebarError) => statusRepository.updateStatus(submissionId, "eBARS submission failed")
    case BarMongoError(error, updateWriteResult) => {
      //Something really, really bad, bad bad, we don't have mongo :(
      Logger.warn(s"Mongo exception while updating status, submissionId: ${submissionId}, detail : ${updateWriteResult}")
    }

  }


  private def ebarsUpload(node: Node, username: String, password: String, submissionId: String): Future[Either[BarError, Boolean]] = {

    val nodesToSubmit = xmlParser.oneReportPerBatch(node)

    //TODO - change to Akka Streams to properly limit concurency and back pressure.
    val uploadResults = Future.sequence(nodesToSubmit.map(submitOneNode(_, username, password)))

    uploadResults.map { results =>
      if (results.find(_.isFailure).isDefined) {
        Left(BarEbarError("failed to upload everything"))
      } else {
        Right(true)
      }
    }
  }

  def submitOneNode(node: Node, username: String, password: String) = {
    val req = BAReportRequest("uuid", node.toString(), username, password)(null)
    legacyConnector.sendBAReport(req)
  }


}