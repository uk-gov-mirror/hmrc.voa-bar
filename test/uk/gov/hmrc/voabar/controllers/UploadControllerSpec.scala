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

  "Return status 200 (OK) for a post carrying xml" in {
    val result = controller.upload()(fakeRequestWithXML)
    result.map{ x => println(x.header)}
    status(result) mustBe 200
  }

  "Return 415 (Unsupported Media Type) when given a content type that is not xml" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("Content-Type" -> "application/text"))
    status(result) mustBe 415
  }

  "Return 400 (Bad Request) when given a content type that is xml but no xml is given" in {
    val result = controller.upload()(FakeRequest("POST", "/upload")
      .withHeaders("Content-Type" -> "application/xml"))
    status(result) mustBe 400
  }

  "A request must contain a Billing Authority Code in the header" in {
    fakeRequestWithXML.headers.get("BA-Code") mustBe Some("1234")
  }

  "A unique id is generated for each xml submission" in {
    val id:Option[String] = controller.generateSubmissionID(fakeRequestWithXML)
    id.isDefined mustBe true
  }
}
