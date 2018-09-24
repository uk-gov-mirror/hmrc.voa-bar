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
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import uk.gov.hmrc.voabar.models.Error
import uk.gov.hmrc.voabar.util.ErrorCode

import scala.xml.XML

class BusinessRulesSpec extends PlaySpec {

  val reportBuilder = new MockBAReportBuilder
  val REPORTNUMBER = "118294"
  val batchWith1Report = IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))
  val batchWith4Reports = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))
  val businessRules = new BusinessRules()

  "A BA property report" must {

    "A report with reason code CR03 (New) is valid if there is 0 existing and 1 proposed" in {
      val validReport = reportBuilder("CR03", 1000, 0, 1)
      val validReportErrors = businessRules.reasonForReportErrors(validReport)
      validReportErrors.size mustBe 0
    }
  }

      "A report with reason code CR03 (New) is invalid if it contains 1 existing and 0 proposed" in {
        val invalidReport = reportBuilder("CR03", 1000, 1, 0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors mustBe Seq[Error](Error(ErrorCode.ONE_PROPOSED,Seq(REPORTNUMBER, "CR03")),
                                              Error(ErrorCode.NONE_EXISTING, Seq(REPORTNUMBER, "CR03")))
      }

      "A report with reason code CR03 (New) is invalid if it contains 0 existing and 2 proposed" in {
        val invalidReport = reportBuilder("CR03", 1000, 0, 2)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors mustBe Seq[Error](Error(ErrorCode.ONE_PROPOSED, Seq(REPORTNUMBER, "CR03")))
      }

      "A report with reason code CR03 (New) is invalid if it contains 0 entries" in {
        val invalidReport = reportBuilder("CR03", 1000, 0, 0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors mustBe Seq[Error](Error(ErrorCode.ONE_PROPOSED, Seq(REPORTNUMBER, "CR03")))
      }

      "A report with reason code CR04 (Change to Domestic Use) is valid if it contains either 1 existing entry or" +
        " 1 proposed entry" in {
        val oneExisting = reportBuilder("CR04", 1000, 1, 0)
        val oneProposed = reportBuilder("CR04", 1000, 0, 1)
        val oneExistingErrors = businessRules.reasonForReportErrors(oneExisting)
        val oneProposedErrors = businessRules.reasonForReportErrors(oneProposed)
        oneExistingErrors.size mustBe 0
        oneProposedErrors.size mustBe 0
      }

      "A report with reason code CR04 (Change to Domestic Use) is invalid if it contains 1 of each type of entry" in {
        val invalidReport = reportBuilder("CR04", 1000, 1, 1)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors mustBe Seq[Error](Error(ErrorCode.EITHER_ONE_EXISTING_OR_ONE_PROPOSED,
          Seq(REPORTNUMBER, "CR04")))
      }

      "A report with reason code CR04 (Change to Domestic Use) is invalid if it contains 0 entries" in {
        val invalidReport = reportBuilder("CR04", 1000, 0, 0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors mustBe Seq[Error](Error(ErrorCode.EITHER_ONE_EXISTING_OR_ONE_PROPOSED,
          Seq(REPORTNUMBER, "CR04")))
      }

      "A report with reason code CR05 (Reconstituted Property) is valid if it contains at least one of each " +
        "type of entry" in {
        val oneOfEach = reportBuilder("CR05", 1000, 1, 1)
        val validReport = reportBuilder("CR05", 1000, 3, 2)
        val oneOfEachErrors = businessRules.reasonForReportErrors(oneOfEach)
        val validReportErrors = businessRules.reasonForReportErrors(validReport)
        oneOfEachErrors.size mustBe 0
        validReportErrors.size mustBe 0
      }

      "A report with reason code CR05 (Reconstituted Property) is invalid if it contains 0 of either type of entry" in {
        val onlyOneExisiting = reportBuilder("CR05", 1000, 1, 0)
        val onlyOneProposed = reportBuilder("CR05", 1000, 0, 1)
        val onlyOneExisitingErrors = businessRules.reasonForReportErrors(onlyOneExisiting)
        val onlyOneProposedErrors = businessRules.reasonForReportErrors(onlyOneProposed)
        onlyOneExisitingErrors mustBe Seq(Error(ErrorCode.ATLEAST_ONE_PROPOSED, Seq(REPORTNUMBER, "CR05")))
        onlyOneProposedErrors mustBe Seq(Error(ErrorCode.ATLEAST_ONE_EXISTING, Seq(REPORTNUMBER, "CR05")))
      }

      "A report with reason code CR08 (DO NOT USE) is invalid" in {
        val invalidReasonCode = reportBuilder("CR08", 1000, 1, 1)
        val invalidCodeErrors = businessRules.reasonForReportErrors(invalidReasonCode)
        invalidCodeErrors mustBe Seq[Error](Error(ErrorCode.NOT_IN_USE, Seq(REPORTNUMBER, "CR08")))
      }

      "A report with reason code CR11 (Boundary Change - DO NOT USE) is invalid" in {
        val invalidReasonCode = reportBuilder("CR11", 1000, 1, 1)
        val invalidCodeErrors = businessRules.reasonForReportErrors(invalidReasonCode)
        invalidCodeErrors mustBe Seq[Error](Error(ErrorCode.NOT_IN_USE, Seq(REPORTNUMBER, "CR11")))

      }

      "A report with reason code CR12 (Major Address Change) is valid if it contains 1 existing and 1 proposed" in {
        val validReport = reportBuilder("CR12",1000,1,1)
        val validReportErrors = businessRules.reasonForReportErrors(validReport)
        validReportErrors.size mustBe 0
      }

      "A report with reason code CR12 (Major Address Change) is invalid if it does not contain 1 of each entry type" in {
        val invalidReport = reportBuilder("CR12",1000,0,0)
        val invalidReportErrors = businessRules.reasonForReportErrors(invalidReport)
        invalidReportErrors mustBe Seq[Error](Error(ErrorCode.ONE_PROPOSED,Seq(REPORTNUMBER, "CR12")),
          Error(ErrorCode.ONE_EXISTING, Seq(REPORTNUMBER, "CR12")))

      }

      "A report with reason code CR13 (Boundary Change add - DO NOT USE) is invalid" in {
        val invalidReasonCode = reportBuilder("CR13", 1000, 1, 1)
        val invalidCodeErrors = businessRules.reasonForReportErrors(invalidReasonCode)
        invalidCodeErrors mustBe Seq[Error](Error(ErrorCode.NOT_IN_USE, Seq(REPORTNUMBER, "CR13")))
      }

      "A report with any other reason code is valid if it contains 0 proposed and 1 existing entry" in {
        val validReport = reportBuilder("CR99",1000,1,0)
        val validReportErrors = businessRules.reasonForReportErrors(validReport)
        validReportErrors.size mustBe 0
      }

      "A report with any other reason code is invalid if it does not contain 0 proposed and 1 existing" in {
        val oneOfEach = reportBuilder("CR99",1000,1,1)
        val oneProposed = reportBuilder("CR99",1000,0,1)
        val oneOfEachErrors = businessRules.reasonForReportErrors(oneOfEach)
        val oneProposedErrors = businessRules.reasonForReportErrors(oneProposed)
        oneOfEachErrors mustBe Seq[Error](Error(ErrorCode.NONE_PROPOSED, Seq(REPORTNUMBER, "CR99")))
        oneProposedErrors mustBe Seq[Error](Error(ErrorCode.NONE_PROPOSED, Seq(REPORTNUMBER, "CR99")),
          Error(ErrorCode.ONE_EXISTING, Seq(REPORTNUMBER, "CR99")))
      }

      "A BA report without a TypeOfTax node" must {

        "return an error" in {
          val xmlNode = <xml></xml>
        businessRules.reasonForReportErrors(xmlNode) mustBe Error(ErrorCode.UNKNOWN_TYPE_OF_TAX, Seq()) :: List()
        }
      }

      "A BA report with a TypeOfTax node" must {

       "throw a runtime exception when the first child of the node is not cTaxReasonForReport" in {
         val xmlNode = <xml><TypeOfTax><NDR></NDR></TypeOfTax></xml>
         businessRules.reasonForReportErrors(xmlNode) mustBe Error(ErrorCode.UNSUPPORTED_TAX_TYPE, Seq("NDR")) :: List()
       }
      }

  "The BA identity code validator" must {
    "return a empty list (no errors) when the BA code in the request header matches that in the report" in {
      val validBatch = XML.loadString(batchWith1Report)
      implicit val request = FakeRequest("GET","").withHeaders("BA-Code" -> "9999")
      businessRules.baIdentityCodeErrors(validBatch).isEmpty mustBe true
    }

    "return a list of 1 error when the BA code in the request header does not match that in the report" in {
      val validBatch = XML.loadString(batchWith1Report)
      implicit val request = FakeRequest("GET","").withHeaders("BA-Code" -> "0000")
      businessRules.baIdentityCodeErrors(validBatch.head) mustBe List(
        Error(ErrorCode.BA_CODE_MATCH, Seq()))
    }

    "return a list of 1 error when the BA code in the report is empty" in {
      val validBatch = XML.loadString(batchWith1Report)
      implicit val request = FakeRequest("GET","").withHeaders("BA-Code" -> "9999")
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head,Map("BillingAuthorityIdentityCode" -> ""))
      businessRules.baIdentityCodeErrors(invalidBatch.head) mustBe List(
        Error(ErrorCode.BA_CODE_REPORT, Seq()))
    }

    "return a list of 1 error when the BA code in the request header is empty" in {
      implicit val request = FakeRequest("GET","")
      val validBatch = XML.loadString(batchWith1Report)
      businessRules.baIdentityCodeErrors(validBatch.head) mustBe List(
        Error(ErrorCode.BA_CODE_REQHDR, Seq()))
    }

    "return a list of 2 errors when the BA code is missing from the request header and the report" in {
      val validBatch = XML.loadString(batchWith1Report)
      implicit val request = FakeRequest("GET","")
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head,Map("BillingAuthorityIdentityCode" -> ""))
      businessRules.baIdentityCodeErrors(invalidBatch.head) mustBe List(
        Error(ErrorCode.BA_CODE_REPORT, Seq()),
        Error(ErrorCode.BA_CODE_REQHDR, Seq())
      )
    }
  }
}
