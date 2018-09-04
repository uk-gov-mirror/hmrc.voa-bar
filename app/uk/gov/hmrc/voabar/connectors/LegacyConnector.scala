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

import com.google.inject.ImplementedBy
import com.typesafe.config.ConfigException
import javax.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import play.mvc.Http.Status
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.voabar.Utils
import uk.gov.hmrc.voabar.models.EbarsRequests._
import uk.gov.hmrc.voabar.models.LoginDetails

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext

@Singleton
class DefaultLegacyConnector @Inject()(val http: HttpClient,
                                val configuration: Configuration,
                                utils: Utils,
                                environment: Environment) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  private val legacyConnectorUrlPath = "microservice.services.legacy-ebars-client.baseUrl"
  private val baseUrl = runModeConfiguration.getString(legacyConnectorUrlPath)
    .getOrElse(throw new ConfigException.Missing(legacyConnectorUrlPath))
  private val autoBarsStubUrlPath = "microservice.services.autobars-stubs.baseUrl"
  private val autoBarsStubBaseUrl = runModeConfiguration.getString(autoBarsStubUrlPath)
    .getOrElse(throw new ConfigException.Missing(autoBarsStubUrlPath))

  def validate(loginDetails: LoginDetails)(implicit ec: ExecutionContext): Future[Try[Int]] = {
    implicit val authHc = utils.generateHeader(loginDetails)

    http.GET(s"${baseUrl}/ebars_dmz_pres_ApplicationWeb/Welcome.do").map { response =>
      response.status match {
        case(Status.OK) => Success(Status.OK)
        case _ => Failure(new RuntimeException("Login attempt fails with username = " + loginDetails.username + ", password = " + loginDetails.password))
      }
    } recover {
      case ex =>
        Logger.warn("Legacy validation fails with exception " + ex.getMessage)
        Failure(new RuntimeException("Legacy validation fails with exception " + ex.getMessage))
    }
  }

  private val X_EBARS_USERNAME = "X-ebars-username"
  private val X_EBARS_PASSWORD = "X-ebars-password"
  private val X_EBARS_ATTEMPT = "X-ebars-attempt"
  private val X_EBARS_UUID = "X-ebars-uuid"
  def sendBAReport(baReport: BAReportRequest)(implicit ec: ExecutionContext): Future[Try[Int]] = {
    implicit val authHc = utils.generateHeader(LoginDetails(baReport.username, baReport.password))
    http.POSTString(s"${autoBarsStubBaseUrl}/submit",
      baReport.propertyReport,
      Seq(
        X_EBARS_USERNAME -> baReport.username,
        X_EBARS_PASSWORD -> baReport.password,
        X_EBARS_ATTEMPT -> s"${baReport.attempt}",
        X_EBARS_UUID -> baReport.uuid)
    ).map{ response =>
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

@ImplementedBy(classOf[DefaultLegacyConnector])
trait LegacyConnector {
  def validate(loginDetails: LoginDetails)(implicit ec: ExecutionContext): Future[Try[Int]]
  def sendBAReport(baReport: BAReportRequest)(implicit ec: ExecutionContext): Future[Try[Int]]
}