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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.BAReport
import uk.gov.hmrc.voabar.services.ReportStatusHistoryService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadController @Inject()(
                                  historyService: ReportStatusHistoryService,
                                  legacyConnector: LegacyConnector
                                )
                                (implicit ec: ExecutionContext) extends BaseController {

  def checkXml(node: String, baCode: String, password: String, submissionId: String): Future[Unit] = {
    Thread.sleep(10)
    historyService.reportCheckedWithNoErrorsFound(baCode, submissionId)
    historyService.reportForwarded(baCode, submissionId)
    Future.successful(())
  }

  def upload(): Action[AnyContent] = Action.async(parse.text) { implicit request =>
    val headers = request.headers
    headers.get("Content-Type") match {
      case Some(content) if content == "text/plain" =>
        headers.get("BA-Code") match {
          case Some(baCode) =>
            headers.get("password") match {
              case Some(pass) => {
                val xml = request.body
                val id = generateSubmissionID(baCode)
                for {
                  _ <- historyService.reportSubmitted(baCode, id)
                  _ <- checkXml(xml, baCode, pass, id)
                } yield (
                legacyConnector.sendBAReport(BAReport(id, xml, baCode, pass, 1))
                  .map(_ => Ok(id))
                  .recover {
                    case ex: Throwable => InternalServerError()
                  })
              }
              case None => Future(Unauthorized)
            }
          case None => Future(Unauthorized)
        }
      case Some(_) => Future(UnsupportedMediaType)
      case None => Future(BadRequest)
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
