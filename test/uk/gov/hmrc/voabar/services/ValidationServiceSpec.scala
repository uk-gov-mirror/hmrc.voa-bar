/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.voabar.models.Error
import uk.gov.hmrc.voabar.util._

import scala.xml.{Node, XML}

class ValidationServiceSpec extends PlaySpec {

  val batchWith1Report = IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))
  val batchWith4Reports = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))
  val batchWith32Reports = IOUtils.toString(getClass.getResource("/xml/res100.xml"))
  val batchWith32ReportsWithErrors = IOUtils.toString(getClass.getResource("/xml/res101.xml"))
  val batchWithWrongBaCodeInSubreport = IOUtils.toString(getClass.getResource("/xml/CTInvalidBAidentityNumber.xml"))

  val BA_LOGIN = "BA5090"


  val xmlParser = new XmlParser
  val xmlValidator = new XmlValidator
  val reportBuilder = new MockBAReportBuilder
  val businessRules= new BusinessRules()
  def validationService(baCode:String): ValidationService = new ValidationService(
    xmlValidator, xmlParser, businessRules)

  "Validation service" must {

    "sucessfully validate correct XML document" in {
      val xmlBatchSubmissionAsString = IOUtils.toByteArray(getClass.getResource("/xml/CTValid1.xml"))
      val validationResult = validationService("9999").validate(xmlBatchSubmissionAsString, BA_LOGIN)
      validationResult mustBe ('right)
    }

    "return Left for not valid XML" in {
      val xmlBatchSubmissionAsString =  IOUtils.toByteArray(getClass.getResource("/xml/CTInvalid1.xml"))
      val validationResult = validationService("9999").validate(xmlBatchSubmissionAsString, BA_LOGIN)
      validationResult mustBe ('left)
    }

    "return an empty list (no errors) when passed a valid batch with one report" in {
      val validBatch: Node = XML.loadString(batchWith1Report)
      validationService("9999").xmlNodeValidation(validBatch, BA_LOGIN).isEmpty mustBe true
    }

    "return an empty list (no errors) when passed a valid batch with 4 reports" in {
      val validBatch: Node = XML.loadString(batchWith4Reports)
      validationService("9999").xmlNodeValidation(validBatch, BA_LOGIN).isEmpty mustBe true
    }

    "return an empty list (no errors) when passed a valid batch with 32 reports" in {
      val validBatch: Node = XML.loadString(batchWith32Reports)
      validationService("5243").xmlNodeValidation(validBatch, "BA5243").isEmpty mustBe true
    }

    "return a list of 1 error when the BACode in the report header does " +
      "not match that in the HTTP request header" in {
      val validBatch = XML.loadString(batchWith1Report)
      validationService("0000").xmlNodeValidation(validBatch, "0000") mustBe List[Error](Error(
        BA_CODE_MATCH, Seq()))
    }


    "return a list of 1 errors when the BACode in the report header does " +
      "not match that in the HTTP request header" in {
      val validBatch = XML.loadString(batchWith1Report)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map("BillingAuthority" -> "BadElement"))
      validationService("0000").xmlNodeValidation(invalidBatch.head, "0000") mustBe List[Error](
        Error(BA_CODE_MATCH, Seq())
      )
    }

    "return an empty list (no errors) when a batch containing 1 report has an illegal char within the property report" in {
      val validBatch = XML.loadString(batchWith1Report)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map(
        "NAME" -> "name"
      ))

      validationService("9999").xmlNodeValidation(invalidBatch.head, BA_LOGIN).isEmpty mustBe true
    }

    "return a list with 2 errors for wrong and missing BAidentityNumber in subreport" in {
      val invalidBatch = XML.loadString(batchWithWrongBaCodeInSubreport)

      validationService("BA9999").xmlNodeValidation(invalidBatch.head, "BA9999") must contain theSameElementsAs List (
        Error(BA_CODE_MATCH, Seq()), Error(BA_CODE_REPORT, Seq("'BAidentityNumber' missing."))
      )

    }

  }

}
