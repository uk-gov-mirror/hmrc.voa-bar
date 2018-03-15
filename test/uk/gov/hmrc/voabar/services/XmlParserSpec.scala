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
import uk.gov.hmrc.voabar.models._
import org.scalatest.Matchers._
import uk.gov.hmrc.voabar.models

import scala.xml._

class XmlParserSpec extends WordSpec {

  val xmlParser = new XmlParser()

  val batchSubmission: BABatchReport = xmlParser.fromXml(IOUtils.toString(getClass.getResource("/xml/CTValid1.xml")))

  "Given a valid BAReports xml file, XmlParser" should {
    "return a BatchSubmission object" in {
      batchSubmission shouldBe a [BABatchReport]
    }
  }

  "A BatchSubmission object" should {

    "contain a BAReports" in {
      batchSubmission.baReports shouldBe a [BAReports]
    }

    "contain a BatchHeader" in {
      batchSubmission.baReportHeader shouldBe a [BAReportHeader]
    }

    "contain a list of BAPropertyReports of type BAPropertyReport" in {
      batchSubmission.baPropertyReport.isInstanceOf[List[BAPropertyReport]] shouldBe true
    }

    "contain a BatchTrailer" in {
      batchSubmission.baReportTrailer shouldBe a [BAReportTrailer]
    }
  }


  "A BatchHeader" should {

    val batchHeader = batchSubmission.baReportHeader

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
      batchSubmission.baPropertyReport.size shouldBe 1
    }
  }

  "A BAPropertyReport" should {

    "contain a DateSent" in {
      (batchSubmission.baPropertyReport.head.node \ "DateSent").text shouldBe "2018-01-30"
    }

    "contain a TransactionIdentityBA" in {
      (batchSubmission.baPropertyReport.head.node \ "TransactionIdentityBA").text shouldBe "22121746115111"
    }

    "contain a BAidentityNumber" in {
      (batchSubmission.baPropertyReport.head.node \ "BAidentityNumber").text shouldBe "9999"
    }

    "contain a BAreportNumber" in {
      (batchSubmission.baPropertyReport.head.node \ "BAreportNumber").text shouldBe "211909"
    }

    "contain a ReasonForReportCode" in {
      (batchSubmission.baPropertyReport.head.node \ "TypeOfTax" \\ "ReasonForReportCode").text shouldBe "CR03"
    }

    "contain a ReasonForReportDescription" in {
      (batchSubmission.baPropertyReport.head.node \ "TypeOfTax" \\ "ReasonForReportDescription").text shouldBe "New"
    }

    "contain a IndicatedDateOfChange" in {
      (batchSubmission.baPropertyReport.head.node \ "IndicatedDateOfChange").text shouldBe "2018-05-01"
    }

    "contain a Remarks" in {
      (batchSubmission.baPropertyReport.head.node \ "Remarks").text shouldBe "THIS IS A BLUEPRINT TEST PLEASE DELETE / NO ACTION THIS REPORT"
    }

    "contain a UniquePropertyReferenceNumber" in {
      (batchSubmission.baPropertyReport.head.node \ "ProposedEntries" \\ "UniquePropertyReferenceNumber").text shouldBe "121102276285"
    }
  }

  "A BAReportTrailer" should {

    val batchTrailer = batchSubmission.baReportTrailer

    "contain a RecordCount" in {
      (batchTrailer.node \ "RecordCount").text shouldBe "8"
    }

    "contain an EntryDateTime" in {
      (batchTrailer.node \ "EntryDateTime").text shouldBe "2018-01-30T23:01:43"
    }

    "contain a TotalNNDRreportCount" in {
      (batchTrailer.node \ "TotalNNDRreportCount").text shouldBe "0"
    }

    "contain a TotalCtaxReportCount" in {
      (batchTrailer.node \ "TotalCtaxReportCount").text shouldBe "8"
    }

  }

  val multipleReportBatch = xmlParser.fromXml(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))

  "A BAReport containing multiple BAReports" should {

    "hold the correct number of reports" in {
      multipleReportBatch.baPropertyReport.size shouldBe 4
    }

    "state the correct batch size in the trailer" in {
      (multipleReportBatch.baReportTrailer.node \ "RecordCount").text shouldBe "4"
    }
  }

  "A BA batch submission" should  {

    val report:Node = XML.loadString(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))
    val result = xmlParser.parseBatch(report)

    "be parsed into multiple smaller batches" in {

      result.size shouldBe 4
    }

    "each batch should contain a single (non-empty) header node" in {
      val nonEmptyHeaders:Seq[NodeSeq] = result.map(_ \ "BAreportHeader")

      nonEmptyHeaders.size shouldBe 4
      nonEmptyHeaders.forall(_.size == 1) shouldBe true
    }

    "each batch should contain a single (non-empty) trailer node" in {
      val nonEmptyTrailers:Seq[NodeSeq] = result.map(_ \ "BAreportTrailer")

      nonEmptyTrailers.size shouldBe 4
      nonEmptyTrailers.forall(_.size == 1) shouldBe true
    }

    "each batch should contain a single (non-empty) property report" in {
      val nonEmptyPropertyReports:Seq[NodeSeq] = result.map(_ \ "BApropertyReport")

      nonEmptyPropertyReports.size shouldBe 4
      nonEmptyPropertyReports.forall(_.size == 1) shouldBe true
    }









  }

}
