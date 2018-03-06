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
import scala.concurrent.ExecutionContext.Implicits.global

class UploadControllerSpec extends PlaySpec with MockitoSugar {

  val controller = new UploadController

  def fakeRequestWithXML = {
    val xmlNode = scala.xml.XML.loadString("""<xml>Wibble</xml>""")
    FakeRequest("POST", "")
      .withHeaders(
        "Content-Type" -> "application/xml",
        "Content-Length" -> s"${xmlNode.length}",
        "BA-Code" -> "1234")
      .withXmlBody(xmlNode)
  }

  def fakeRequestWithXMLButNoBACode = {
    val xmlNode = scala.xml.XML.loadString("""<xml>Wibble</xml>""")
    FakeRequest("POST", "")
      .withHeaders(
        "Content-Type" -> "application/xml",
        "Content-Length" -> s"${xmlNode.length}")
      .withXmlBody(xmlNode)
  }

  "Return status 200 (OK) for a post carrying xml" in {
    val result = controller.upload()(fakeRequestWithXML)
    status(result) mustBe 200
  }

  "Return 415 (Unsupported Media Type) when given a content type that is not xml" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("Content-Type" -> "application/text"))
    status(result) mustBe 415
  }

  "Return 400 (Bad Request) when given a content type that is xml but no xml is given" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("Content-Type" -> "application/xml", "BA-Code" -> "1234"))
    status(result) mustBe 400
  }

  "A request must contain a Billing Authority Code in the header" in {
    val result = controller.upload()(fakeRequestWithXMLButNoBACode)
    status(result) mustBe UNAUTHORIZED
  }

  "An id is generated for each xml submission" in {
    val result = controller.upload()(fakeRequestWithXML)
    status(result) mustBe 200
    contentAsString(result).matches("^\\d+-\\d+-[A-Z][A-Z]$") mustBe true
  }
}
