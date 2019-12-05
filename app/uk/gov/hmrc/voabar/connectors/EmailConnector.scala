/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Singleton}
import com.typesafe.config.ConfigException
import javax.inject.Inject
import models.Purpose.Purpose
import play.api.{Configuration, Environment}
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.voabar.Utils
import uk.gov.hmrc.voabar.models.LoginDetails

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultEmailConnector @Inject() (val http: HttpClient,
                                       val configuration: Configuration,
                                       utils: Utils,
                                       environment: Environment)(implicit ec: ExecutionContext)
  extends EmailConnector {
  val emailConfig = configuration.getConfig("microservice.services.email")
    .getOrElse(throw new ConfigException.Missing("microservice.services.email"))
  val emailUrl = s"${emailConfig.getString("protocol").getOrElse("http")}://${emailConfig.getString("host").get}:${emailConfig.getString("port").get}"
  val needsToSendEmail = configuration.getBoolean("needToSendEmail").getOrElse(false)
  val email = configuration.getString("email")
    .getOrElse(if (needsToSendEmail) throw new ConfigException.Missing("email") else "")
  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  def sendEmail(
                 baRefNumber: String,
                 purpose: Purpose,
                 transactionId: String,
                 username: String,
                 password: String,
                 fileName: String,
                 dateSubmitted: String,
                 errorList: String): Future[Unit] = {
    implicit val authHc = utils.generateHeader(LoginDetails(username, password))
    if (needsToSendEmail) {
      val json = Json.obj(
        "to" -> JsArray(Seq(JsString(email))),
        "templateId" -> JsString("bars_alert_transaction"),
        "parameters" -> JsObject(Seq(
          "baRefNumber" -> JsString(s"""BA : $baRefNumber Type: $purpose"""),
          "transactionId" -> JsString(s"""Transaction id : $transactionId"""),
          "fileName" -> JsString(s"""File name : $fileName"""),
          "dateSubmitted" -> JsString(s"""Date Submitted : $dateSubmitted"""),
          "errorList" -> JsString(s"""Errors $errorList""")
        )),
        "force" -> JsBoolean(false)
      )

      http.POST[JsValue, Unit](s"$emailUrl/hmrc/email", json)
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
                 purpose: Purpose,
                 transactionId: String,
                 username: String,
                 password: String,
                 fileName: String,
                 dateSubmitted: String,
                 errorList: String): Future[Unit]
}
