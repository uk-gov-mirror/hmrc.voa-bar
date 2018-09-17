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

package uk.gov.hmrc.voabar.controllers

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.voabar.services.ReportUploadService
import scala.concurrent.ExecutionContext.Implicits.global


class UploadControllerSpec extends PlaySpec with MockitoSugar {

  val reportUploadService = mock[ReportUploadService]

  val controller = new UploadController(reportUploadService)

  def fakeRequestWithXML = {
    val xmlNode = """<xml>Wibble</xml>"""
    FakeRequest("POST", "/request?reference=1234")
      .withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Length" -> s"${xmlNode.length}",
        "BA-Code" -> "1234",
        "password" -> "pass1")
        .withTextBody(xmlNode)
  }

  def fakeRequestWithXMLButNoBACode = {
    val xmlNode = """<xml>Wibble</xml>"""
    FakeRequest("POST", "")
      .withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Length" -> s"${xmlNode.length}")
      .withTextBody(xmlNode)
  }

  def fakeRequestWithXMLButNoPassword = {
    val xmlNode = """<xml>Wibble</xml>"""
    FakeRequest("POST", "")
      .withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Length" -> s"${xmlNode.length}",
        "BA-Code" -> "1234")
      .withTextBody(xmlNode)
  }

  "Checking the incoming XML returns a unit" ignore {
    //val unit: Unit = ()
    //await(controller.checkXml("", "bacode", "password", "submissionid")) mustBe unit
  }

   "Uploading an xml file records that the report was submitted" ignore {
//    fakeHistoryService.clearCaptures()
//    val result = await(controller.upload()(fakeRequestWithXML))
//    fakeHistoryService.reportIsSubmittedCalled mustBe true
  }

  "Uploading an xml file records either that the file was checked without errors or the file was checked and errors were found" ignore {
//    fakeHistoryService.clearCaptures()
//    val result = await(controller.upload()(fakeRequestWithXML))
//    fakeHistoryService.reportIsCheckedWithErrorsFound || fakeHistoryService.reportIsCheckedWithoutErrorsFound mustBe true
  }


  "Return status 200 (OK) for a post carrying xml" in {
    val result = controller.upload()(fakeRequestWithXML)
    status(result) mustBe 200
  }

  "Return 415 (Unsupported Media Type) when the Content-Type header value is not text/plain" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("Content-Type" -> "application/text"))
    status(result) mustBe 415
  }

  "Return 400 (Bad Request) when given a content type that is text/plain but no text is given" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("Content-Type" -> "text/plain", "BA-Code" -> "1234", "password" -> "pass1"))
    status(result) mustBe 400
  }

  "Return 415 (Unsupported Media Type) when a request contains no content type" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("BA-Code" -> "1234", "password" -> "pass1"))
    status(result) mustBe 415
  }

  "A request must contain a Billing Authority Code in the header" in {
    val result = controller.upload()(fakeRequestWithXMLButNoBACode)
    status(result) mustBe UNAUTHORIZED
  }

  "A request must contain a password in the header" in {
    val result = controller.upload()(fakeRequestWithXMLButNoPassword)
    status(result) mustBe UNAUTHORIZED
  }

  "An id is generated for each xml submission" ignore { //Probably not relevant anymore.
    val result = controller.upload()(fakeRequestWithXML)
    status(result) mustBe 200
    contentAsString(result).matches("^\\d+-\\d+-[A-Z][A-Z]$") mustBe true
  }
}
