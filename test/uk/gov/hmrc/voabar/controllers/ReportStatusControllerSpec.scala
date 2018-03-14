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

class ReportStatusControllerSpec  extends PlaySpec with MockitoSugar {
  "ReportStatusController" must {
    "Given some Json representing a a business authority asking for the current report status" in {
      val baCode = "ba1221"
      val submissionId = "1234-XX"
      val errors = Seq(Error("BAD-CHAR", Seq("ba1221")))

      val rs0 = ReportStatus(baCode, submissionId, "SUBMITTED")
      val rs1 = ReportStatus(baCode, submissionId, "INVALIDATED", errors)

      val goodJson = """{"bacode": "ba1221"}"""

      val controller = new ReportStatusController()
      val result = await(controller.generateReportStatuses(Json.parse(goodJson)))

    }
  }
}
