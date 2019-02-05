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

package uk.gov.hmrc.voabar.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.voabar.services.ReportUploadService

import scala.concurrent.ExecutionContext

@Singleton
class UploadController @Inject()(reportUploadService: ReportUploadService)
                                (implicit ec: ExecutionContext) extends BaseController {

  def upload(): Action[AnyContent] = Action(parse.anyContent(Option(1024L * 1024L * 20L))) { implicit request =>
    val headers = request.headers

    val response = for {
      contentType <- headers.get("Content-Type").toRight(UnsupportedMediaType).right
      _ <- checkContentType(contentType).right
      baCode <- headers.get("BA-Code").toRight(Unauthorized("BA-Code missing")).right
      password <- headers.get("password").toRight(Unauthorized("password missing")).right
      reference <- request.getQueryString("reference").toRight(BadRequest("missing reference")).right
      xml <- request.body.asText.toRight(BadRequest("missing xml playload")).right
    } yield {
      reportUploadService.upload(baCode, password, xml, reference)
      Ok("")
    }

    response.fold(x => x, x => x)

  }

  private def checkContentType(contentType: String): Either[Result, Boolean] = {
    if("text/plain".equals(contentType)) {
      Right(true)
    }else {
      Left(UnsupportedMediaType("only text/plain is supported"))
    }
  }

}
