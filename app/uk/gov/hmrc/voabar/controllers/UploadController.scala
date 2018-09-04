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
import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.EbarsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.services.{ReportStatusHistoryService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadController @Inject()(
                                  historyService: ReportStatusHistoryService,
                                  legacyConnector: LegacyConnector,
                                  ebarsValidator: EbarsValidator
                                )
                                (implicit ec: ExecutionContext) extends BaseController {

  def checkXml(node: String, baCode: String, password: String, submissionId: String): Future[Unit] = {
    for {
      _ <- historyService.reportCheckedWithNoErrorsFound(baCode, submissionId)
      _ <- historyService.reportForwarded(baCode, submissionId)
    } yield ()
  }

  def upload(): Action[AnyContent] = Action.async(parse.anyContent) { implicit request =>
    val headers = request.headers
    headers.get("Content-Type") match {
      case Some(content) if content == "text/plain" =>
        headers.get("BA-Code") match {
          case Some(baCode) =>
            headers.get("password") match {
              case Some(pass) => {
                process(request, baCode, pass)
              }
              case None => Future.successful(Unauthorized)
            }
          case None => Future.successful(Unauthorized)
        }
      case Some(_) => Future.successful(UnsupportedMediaType)
      case None => Future.successful(BadRequest)
    }
  }

  private def process(request: Request[AnyContent], baCode: String, pass: String)(implicit hc: HeaderCarrier): Future[Result] = {
    request.body.asText match {
      case Some(xml) => {
        val id = generateSubmissionID(baCode)
          .replace("\\n", "")
        for {
          _ <- historyService.reportSubmitted(baCode, id)
          _ <- checkXml(xml, baCode, pass, id)
          result <-
            legacyConnector.sendBAReport(BAReportRequest(id, ebarsValidator.toJson(ebarsValidator.fromXml(xml)), baCode, pass))
              .map(_ => Ok(id))
              .recover {
                case ex: Throwable => {
                  Logger.warn(s"Error while processing xml: \n$xml", ex)
                  InternalServerError
                }
              }
        } yield result
      }
      case _ => Future.successful(BadRequest)
    }
  }

  private def generateSubmissionID(baCode: String): String = {
    val chars = 'A' to 'Z'

    def ran = scala.util.Random.nextInt(chars.size)

    s"$baCode-${
      System.currentTimeMillis()
    }-${
      chars(ran)
    }${
      chars(ran)
    }"
  }

}
