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

import com.typesafe.config.ConfigException
import javax.inject.{Inject, Singleton}
import org.apache.commons.codec.binary.Base64
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import play.mvc.Http.Status
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.http.logging._
import uk.gov.hmrc.voabar.models.LoginDetails

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext

@Singleton
class LegacyConnector @Inject()(val http: HttpClient,
                                val configuration: Configuration,
                                environment: Environment) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  lazy val crypto = ApplicationCrypto.JsonCrypto

  val legacyConnectorUrlPath = "legacy-ebars-client.baseUrl"
  val baseUrl = runModeConfiguration.getString(legacyConnectorUrlPath) match {
    case Some(url) => url
    case _ => throw new ConfigException.Missing(legacyConnectorUrlPath)
  }

  def decryptPassword(password: String) : String = crypto.decrypt(Crypted(password)).value


  def generateHeader(loginDetails: LoginDetails)(implicit ec: ExecutionContext): HeaderCarrier = {
    val decryptedPassword = decryptPassword(loginDetails.password)
    val encodedAuthHeader = Base64.encodeBase64String(s"${loginDetails.username}:${decryptedPassword}".getBytes("UTF-8"))
    HeaderCarrier(authorization = Some(Authorization(s"Basic $encodedAuthHeader")))
  }

  def validate(loginDetails: LoginDetails)(implicit ec: ExecutionContext): Future[Try[Int]] = {
    implicit val authHc = generateHeader(loginDetails)

    http.GET(s"${baseUrl}/ebars_dmz_pres_ApplicationWeb/Welcome.do").map { response =>
      response.status match {
        case(200) => Success(200)
        case status => Failure(new RuntimeException("Login attempt fails with username = " + loginDetails.username + ", password = " + loginDetails.password))
      }
    } recover {
      case ex =>
        Logger.warn("Legacy validation fails with exception " + ex.getMessage)
        Failure(new RuntimeException("Legacy validation fails with exception " + ex.getMessage))
    }
  }

  def sendBAReport(baReports: Any)(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Try[Int]] = {
    http.POST(s"${baseUrl}/ebars_dmz_pres_ApplicationWeb/UploadAction.do", "", Seq()).map{ response =>
      response.status match {
        case(Status.OK) => Success(Status.OK)
      }
    } recover {
      case ex =>
        val errorMsg = "Couldn't send BA Reports"
        Logger.warn(s"$errorMsg\n${ex.getMessage}")
        Failure(new RuntimeException(errorMsg))
    }
  }
}

