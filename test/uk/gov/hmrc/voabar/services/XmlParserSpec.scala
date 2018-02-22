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
import uk.gov.hmrc.voabar.models.{BAPropertyReport, BatchHeader, BatchSubmission, BatchTrailer}
import org.scalatest.Matchers._
import uk.gov.hmrc.voabar.models

import scala.xml.{Elem, Node, NodeSeq}

class XmlParserSpec extends WordSpec {

  val xmlParser = new XmlParser()

  val batchSubmission:BatchSubmission = xmlParser.parseXml(IOUtils.toString(getClass.getResource("/xml/CTValid1.xml")))

  "Given a valid BAReports xml file, XmlParser" should {
    "return a BatchSubmission object" in {
      batchSubmission shouldBe a [BatchSubmission]
    }
  }

  "A BatchSubmission object" should {

    "contain a BatchHeader" in {
      batchSubmission.batchHeader shouldBe a [BatchHeader]
    }

    "contain a list of BAPropertyReports of type BAPropertyReport" in {
      batchSubmission.baPropertyReports shouldBe a [List[BAPropertyReport]]
    }

    "contain a BatchTrailer" in {
      batchSubmission.batchTrailer shouldBe a [BatchTrailer]
    }
  }

  "A BatchHeader" should {

    val batchHeader = batchSubmission.batchHeader

    "contain a BillingAuthority element value" in {
      (batchHeader.node \ "BillingAuthority").text shouldBe "Valid Council"
    }

    "contain a BillingAuthorityIdentityCode element value" in {
      (batchHeader.node \ "BillingAuthorityIdentityCode").text shouldBe "9999"
    }

    "contain a ProcessDate element value" in {
      (batchHeader.node \ "ProcessDate").text shouldBe "2018-01-30"
    }

    "contain an EntryDateTime element value" in {
      (batchHeader.node \ "EntryDateTime").text shouldBe "2018-01-30T23:00:22"
    }
  }

  "BAPropertyReports" should {

    "contain 1 report" in {
      batchSubmission.baPropertyReports.size shouldBe (1)
    }
  }

  "A BAPropertyReport" should {

    "contain a DateSent" in {
      (batchSubmission.baPropertyReports.head.node \ "DateSent").text shouldBe "2018-01-30"
    }

    "contain a TransactionIdentityBA" in {
      (batchSubmission.baPropertyReports.head.node \ "TransactionIdentityBA").text shouldBe "22121746115111"
    }

    "contain a BAidentityNumber" in {
      (batchSubmission.baPropertyReports.head.node \ "BAidentityNumber").text shouldBe "9999"
    }

    "contain a BAreportNumber" in {
      (batchSubmission.baPropertyReports.head.node \ "BAreportNumber").text shouldBe "211909"
    }

    "contain a ReasonForReportCode" in {
      (batchSubmission.baPropertyReports.head.node \ "TypeOfTax" \\ "ReasonForReportCode").text shouldBe "CR03"
    }

    "contain a ReasonForReportDescription" in {
      (batchSubmission.baPropertyReports.head.node \ "TypeOfTax" \\ "ReasonForReportDescription").text shouldBe "New"
    }

    "contain a IndicatedDateOfChange" in {
      (batchSubmission.baPropertyReports.head.node \ "IndicatedDateOfChange").text shouldBe "2018-05-01"
    }

    "contain a Remarks" in {
      (batchSubmission.baPropertyReports.head.node \ "Remarks").text shouldBe "THIS IS A BLUEPRINT TEST PLEASE DELETE / NO ACTION THIS REPORT"
    }

    "contain a UniquePropertyReferenceNumber" in {
      (batchSubmission.baPropertyReports.head.node \ "ProposedEntries" \\ "UniquePropertyReferenceNumber").text shouldBe "121102276285"
    }
  }

  "A BAReportTrailer" should {

    val batchTrailer = batchSubmission.batchTrailer

    "contain a RecordCount" in {
      (batchTrailer.node \ "RecordCount").text shouldBe ("8")
    }

    "contain an EntryDateTime" in {
      (batchTrailer.node \ "EntryDateTime").text shouldBe ("2018-01-30T23:01:43")
    }

    "contain a TotalNNDRreportCount" in {
      (batchTrailer.node \ "TotalNNDRreportCount").text shouldBe ("0")
    }

    "contain a TotalCtaxReportCount" in {
      (batchTrailer.node \ "TotalCtaxReportCount").text shouldBe ("8")
    }

  }

  val complexBatchSubmission = xmlParser.parseXml(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))

  "A BAReport containing multiple BAReports" should {

    "be parsed correctly and hold the correct number of reports" in {
      complexBatchSubmission.baPropertyReports.size shouldBe (4)
    }
  }

}
