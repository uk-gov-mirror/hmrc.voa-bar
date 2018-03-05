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

class UploadControllerSpec extends PlaySpec with MockitoSugar {

  val controller = new UploadController

  def fakeRequestWithXML(xmlStr: String) = {
    val xmlNodes = scala.xml.XML.loadString(xmlStr)
    FakeRequest("POST", "").withHeaders("Content-Type" -> "application/xml").withXmlBody(xmlNodes)
  }

  "Return status 200 for a post carrying xml" in {
    val result = controller.upload()(fakeRequestWithXML("""<xml>Wibble</xml>"""))
    status(result) mustBe 200
  }

  "Return 406 when given a content type that is not xml" in {
    val result = controller.upload()(FakeRequest("POST", "/upload").withHeaders("Content-Type" -> "application/text"))
    status(result) mustBe 406
  }

  "Return 400 when given a content type that is xml but no xml is given" in {
    val result = controller.upload()(FakeRequest("POST", "/upload").withHeaders("Content-Type" -> "application/xml"))
    status(result) mustBe 400
  }
}
