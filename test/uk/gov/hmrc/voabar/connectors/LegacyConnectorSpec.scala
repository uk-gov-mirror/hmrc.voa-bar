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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.inject.Injector
import play.api.libs.json.{JsValue, Writes}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.voabar.models.LoginDetails

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._

class LegacyConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  def injector: Injector = app.injector
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val goodLogin = LoginDetails("ba0121", "wibble")

  def getHttpMock(returnedStatus: Int): HttpClient = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
      any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    httpMock
  }

  "LegacyConnector " must {
    "Send the contact details returning a 200 when it succeeds" in {
      val httpMock = getHttpMock(200)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val result = await(connector.validate(goodLogin))
      result.isSuccess mustBe true
      result.get mustBe 200
    }

    "return a failure representing the error when send method fails" in {
      val httpMock = getHttpMock(500)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val result = await(connector.validate(goodLogin))

      result.isFailure mustBe true
    }

    "provided with JSON directly" must {
      "call the Microservice" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val httpMock = getHttpMock(200)

        val connector = new LegacyConnector(httpMock, configuration, environment)
        connector.validate(goodLogin)

        verify(httpMock).GET(urlCaptor.capture)(any(), any(), any())
        urlCaptor.getValue must endWith("Welcome.do")
      }

      "return a 200 if the data transfer call is successful" in {
        val connector = new LegacyConnector(getHttpMock(200), configuration, environment)
        val result = await(connector.validate(goodLogin))
        result.isSuccess mustBe true
        result.get mustBe 200
      }

      "throw an failure if the data transfer call fails" in {
        val connector = new LegacyConnector(getHttpMock(500), configuration, environment)
        val result = await(connector.validate(goodLogin))
        assert(result.isFailure)
      }

      "return a failure if the data transfer call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(new RuntimeException)
        val connector = new LegacyConnector(httpMock, configuration, environment)
        val result = await(connector.validate(goodLogin))
        assert(result.isFailure)
      }
    }
  }

  "The generateHeaderCarrier method " must {

    "include some basic authorization in the header" in {
      val httpMock = getHttpMock(200)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val hc = connector.generateHeader(goodLogin)

      hc.authorization.isDefined mustBe true
      println(hc.authorization.get)
      hc.authorization.get.toString.startsWith("Authorization(Basic") mustBe true
    }
  }

}
