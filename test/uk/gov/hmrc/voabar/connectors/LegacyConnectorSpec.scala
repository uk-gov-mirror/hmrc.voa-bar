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
import org.apache.commons.codec.binary.Base64
import uk.gov.hmrc.crypto.{ApplicationCryptoDI, PlainText}

class LegacyConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  def injector: Injector = app.injector
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val crypto = new ApplicationCryptoDI(configuration).JsonCrypto
  implicit val hc = mock[HeaderCarrier]

  val username = "ba0121"
  val password = "wibble"
  val encryptedPassword = crypto.encrypt(PlainText(password)).value

  val goodLogin = LoginDetails(username, encryptedPassword)
  val baReport = "BAReport"

  def getHttpMock(returnedStatus: Int): HttpClient = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
      any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    httpMock
  }

  "decryptPassword" must {
    "Decrypt the  encrypted password and return it in plain text" in {
      val httpMock = getHttpMock(Status.OK)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val decryptedPassword = connector.decryptPassword(encryptedPassword)
      decryptedPassword mustBe password
    }
  }

  "LegacyConnector " must {
    "Send the contact details returning a 200 when it succeeds" in {
      val httpMock = getHttpMock(Status.OK)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val result = await(connector.validate(goodLogin))
      result.isSuccess mustBe true
      result.get mustBe Status.OK
    }

    "return a failure representing the error when the send contact details method fails" in {
      val httpMock = getHttpMock(Status.INTERNAL_SERVER_ERROR)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val result = await(connector.validate(goodLogin))

      result.isFailure mustBe true
    }

    "return a 200 when an BA report upload request is successful" in {
      val httpMock = getHttpMock(Status.OK)
      val connector = new LegacyConnector(httpMock, configuration, environment)

      val result = await(connector.sendBAReport(baReport))

      result.isSuccess mustBe true
      result.get mustBe Status.OK
    }

    "return an internal servererror when an BA report upload request fails" in {
      val httpMock = getHttpMock(Status.INTERNAL_SERVER_ERROR)
      val connector = new LegacyConnector(httpMock, configuration, environment)

      val result = await(connector.sendBAReport(baReport))

      result.isFailure mustBe true
    }

    "provided with JSON directly" must {
      "call the Microservice" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val httpMock = getHttpMock(Status.OK)

        val connector = new LegacyConnector(httpMock, configuration, environment)
        connector.validate(goodLogin)

        verify(httpMock).GET(urlCaptor.capture)(any(), any(), any())
        urlCaptor.getValue must endWith("Welcome.do")
      }

      "return a 200 if the data transfer call is successful" in {
        val connector = new LegacyConnector(getHttpMock(Status.OK), configuration, environment)
        val result = await(connector.validate(goodLogin))
        result.isSuccess mustBe true
        result.get mustBe Status.OK
      }

      "throw an failure if the data transfer call fails" in {
        val connector = new LegacyConnector(getHttpMock(Status.INTERNAL_SERVER_ERROR), configuration, environment)
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
      val httpMock = getHttpMock(Status.OK)
      val connector = new LegacyConnector(httpMock, configuration, environment)
      val hc = connector.generateHeader(goodLogin)

      val encodedAuthHeader = Base64.encodeBase64String(s"${goodLogin.username}:${password}".getBytes("UTF-8"))

      hc.authorization match {
        case Some(s) => hc.authorization.isDefined mustBe true
          s.toString.equals(s"Authorization(Basic ${encodedAuthHeader})") mustBe true
        case _ => assert(false)
      }
    }
  }

}
