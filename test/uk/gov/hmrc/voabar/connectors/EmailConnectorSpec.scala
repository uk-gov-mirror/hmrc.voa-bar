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

package uk.gov.hmrc.voabar.connectors

import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import play.api.{Configuration, Environment}
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.voabar.models.Email
import uk.gov.hmrc.voabar.utils.Initialize
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import play.api.test.Helpers._
import uk.gov.hmrc.voabar.base.SpecBase

class EmailConnectorSpec extends SpecBase {

  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val init = injector.instanceOf[Initialize]

  val baRefNumber = "13432121232"
  val fileName = "sample.xml"
  val dateSubmitted = "2017-08-30"
  val errorList = ""
  val email = Email(baRefNumber, fileName, dateSubmitted, errorList, init)

  val minimalJson = Json.toJson(email)

  "EmailConnector" when {
    "provided with Email model input" must {
      "Send the email returning a 200 when the email service succeeds" in {
        val httpMock = getHttpMock(202)
        val connector = new EmailConnector(httpMock, configuration, environment)
        val result = await(connector.sendEmail(email))

        result.isSuccess mustBe true
        result.get mustBe 200
      }

      "return a failure representing the error when send method fails" in {
        val httpMock = getHttpMock(500)
        val connector = new EmailConnector(httpMock, configuration, environment)
        val result = await(connector.sendEmail(email))
        result.isFailure mustBe true
      }
    }

    "provided with JSON directly" must {
      "call the Microservice with the given JSON" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[Any]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(200)

        val connector = new EmailConnector(httpMock, configuration, environment)
        await(connector.sendJson(minimalJson))

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any())
        urlCaptor.getValue must endWith(s"${connector.domain}email")
        bodyCaptor.getValue mustBe minimalJson
        headersCaptor.getValue mustBe Seq(connector.jsonContentTypeHeader)
      }

      "return a 200 if the email service call is successful" in {
        val connector = new EmailConnector(getHttpMock(202), configuration, environment)
        val result = await(connector.sendJson(minimalJson))

        result mustBe Success(200)
        }
      }

      "throw an failure if the email service call fails" in {
        val connector = new EmailConnector(getHttpMock(500), configuration, environment)
        val result = await(connector.sendJson(minimalJson))
          assert(result.isFailure)
        }
      }

      "return a failure if the email service call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
          any[HeaderCarrier], any())) thenReturn  Future.successful(new RuntimeException)
        val connector = new EmailConnector(httpMock, configuration, environment)
        val result = await(connector.sendJson(minimalJson))
          assert(result.isFailure)
      }

}
