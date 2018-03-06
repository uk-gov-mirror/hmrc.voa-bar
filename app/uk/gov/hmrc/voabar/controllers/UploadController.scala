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

import scala.collection.immutable
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class UploadController @Inject()() extends BaseController {

  def checkXml(node:NodeSeq): Future[Int] = {
    Thread.sleep(10)
    Future.successful(0)
  }

  def upload(): Action[AnyContent] = Action { implicit request =>

    //    request.headers.get("Content-Type") match {
    //      case Some(content) if content == "application/xml" =>
    //        request.body.asXml match {
    //          case Some(xml) =>
    //            request.headers.get("BA-Code") match {
    //              case Some(baCode) =>
    //                val id = generateSubmissionID(baCode)
    //                checkXml (xml)
    //                Future.successful (Ok(id))
    //              case _ => Future.successful(BadRequest)
    //            }
    //          case None => Future.successful(Unauthorized)
    //        }
    //      case _ => Future.successful(UnsupportedMediaType)
    //    }
    //  }

    val headers = request.headers
    headers.get("Content-Type") match {
      case Some(content) if content == "application/xml" =>
        headers.get("BA-Code") match {
          case Some(baCode) => request.body.asXml match {
            case Some(xml) =>
              val id = generateSubmissionID(baCode)
              checkXml(xml)
              Ok(id)
            case None => BadRequest
          }
          case None => Unauthorized
        }
      case Some(_) => UnsupportedMediaType
      case None => BadRequest
    }
  }

  def generateSubmissionID(baCode: String): String = {
    val chars = 'A' to 'Z'
    def ran = scala.util.Random.nextInt(chars.size)
    s"$baCode-${System.currentTimeMillis()}-${chars(ran)}${chars(ran)}"
  }

}
