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

import play.api.libs.json._
import play.api.Logger
import play.api.mvc.{Action, AnyContent, Result}
import play.api.mvc.Results._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.voabar.models.ReportStatus
import uk.gov.hmrc.voabar.services.ReportStatusHistoryService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ReportStatusController @Inject()(reportStatusHistory: ReportStatusHistoryService) extends BaseController {
  def generateReportStatuses(baCode: String): Future[Option[JsValue]] = {
    reportStatusHistory.findReportsByBaCode(baCode) map {
      case Some(sm) => Some(Json.toJson(sm))
      case None => None
    }
  }

  def onPageLoad(baCode: String): Action[AnyContent] = Action.async {implicit request =>
    generateReportStatuses(baCode) map {
        case Some(json) => Ok(json)
        case None =>
          Logger.warn(s"Request for status reports from front end received while mongo unavailable")
          BadRequest("No BA Code found in header")
      }
  }
}
