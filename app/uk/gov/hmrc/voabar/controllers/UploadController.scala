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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class UploadController @Inject()() extends BaseController {

  def checkXml(node:NodeSeq): Future[Int] = {
    Thread.sleep(10)
    Future.successful(0)
  }

  def upload(): Action[AnyContent] = Action.async { implicit request =>

    request.headers.get("Content-Type") match {
      case Some(content) if content == "application/xml" =>
        request.body.asXml match {
          case Some(xml) =>
            request.headers.get("BA-Code") match {
              case Some(baCode) =>
                val id = generateSubmissionID(baCode)
                checkXml (xml)
                Future.successful (Ok(id))
              case _ => Future.successful(BadRequest)
            }
          case None => Future.successful(BadRequest)
        }
      case _ => Future.successful(UnsupportedMediaType)
    }
  }

  def generateSubmissionID(baCode: String): String = {
    s"$baCode-${System.currentTimeMillis()}-${scala.util.Random.alphanumeric.take(2).mkString("")}"
  }

}
