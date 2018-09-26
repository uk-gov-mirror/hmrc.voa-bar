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
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.voabar.models.{BarXmlValidationError, Error}
import uk.gov.hmrc.voabar.util.{BA_CODE_MATCH, CHARACTER, INVALID_XML_XSD, ONE_EXISTING}

import scala.xml.XML

class XmlValidatorSpec extends PlaySpec with EitherValues {

  val validator = new XmlValidator
  val parser = new XmlParser
  val reportBuilder = new MockBAReportBuilder
  val xmlParser = new XmlParser()

  val valid1 = parser.parse(IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))).right.get
  val valid2 = parser.parse(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))).right.get
  val invalid1 = parser.parse(IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))).right.get
  val invalid2 = parser.parse(IOUtils.toString(getClass.getResource("/xml/CTInvalid2.xml"))).right.get


  "A valid ba batch submission xml file (valid1)" must {
    "validate successfully" in {
      validator.validate(valid1) mustBe ('right)
    }
  }

  "An invalid ba batch submission xml file (invalid1)" must {
    "not validate successfully" in {
      validator.validate(invalid1) mustBe ('left)
      //errors.size mustBe 4
    }
  }

  "A valid ba batch submission xml file (valid2)" must {
    "validate successfully" in {
      validator.validate(valid2) mustBe ('right)
    }
  }

  "An invalid ba batch submission xml file (invalid2)" must {
    "not validate successfully and contain a CouncilTaxBand related error" in {
      validator.validate(invalid2) mustBe ('left)
      //errors.size mustBe 18
      //assert(errors.toString.contains("CouncilTaxBand"))
    }
  }

  "A invalid XML " must {
    "fail for wrong namespace" in {
      val invalidNamespaceDocument = parser.parse(IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
        .replaceAll("http://www.govtalk.gov.uk/LG/Valuebill", "uri:wrong")).right.get

      val result = validator.validate(invalidNamespaceDocument)

      result mustBe ('left)

      result.left.value mustBe BarXmlValidationError(List(Error(INVALID_XML_XSD, List("Cannot find the declaration of element 'BAreports'."))))

    }

    "fail for misspelled root element" in {

      val invalidNamespaceDocument = parser.parse(IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
        .replaceAll("BAreports", "bareports")).right.get

      val result = validator.validate(invalidNamespaceDocument)

      result mustBe ('left)

      result.left.value mustBe BarXmlValidationError(List(Error(INVALID_XML_XSD, List("Cannot find the declaration of element 'bareports'."))))

    }

  }

  val batchWith4Reports = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))


  "another test" should {
    "return a list of 3 errors when the report header contains 1 illegal element and each " +
      "of the 4 reports contains 1 illegal element " in {
      val validBatch = XML.loadString(batchWith4Reports)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, Map(
        "BAreportNumber" -> "WrongElement", "BillingAuthority" -> "IllegalElement",
        "PropertyDescriptionText" -> "BadElement")).toString()

      val documment = xmlParser.parse(invalidBatch)
      val validationResutl = validator.validate(documment.right.get)

      validationResutl must be('left)

      validationResutl.left.value mustBe a[BarXmlValidationError]

      validationResutl.left.value.asInstanceOf[BarXmlValidationError].errors must contain only(
        Error(INVALID_XML_XSD, Seq("Invalid content was found starting with element 'IllegalElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BillingAuthority}' is expected.")),
        Error(INVALID_XML_XSD, Seq("Invalid content was found starting with element 'WrongElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BAreportNumber}' is expected.")),
        Error(INVALID_XML_XSD, Seq("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":PrimaryDescriptionCode, \"http://www.govtalk." +
          "gov.uk/LG/Valuebill\":SecondaryDescriptionCode, \"http://www.govtalk.gov.uk/LG/Valuebill\":PropertyDescriptionText}' is expected.")))

    }

    val batchWith32ReportsWithErrors = IOUtils.toString(getClass.getResource("/xml/res101.xml"))

    "return a list of errors when a batch containing 32 reports has multiple errors" in {
      val invalidBatch = XML.loadString(batchWith32ReportsWithErrors).toString()

      val documment = xmlParser.parse(invalidBatch)
      val validationResutl = validator.validate(documment.right.get)

      validationResutl must be('left)

      validationResutl.left.value mustBe a[BarXmlValidationError]

      validationResutl.left.value.asInstanceOf[BarXmlValidationError].errors must contain only (
        Error(INVALID_XML_XSD,List("Invalid content was found starting with element 'IllegalElement'. One of " +
          "'{\"http://www.govtalk.gov.uk/LG/Valuebill\":EntryDateTime}' is expected.")),
        Error(INVALID_XML_XSD,List("'0£' is not a valid value for 'integer'.")),
        Error(INVALID_XML_XSD,List("The value '0£' of element 'TotalNNDRreportCount' is not valid.")),
        Error(INVALID_XML_XSD,List("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":TypeOfTax}' is expected.")),
        Error(INVALID_XML_XSD,List("Invalid content was found starting with element 'ExistingEntries'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":ProposedEntries, \"http://www.govtalk.gov.uk/LG/Value" +
          "bill\":IndicatedDateOfChange}' is expected.")))
    }

  }

}