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

package uk.gov.hmrc.voabar.services

import org.apache.commons.io.IOUtils
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec

import scala.xml._

class MockBAReportBuilderSpec extends PlaySpec{

  val reportBuilder = new MockBAReportBuilder

  "A mock BA property report" must {

    "contain the reason for report code specified" in {
      val reasonCode: String = (reportBuilder("CR03", 1000, 1, 0).node \\ "ReasonForReportCode").text
      reasonCode mustBe "CR03"
    }

    "contain the corresponding reason for report description for a given reason code" in {
      val reasonDescription: String = (reportBuilder("CR05", 1000, 1, 1).node \\ "ReasonForReportDescription").text
      reasonDescription mustBe "Reconstituted Property"
    }

    "contain the BA code specified" in {
      val baCode: String = (reportBuilder("CR03", 1000, 1, 0).node \\ "BAidentityNumber").text
      baCode mustBe "1000"
    }

    "contain the number of existing entries and proposed entries specified" in {
      val baPropertyReport: NodeSeq = reportBuilder("CR03", 1000, 3, 0).node
      val existingEntries = baPropertyReport \\ "ExistingEntries"
      val proposedEntries = baPropertyReport \\ "ProposedEntries"
      existingEntries.size mustBe 3
      proposedEntries.size mustBe 0
    }

    "a valid batch report may be made invalid" should {

      val batchSubmission = XML.loadString(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))

      "may be modified by replacing an existing element label with a new label" in {
        val result = reportBuilder.invalidateBatch(batchSubmission, "BAreportHeader", "invalidHeader")
        (result \\ "BAreportHeader").size mustBe 0
        (result \\ "invalidHeader").size mustBe 1
      }

      "may be modified by replacing some existing data with some new data" in {
        val result = reportBuilder.invalidateBatch(batchSubmission, "Some Valid Council", "Some New Council")
        (result \\ "BillingAuthority").text mustBe "Some New Council"
      }
    }
  }
}
