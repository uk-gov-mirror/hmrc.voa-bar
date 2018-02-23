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

import java.lang.RuntimeException

import org.apache.commons.io.IOUtils
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.voabar.models.{BAPropertyReport, BatchHeader, BatchTrailer}

import scala.xml.{Node, NodeSeq}

class CharacterValidatorSpec extends PlaySpec {

  val validHeader: NodeSeq = <BAreportHeader>
    <BillingAuthority>VALID</BillingAuthority>
    <BillingAuthorityIdentityCode>9999</BillingAuthorityIdentityCode>
    <ProcessDate>2018-01-30</ProcessDate>
    <EntryDateTime>2018-01-30T23:00:22</EntryDateTime>
  </BAreportHeader>

  val invalidTrailer: NodeSeq = <BAreportTrailer>
    <RecordCount>4</RecordCount>
    <EntryDateTime>2017-12-05T16:14:33</EntryDateTime>
    <TotalNNDRreportCount>0£</TotalNNDRreportCount>
    <TotalCtaxReportCount>4</TotalCtaxReportCount>
  </BAreportTrailer>

  val validTestBatchXml =
    """<BAreports SchemaId="BAtoVOA" SchemaVersion="4-0"
            xmlns="http://www.govtalk.gov.uk/LG/Valuebill"
            xmlns:apd="http://www.govtalk.gov.uk/people/AddressAndPersonalDetails"
            xmlns:pdt="http://www.govtalk.gov.uk/people/PersonDescriptives"
            xmlns:bs7666="http://www.govtalk.gov.uk/people/bs7666"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <BAreportHeader>
       <BillingAuthority>VALID</BillingAuthority>
        </BAreportHeader>
        <BApropertyReport>
        <BAreportNumber>200333</BAreportNumber>
        </BApropertyReport>
        <BAreportTrailer>
        <RecordCount>4</RecordCount>
        </BAreportTrailer>
      </BAreports>"""

  val report = <BApropertyReport><DateSent>2018-03-03</DateSent></BApropertyReport>
  val validText = "SOME123'/:@ .-+&()"
  val invalidText = "SOME? <INVALID> £"

  val characterValidator = new CharacterValidator
  val xmlParser = new XmlParser

  val batchSubmission = xmlParser.parseXml(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))
  val fakeBatch = xmlParser.parseXml(validTestBatchXml)

  "Character Validator" must {

    "The elementNodes should throw an exception if the NodeSeq is empty" in {
      intercept[Exception] {
        val result = characterValidator.elementNodes(NodeSeq.Empty)
        result mustBe a[RuntimeException]
      }
    }

    "The elementNodes should return a sequence of distinct end point nodes given a non empty NodeSeq" in {
      val result = characterValidator.elementNodes(validHeader)
      result.isEmpty mustBe false
      result.size mustBe 4
    }

    "The validateString method should return true if the element has valid characters only" in {
      characterValidator.validateString(validText) mustBe true
    }

    "The validateString method should return false if the element contains invalid characters" in {
      val result = characterValidator.validateString(invalidText)
      result mustBe false
    }

    "The validateHeader method should return an empty sequence if the header doesn't contain any invalid elements" in {
      val batchHeader = BatchHeader(validHeader)
      val result = characterValidator.validateHeader(batchHeader)
      result.isEmpty mustBe true
    }

    "The validateHeader method should return a sequence of errors if the header contains invalid elements" in {
      val batchHeader = batchSubmission.batchHeader
      val result = characterValidator.validateHeader(batchHeader)
      result.isEmpty mustBe false
      result.size mustBe 1
    }

    "The validateTrailer method should return an empty sequence if the header doesn't contain any invalid elements" in {
      val batchTrailer = batchSubmission.batchTrailer
      val result = characterValidator.validateTrailer(batchTrailer)
      result.isEmpty mustBe true
    }

    "The validateTrailer method should return a sequence of errors if the header contains invalid elements" in {
      val batchTrailer = BatchTrailer(invalidTrailer)
      val result = characterValidator.validateTrailer(batchTrailer)
      result.isEmpty mustBe false
      result.size mustBe 1
    }

    "The validateBAPropertyReports method should return a sequence of errors if the reports contain invalid elements and " +
      "no remaining reports if all contain errors" in {
      val baPropertyReports = batchSubmission.baPropertyReports
      val result = characterValidator.validateBAPropertyReports(baPropertyReports)
      result._1.size mustBe 0
      result._2.size mustBe 14
    }

    "The validateBAPropertyReports method should return an empty list of errors and a list of remaining reports if all reports are valid" in {
      val baPropertyReports = fakeBatch.baPropertyReports
      val result = characterValidator.validateBAPropertyReports(baPropertyReports)
      result._1.size mustBe 1
      result._2.size mustBe 0
    }

    "The validatePropertyReport method should return a Left(Seq[Error]) for invalid reports" in {
      val baPropertyReport = batchSubmission.baPropertyReports.head
      val result = characterValidator.validatePropertyReport(baPropertyReport)
      result.isLeft mustBe true
    }

    "The validatePropertyReport method should return a Right(BAPropertyReport) for valid reports" in {
      val baPropertyReport = fakeBatch.baPropertyReports.head
      val result = characterValidator.validatePropertyReport(baPropertyReport)
      result.isRight mustBe true
    }

    "The getPropertyReportNumber should return a report number for an existing BAPropertyReport" in {
     val result = characterValidator.getPropertyReportNumber(batchSubmission.baPropertyReports.head)
      result mustBe "200000"
    }

    "The getPropertyReportNumber should return an empty string if the report number is missing" in {
      val result = characterValidator.getPropertyReportNumber(BAPropertyReport(report))
      result.isEmpty mustBe true
    }

    "The charactersValidationStatus should return a ValidationResult containing the remaining reports and all character errors if any" in {
      val result = characterValidator.charactersValidationStatus(batchSubmission)
      result.baPropertyReports.size mustBe 0
      result.errors.size mustBe 15
    }

    "The charactersValidationStatus should return a ValidationResult containing the remaining reports and no character errors for a valid batch" in {
      val result = characterValidator.charactersValidationStatus(fakeBatch)
      result.baPropertyReports.size mustBe 1
      result.errors.size mustBe 0
    }
  }

}
