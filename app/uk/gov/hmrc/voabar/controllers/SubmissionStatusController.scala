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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.{Action, Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.voabar.models.ReportStatus
import uk.gov.hmrc.voabar.models.ReportStatus._
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionStatusController @Inject() (
                                           submissionStatusRepository: SubmissionStatusRepository
                                           )(implicit ec: ExecutionContext) extends BaseController {
  def getByUser() = Action.async(parse.empty) { implicit request =>
    (for {
      userId <- EitherT.fromOption[Future](request.headers.get("BA-Code"), Unauthorized("BA-Code missing"))
      reportStatuses <- EitherT(submissionStatusRepository.getByUser(userId))
    } yield (Ok(reportStatuses)))
        .valueOr(_ => InternalServerError)
  }

  private def parseReportStatus(request: Request[JsValue]): Either[Result, ReportStatus] = {
    request.body.validate[ReportStatus] match {
      case result: JsSuccess[ReportStatus] => Right(result.get)
      case _ => Left(BadRequest)
    }
  }

  def save(upsert: Boolean = false) = Action.async(parse.tolerantJson) { request =>
    (for {
      reportStatus <- EitherT.fromEither[Future](parseReportStatus(request))
      _ <- EitherT(submissionStatusRepository.saveOrUpdate(reportStatus, upsert))
    } yield NoContent)
      .valueOr(_ => InternalServerError)
  }

  def saveUserInfo() = Action.async(parse.tolerantJson) { request =>
    (for {
      reportStatus <- EitherT.fromEither[Future](parseReportStatus(request))
      _ <- EitherT(submissionStatusRepository.saveOrUpdate(reportStatus.userId.get, reportStatus._id, true))
    } yield NoContent)
      .valueOr(_ => InternalServerError)
  }
}
