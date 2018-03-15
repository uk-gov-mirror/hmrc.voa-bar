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

import org.mockito.Matchers._
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.PlaySpec
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import play.api.libs.json.Json
import uk.gov.hmrc.voabar.models.{Error, LoginDetails, ReportStatus}
import play.api.test.Helpers.{status, _}
import play.api.test._
import uk.gov.hmrc.voabar.services.ReportStatusHistoryService

import scala.concurrent.Future

class ReportStatusControllerSpec  extends PlaySpec with MockitoSugar {
  val baCode = "ba1221"
  val submissionId = "1234-XX"
  val errors = Seq(Error("BAD-CHAR", Seq("ba1221")))
  val rs = ReportStatus(baCode, submissionId, "INVALIDATED", errors)
  val fakeMap = Map(submissionId -> List(rs))
  val fakeMapAsJson = Json.toJson(fakeMap).toString()

  val fakeRequestWithHeader = FakeRequest("GET", "").withHeaders("Content-Type" -> "application/json", "BA-Code" -> baCode)
  val fakeRequestWithoutHeader = FakeRequest("GET", "").withHeaders("Content-Type" -> "application/json")

  object fakeHistoryService extends ReportStatusHistoryService {
    def reportSubmitted(baCode: String, submissionId: String): Future[Boolean] = ???
    def reportCheckedWithNoErrorsFound(baCode: String, submissionId: String): Future[Boolean] = ???
    def reportCheckedWithErrorsFound(baCode: String, submissionId: String, errors: Seq[Error]): Future[Boolean] = ???
    def reportForwarded(baCode: String, submissionId: String): Future[Boolean] = ???
    def findReportsBySubmission(submissionId: String): Future[Option[List[ReportStatus]]] = ???
    def findReportsByBaCode(code: String): Future[Option[Map[String, List[ReportStatus]]]] = Future.successful(Some(fakeMap))
  }

  object fakeHistoryServiceMongoFails extends ReportStatusHistoryService {
    def reportSubmitted(baCode: String, submissionId: String): Future[Boolean] = ???
    def reportCheckedWithNoErrorsFound(baCode: String, submissionId: String): Future[Boolean] = ???
    def reportCheckedWithErrorsFound(baCode: String, submissionId: String, errors: Seq[Error]): Future[Boolean] = ???
    def reportForwarded(baCode: String, submissionId: String): Future[Boolean] = ???
    def findReportsBySubmission(submissionId: String): Future[Option[List[ReportStatus]]] = ???
    def findReportsByBaCode(code: String): Future[Option[Map[String, List[ReportStatus]]]] = Future.successful(None)
  }

  "ReportStatusController" must {
    "Given a business authority, the generateReportStatuses will generate the Json needed for the current report status" in {
      val controller = new ReportStatusController(fakeHistoryService)
      val result = await(controller.generateReportStatuses(submissionId))
      result match {
        case Some(json) => json.toString() mustBe fakeMapAsJson
        case None => fail
      }
    }

    "Given a BA in the header, the onPageLoad method returns a status of 200" in {
      val controller = new ReportStatusController(fakeHistoryService)
      val result = controller.onPageLoad()(fakeRequestWithHeader)
      status(result) mustBe 200
    }

    "Given a BA in the header, the onPageLoad method returns a json body with the report statuses in it" in {
      val controller = new ReportStatusController(fakeHistoryService)
      val result = controller.onPageLoad()(fakeRequestWithHeader)
      contentAsJson(result).toString() mustBe fakeMapAsJson
    }

    "return 400 (badrequest) when given no BA in the header" in {
      val controller = new ReportStatusController(fakeHistoryService)
      val result = controller.onPageLoad()(fakeRequestWithoutHeader)
      status(result) mustBe 400
    }


    "return 400 (badrequest) when the backend service call fails" in {
      val controller = new ReportStatusController(fakeHistoryServiceMongoFails)
      val result = controller.onPageLoad()(fakeRequestWithHeader)
      status(result) mustBe 400
    }
  }
}
