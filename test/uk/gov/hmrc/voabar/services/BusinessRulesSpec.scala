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


import org.scalatest.WordSpec
import org.scalatest.Matchers._
import uk.gov.hmrc.voabar.models.BAPropertyReport

class BusinessRulesSpec extends WordSpec {

  val businessRules = new BusinessRules
  val reportBuilder = new MockBAReportBuilder

  "A BA property report" should {

    "A report with reason code CR03 (New) is valid if there is 0 existing and 1 proposed" in {
      val validReport = reportBuilder("CR03", 1000, 0, 1)
      val validReportErrors = businessRules.reasonForReportErrors(validReport)
      validReportErrors should have size 0
    }
  }

      "A report with reason code CR03 (New) is invalid if it contains 1 existing and 0 proposed" in {
        val invalidReport = reportBuilder("CR03", 1000, 1, 0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors should have size 2
      }

      "A report with reason code CR03 (New) is invalid if it contains 0 existing and 2 proposed" in {
        val invalidReport = reportBuilder("CR03", 1000, 0, 2)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors should have size 1
      }

      "A report with reason code CR03 (New) is invalid if it contains 0 entries" in {
        val invalidReport = reportBuilder("CR03", 1000, 0, 0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors should have size 1
      }

      "A report with reason code CR04 (Change to Domestic Use) is valid if it contains either 1 existing entry or" +
        "1 proposed entry" in {
        val oneExisting = reportBuilder("CR04", 1000, 1, 0)
        val oneProposed = reportBuilder("CR04", 1000, 0, 1)
        val oneExistingErrors = businessRules.reasonForReportErrors(oneExisting)
        val oneProposedErrors = businessRules.reasonForReportErrors(oneProposed)
        oneExistingErrors should have size 0
        oneProposedErrors should have size 0
      }

      "A report with reason code CR04 (Change to Domestic Use) is invalid if it contains 1 of each type of entry" in {
        val invalidReport = reportBuilder("CR04", 1000, 1, 1)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors should have size 1
      }

      "A report with reason code CR04 (Change to Domestic Use) is invalid if it contains 0 entries" in {
        val invalidReport = reportBuilder("CR04", 1000, 0, 0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors should have size 1
      }

      "A report with reason code CR05 (Reconstituted Property) is valid if it contains at least one of each " +
        "type of entry" in {
        val oneOfEach = reportBuilder("CR05", 1000, 1, 1)
        val validReport = reportBuilder("CR05", 1000, 3, 2)
        val oneOfEachErrors = businessRules.reasonForReportErrors(oneOfEach)
        val validReportErrors = businessRules.reasonForReportErrors(validReport)
        oneOfEachErrors should have size 0
        validReportErrors should have size 0
      }

      "A report with reason code CR05 (Reconstituted Property) is invalid if it contains 0 of either type of entry" in {
        val onlyOneExisiting = reportBuilder("CR05", 1000, 1, 0)
        val onlyOneProposed = reportBuilder("CR05", 1000, 0, 1)
        val onlyOneExisitingErrors = businessRules.reasonForReportErrors(onlyOneExisiting)
        val onlyOneProposedErrors = businessRules.reasonForReportErrors(onlyOneProposed)
        onlyOneExisitingErrors should have size 1
        onlyOneProposedErrors should have size 1
      }

      "A report with reason code CR08 (DO NOT USE) is invalid" in {
        val invalidReasonCode = reportBuilder("CR08", 1000, 1, 1)
        val invalidCodeErrors = businessRules.reasonForReportErrors(invalidReasonCode)
        invalidCodeErrors should have size 1
      }

      "A report with reason code CR11 (Boundary Change - DO NOT USE) is invalid" in {
        val invalidReasonCode = reportBuilder("CR11", 1000, 1, 1)
        val invalidCodeErrors = businessRules.reasonForReportErrors(invalidReasonCode)
        invalidCodeErrors should have size 1
      }

      "A report with reason code CR12 (Major Address Change) is valid if it contains 1 existing and 1 proposed" in {
        val validReport = reportBuilder("CR12",1000,1,1)
        val validReportErrors = businessRules.reasonForReportErrors(validReport)
        validReportErrors should have size 0
      }

      "A report with reason code CR12 (Major Address Change) is invalid if it does not contain 1 of each" in {
        val invalidReport = reportBuilder("CR12",1000,0,0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors should have size 2
      }

      "A report with reason code CR13 (Boundary Change add - DO NOT USE) is invalid" in {
        val invalidReasonCode = reportBuilder("CR13", 1000, 1, 1)
        val invalidCodeErrors = businessRules.reasonForReportErrors(invalidReasonCode)
        invalidCodeErrors should have size 1
      }

      "A report with any other reason code is valid if it contains 0 proposed and 1 existing entry" in {
        val validReport = reportBuilder("CR99",1000,1,0)
        val validReportErrors = businessRules.reasonForReportErrors(validReport)
        validReportErrors should have size 0
      }

      "A report with any other reason code is invalid if it does not contain 0 proposed and 1 existing" in {
        val oneOfEach = reportBuilder("CR99",1000,1,1)
        val oneProposed = reportBuilder("CR99",1000,0,1)
        val oneOfEachErrors = businessRules.reasonForReportErrors(oneOfEach)
        val oneProposedErrors = businessRules.reasonForReportErrors(oneProposed)
        oneOfEachErrors should have size 1
        oneProposedErrors should have size 2
      }

      "A BA report without a TypeOfTax node" should {
        "throw a runtime exception when an attempt is made to access the first child of a TypeOfTax node" in {
          val xmlNode = BAPropertyReport(<xml></xml>)
          an [RuntimeException] should be thrownBy businessRules.reasonForReportErrors(xmlNode)
        }
      }

      "A BA report with a TypeOfTax node" should {
       "throw a runtime exception when the first child of the node is not cTaxReasonForReport" in {
         val xmlNode = BAPropertyReport(<xml><TypeOfTax><NDR></NDR></TypeOfTax></xml>)
        an [RuntimeException] should be thrownBy businessRules.reasonForReportErrors(xmlNode)
       }
      }






}
