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
import uk.gov.hmrc.voabar.models.{BatchHeader, BatchTrailer}

import scala.xml.NodeSeq

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

  val validText = "SOME123'/:@ .-+&()"
  val invalidText = "SOME? <INVALID> £"

  val validChar = "A"
  val invalidChar = "%"

  val characterValidator = new CharacterValidator
  val xmlParser = new XmlParser
  val batchSubmission = xmlParser.parseXml(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))

  "Character Validator" must {

    "The validateCharacter method should return true if the character is valid" in {
      characterValidator.validateCharacter(validChar) mustBe true
    }

    "The validateCharacter method should return false if the character is invalid" in {
      characterValidator.validateCharacter(invalidChar) mustBe false
    }

    "The validateString method should return an empty list if the element doesn't contain any invalid characters" in {
      characterValidator.validateString(validText, "label", "reportNo").isEmpty mustBe true
    }

    "The validateString method should return a list of errors if the element contains invalid characters" in {
      val result = characterValidator.validateString(invalidText, "label", "reportNo")
      result.isEmpty mustBe false
      result.size mustBe 4
    }

    "The validateHeader method should return an empty list if the header doesn't contain any invalid elements" in {
      val batchHeader = BatchHeader(validHeader)
      val result = characterValidator.validateHeader(batchHeader)
      result.isEmpty mustBe true
    }

    "The validateHeader method should return a list of errors if the header contains invalid elements" in {
      val batchHeader = batchSubmission.batchHeader
      val result = characterValidator.validateHeader(batchHeader)
      result.isEmpty mustBe false
      result.size mustBe 13
    }

    "The validateTrailer method should return an empty list if the header doesn't contain any invalid elements" in {
      val batchTrailer = batchSubmission.batchTrailer
      val result = characterValidator.validateTrailer(batchTrailer)
      result.isEmpty mustBe true
    }

    "The validateTrailer method should return a list of errors if the header contains invalid elements" in {
      val batchTrailer = BatchTrailer(invalidTrailer)
      val result = characterValidator.validateTrailer(batchTrailer)
      result.isEmpty mustBe false
      result.size mustBe 1
    }

    "The validateBAPropertyReports method should return a list of errors if the reports contain invalid elements" in {
      val baPropertyReports = batchSubmission.baPropertyReports
      val result = characterValidator.validateBAPropertyReports(baPropertyReports)
      result.foreach(println)
    }

    "The charactersValidationStatus should return a CharacterValidationResult containing the remaining reports and all character errors if any" in {
      val result = characterValidator.charactersValidationStatus(batchSubmission)
      result.baPropertyReports.size mustBe 0
      result.characterErrors.size mustBe 127
    }
  }

}
