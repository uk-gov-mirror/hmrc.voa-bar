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

import scala.xml.{Node, XML}

class ValidationServiceSpec extends PlaySpec {

  val batchWith1Report = IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))
  val batchWith4Reports = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))
  val batchWith32Reports = IOUtils.toString(getClass.getResource("/xml/res100.xml"))
  val batchWith32ReportsWithErrors = IOUtils.toString(getClass.getResource("/xml/res101.xml"))


  val xmlParser = new XmlParser
  val xmlValidator = new XmlValidator
  val charValidator = new CharacterValidator
  val reportBuilder = new MockBAReportBuilder
  def businessRules(baCode:String):BusinessRules = new BusinessRules()(FakeRequest("GET","").
    withHeaders("BA-Code" -> baCode))
  def validationService(baCode:String): ValidationService = new ValidationService(
    xmlValidator, xmlParser, charValidator, businessRules(baCode))

  "Validation service" must {

    "return an empty list (no errors) when passed a valid batch with one report" in {
        val validBatch:Node = XML.loadString(batchWith1Report)
        validationService("9999").validate(validBatch).isEmpty mustBe true
    }

    "return an empty list (no errors) when passed a valid batch with 4 reports" in {
      val validBatch:Node = XML.loadString(batchWith4Reports)
      validationService("9999").validate(validBatch).isEmpty mustBe true
    }

    "return an empty list (no errors) when passed a valid batch with 32 reports" in {
      val validBatch:Node = XML.loadString(batchWith32Reports)
      validationService("5243").validate(validBatch).isEmpty mustBe true
    }

    "return a list of 1 error when the BACode in the report header does " +
      "not match that in the HTTP request header" in {
      val validBatch = XML.loadString(batchWith1Report)
      validationService("0000").validate(validBatch) mustBe List[Error](Error(
        "1010", Seq()))
    }


    "return a list of 2 errors when the BACode in the report header does " +
      "not match that in the HTTP request header and the report header contains 1 illegal element" in {
      val validBatch = XML.loadString(batchWith1Report)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map("BillingAuthority" -> "BadElement"))
      validationService("0000").validate(invalidBatch.head) mustBe List[Error](
        Error("1010", Seq()),
        Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BillingAuthority}' is expected."))
      )
    }

    "return a list of 3 errors when the BACode in the report batch header of a batch of 4 reports does " +
      "not match that in the HTTP request header, the report header contains 1 illegal element and each " +
      "of the 4 reports contains 1 illegal element " in {
      val validBatch = XML.loadString(batchWith4Reports)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map(
        "BAreportNumber" -> "WrongElement", "BillingAuthority" -> "IllegalElement",
        "PropertyDescriptionText" -> "BadElement"))

       validationService("0000").validate(invalidBatch.head) mustBe List[Error](
         Error("1010", Seq()),
         Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'IllegalElement'. " +
           "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BillingAuthority}' is expected.")),
         Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'WrongElement'. " +
           "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BAreportNumber}' is expected.")),
         Error("cvc-complex-type.2.4.a",Seq("Invalid content was found starting with element 'BadElement'. " +
           "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":PrimaryDescriptionCode, \"http://www.govtalk." +
           "gov.uk/LG/Valuebill\":SecondaryDescriptionCode, \"http://www.govtalk.gov.uk/LG/Valuebill\":PropertyDescriptionText}' is expected.")))

    }

    "return a list of 3 errors when the BACode in the report batch header of a batch of 4 reports does " +
      "not match that in the HTTP request header, the report header contains 1 illegal element, " +
      "the BillingAuthority name contains illegal chars and each of the 4 reports contains 1 illegal element " in {
      val validBatch = XML.loadString(batchWith4Reports)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map(
        "BAreportNumber" -> "WrongElement", "BillingAuthority" -> "IllegalElement",
        "PropertyDescriptionText" -> "BadElement", "SOME VALID COUNCIL" -> "Some Valid Council"))

      validationService("0000").validate(invalidBatch.head) mustBe List[Error](
        Error("1010", Seq()),
        Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'IllegalElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BillingAuthority}' is expected.")),
        Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'WrongElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BAreportNumber}' is expected.")),
        Error("cvc-complex-type.2.4.a",Seq("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":PrimaryDescriptionCode, \"http://www.govtalk." +
          "gov.uk/LG/Valuebill\":SecondaryDescriptionCode, \"http://www.govtalk.gov.uk/LG/Valuebill\":PropertyDescriptionText}' is expected.")),
        Error("1000",Seq("Header","IllegalElement","Some Valid Council"))
      )
    }

    "return a list of 1 error when a batch containing 1 report has an illegal char within the property report" in {
      val validBatch = XML.loadString(batchWith1Report)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map(
        "NAME" -> "name"
      ))

      validationService("9999").validate(invalidBatch.head) mustBe List[Error](
        Error("1000",Seq("211909","PersonGivenName","name"))
      )
    }

    "return a list of errors when a batch containing 32 reports has multiple errors" in {
      val invalidBatch = XML.loadString(batchWith32ReportsWithErrors)
      validationService("8888").validate(invalidBatch.head) mustBe List(
        Error("1010",List()),
        Error("cvc-complex-type.2.4.a",List("Invalid content was found starting with element 'IllegalElement'. One of " +
          "'{\"http://www.govtalk.gov.uk/LG/Valuebill\":EntryDateTime}' is expected.")),
        Error("cvc-datatype-valid.1.2.1",List("'0£' is not a valid value for 'integer'.")),
        Error("cvc-type.3.1.3",List("The value '0£' of element 'TotalNNDRreportCount' is not valid.")),
        Error("1000",List("Trailer", "IllegalElement", "Some text")),
        Error("1000",List("Trailer", "TotalNNDRreportCount", "0£")),
        Error("1000",List("138161", "StreetDescription", "23 NeW ST")),
        Error("cvc-complex-type.2.4.a",List("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":TypeOfTax}' is expected.")),
        Error("1000",List("138156", "Town", "WIMBLETOWN£")),
        Error("cvc-complex-type.2.4.a",List("Invalid content was found starting with element 'ExistingEntries'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":ProposedEntries, \"http://www.govtalk.gov.uk/LG/Value" +
          "bill\":IndicatedDateOfChange}' is expected.")),
        Error("1007",List("138159", "CR09")))
    }
  }
}
