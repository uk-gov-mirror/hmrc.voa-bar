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
import play.api.Logger
import play.api.mvc.Action
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.voabar.services.ValidationService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateController @Inject() (validationService: ValidationService)(implicit ec: ExecutionContext) extends BaseController {

  val logger = Logger("v2-validation")

  def validate(baLogin: String) = Action.async(parse.temporaryFile) { implicit request =>

    val headerCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    val requestId = headerCarrier.requestId.map(_.value).getOrElse("None")

    val v1ProcessingStatus = request.headers.get("X-autobars-processing-status").getOrElse("None")

    val url = request.body.file.toURI.toURL.toString //Safe conversion

    Future {
      validationService.validate(url, baLogin) match {
        case Left(errors) => {
          logger.info(s"Validation failed, baLogin: ${baLogin}, requestId: ${requestId}, v1-processing-status: ${v1ProcessingStatus} errors: ${errors}")
        }
        case Right((document, node)) => {
          logger.info(s"Validation successful, baLogin: ${baLogin}, v1-processing-status: ${v1ProcessingStatus}, requestId: ${requestId}")
        }
      }
      Ok("")
    }

  }

}
