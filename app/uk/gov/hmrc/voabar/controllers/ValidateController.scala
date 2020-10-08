/*
 * Copyright 2020 HM Revenue & Customs
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

import org.apache.commons.io.IOUtils


import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.voabar.services.{SubmissionProcessingService, ValidationService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateController @Inject() (validationService: ValidationService,
                                    controllerComponents: ControllerComponents,
                                    submissionProcessingService: SubmissionProcessingService
                                   )(implicit ec: ExecutionContext)
  extends BackendController(controllerComponents) {

  val logger = Logger("v2-validation")

  def validate(baLogin: String) = Action.async(parse.temporaryFile) { implicit request =>

    val headerCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    val requestId = headerCarrier.requestId.map(_.value).getOrElse("None")

    val v1ProcessingStatus = request.headers.get("X-autobars-processing-status").getOrElse("None")

    val url = request.body.path.toUri.toURL.toString //Safe conversion

    Future {
      validationService.validate(url, baLogin) match {
        case Left(errors) => {
          logger.info(s"Validation failed, baLogin: ${baLogin}, requestId: ${requestId}, v1-processing-status: ${v1ProcessingStatus} errors: ${errors}")
        }
        case Right((document, node)) => {
          logger.info(s"Validation successful, baLogin: ${baLogin}, v1-processing-status: ${v1ProcessingStatus}, requestId: ${requestId}")
        }
      }

      val xmlAsString = IOUtils.toString(request.body.path.toUri, "UTF-8") //TODO - We need to change how we load xml.
                                                                                     //TODO   It should be XML parser to decide which encoding he want.
                                                                                     //TODO - Maybe some fuzzy encoding detection.

      submissionProcessingService.processAsV1(xmlAsString, baLogin, requestId)

      Ok("")
    }

  }

}
