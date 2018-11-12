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

import com.google.inject.name.Named
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.typesafe.config.ConfigException
import play.api.Configuration
import play.api.Mode.Mode
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.voabar.Utils
import uk.gov.hmrc.voabar.models.LoginDetails

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultEmailConnector @Inject() (
                                 @Named("Mode") override val mode : Mode,
                                 override val runModeConfiguration: Configuration,
                                 val http: WSHttp,
                                 utils: Utils
                               ) (implicit messages: Messages, ec: ExecutionContext) extends ServicesConfig with EmailConnector {
  lazy val emailUrl = baseUrl("email")
  lazy val needsToSendEmail = runModeConfiguration.getBoolean("needToSendEmail").getOrElse(false)
  lazy val email = runModeConfiguration.getString("email").getOrElse(throw new ConfigException.Missing("email"))
  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  def sendEmail(
                 baRefNumber: String,
                 username: String,
                 password: String,
                 fileName: String,
                 dateSubmitted: String,
                 errorList: String): Future[Unit] = {
    implicit val authHc = utils.generateHeader(LoginDetails(username, password))
    if (needsToSendEmail) {
      val json = Json.obj(
        "to" -> JsArray(Seq(JsString(email))),
        "templateId" -> JsString("bars_alert"),
        "parameters" -> JsObject(Seq(
          "baRefNumber" -> JsString(s"""${Messages("BA ")}: $baRefNumber"""),
          "fileName" -> JsString(s"""${Messages("File name ")}: $fileName"""),
          "dateSubmitted" -> JsString(s"""${Messages("Date Submitted ")}: $dateSubmitted"""),
          "errorList" -> JsString(s"""${Messages("Errors ")} $errorList""")
        )),
        "force" -> JsBoolean(false)
      )

      http.POST[JsValue, Unit](s"$emailUrl/send-templated-email/", json)
    }
    else {
      Future.successful(Unit)
    }
  }
}

@ImplementedBy(classOf[DefaultEmailConnector])
trait EmailConnector {
  def sendEmail(
                 baRefNumber: String,
                 username: String,
                 password: String,
                 fileName: String,
                 dateSubmitted: String,
                 errorList: String): Future[Unit]
}
