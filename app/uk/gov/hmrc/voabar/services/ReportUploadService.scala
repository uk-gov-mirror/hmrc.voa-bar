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

import java.io.StringReader

import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.BarError
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{InputSource, Node, XML}

class ReportUploadService(statusRepository: SubmissionStatusRepository,
                          validationService: ValidationService,
                          xmlParser: XmlParser,
                          legacyConnector: LegacyConnector)(implicit executionContext: ExecutionContext) {

  def upload(username: String, password: String, xml: String, uploadReference: String) = {

    for {
      //TODO update status - "validation"
      validationResult <- validationService.validate(xml)
      //TODO update status  - "upload to eBars"
      node <- xmlParser.xmlToNode(xml)
      _ <- ebarsUpload(node, username, password, uploadReference)
      //TODO update status - "done"
    }yield("ok")

    //TODO handle failure -> add all errors + change status "failed"

  }


  private def ebarsUpload(node: Node, username: String, password: String, submissionId: String): Either[BarError, Boolean] = {

    val nodesToSubmit = xmlParser.oneReportPerBatch(node)
    val uploadResults = Future.sequence(nodesToSubmit.map(submitOneNode(_, username, password)))

    val uplodResult = uploadResults.flatMap { results =>
      if(results.find(_.isFailure).isDefined) {
        statusRepository.updateStatus(submissionId, "Failed").flatMap( x =>
          Future.failed(new RuntimeException("ebars failed"))
        )
      } else {
        statusRepository.updateStatus(submissionId, "done").map(_ => "ok")
      }
    }


  }

  def submitOneNode(node: Node, username: String, password: String) = {
    val req = BAReportRequest("uuid", node.toString(),username, password)(null)

    legacyConnector.sendBAReport(req)
  }


}