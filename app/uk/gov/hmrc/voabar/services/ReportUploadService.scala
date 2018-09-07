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
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{InputSource, Node, XML}

class ReportUploadService(statusRepository: SubmissionStatusRepository,
                          validationService: ValidationService,
                          legacyConnector: LegacyConnector)(implicit executionContext: ExecutionContext) {

  def upload(username: String, password: String, xml: String, uploadReference: String) = {

    for {
      _ <- statusRepository.updateStatus(uploadReference, "validating XML")
      _ <- validateXml(xml)
      _ <- statusRepository.updateStatus(uploadReference, "sending to ebars")
      _ <- ebarsUpload(xml, username, password, uploadReference)

    }yield("ok")

  }


  private def ebarsUpload(xml: String, username: String, password: String, submissionId: String): Future[String] = {

    val node = xmlToNode(xml)
    val nodesToSubmit = new XmlParser().oneReportPerBatch(node)
    val uploadResults = Future.sequence(nodesToSubmit.map(submitOneNode(_, username, password)))

    uploadResults.flatMap { results =>
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

  private def xmlToNode(xml: String) = {
    val factory = javax.xml.parsers.SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory.setFeature("http://xml.org/sax/features/external-general-entities",false)

    val saxParser = factory.newSAXParser()

    XML.loadXML(new InputSource(new StringReader(xml)), factory.newSAXParser())

  }

  private def validateXml(xml: String) = Future {
    val errors = validationService.validate(xml)
    if(errors.isEmpty) {
      "ok"
    } else {
      throw new RuntimeException("some errors" + errors.mkString(","))
    }
  }

}