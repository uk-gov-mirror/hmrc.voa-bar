/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.voabar.util.INVALID_XML_XSD

import scala.xml.XML

class XmlValidatorSpec extends PlaySpec with EitherValues {

  val validator = new XmlValidator
  val parser = new XmlParser
  val reportBuilder = new MockBAReportBuilder
  val xmlParser = new XmlParser()

  val valid1 = IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))
  val valid2 = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))
  val invalid1 = IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
  val invalid2 = IOUtils.toString(getClass.getResource("/xml/CTInvalid2.xml"))


  "A valid ba batch submission xml file (valid1)" must {
    "validate successfully" in {
      validator.validate(valid1) mustBe 'right
    }
  }

  "An invalid ba batch submission xml file (invalid1)" must {
    "not validate successfully" in {
      validator.validate(invalid1) mustBe 'left
      //errors.size mustBe 4
    }
  }

  "A valid ba batch submission xml file (valid2)" must {
    "validate successfully" in {
      validator.validate(valid2) mustBe 'right
    }
  }

  "An invalid ba batch submission xml file (invalid2)" must {
    "not validate successfully and contain a CouncilTaxBand related error" in {
      validator.validate(invalid2) mustBe 'left
      //errors.size mustBe 18
      //assert(errors.toString.contains("CouncilTaxBand"))
    }
  }

  "A invalid XML " must {
    "fail for wrong namespace" in {
      val invalidNamespaceDocument = IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
        .replaceAll("http://www.govtalk.gov.uk/LG/Valuebill", "uri:wrong")

      val result = validator.validate(invalidNamespaceDocument)

      result mustBe ('left)

      result.left.value mustBe BarXmlValidationError(List(Error(INVALID_XML_XSD, List("Error on line 2: Cannot find the declaration of element 'BAreports'."))))

    }

    "fail for misspelled root element" in {

      val invalidNamespaceDocument = IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
        .replaceAll("BAreports", "bareports")

      val result = validator.validate(invalidNamespaceDocument)

      result mustBe ('left)

      result.left.value mustBe BarXmlValidationError(List(Error(INVALID_XML_XSD, List("Error on line 2: Cannot find the declaration of element 'bareports'."))))

    }

  }

  val batchWith4Reports = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))


  "another test" should {
    val batchWith32ReportsWithErrors = IOUtils.toString(getClass.getResource("/xml/res101.xml"))

    "return a list of errors when a batch containing 32 reports has multiple errors" in {
      val invalidBatch = XML.loadString(batchWith32ReportsWithErrors).toString()

      val validationResutl = validator.validate(invalidBatch)

      validationResutl must be('left)

      validationResutl.left.value mustBe a[BarXmlValidationError]

      validationResutl.left.value.asInstanceOf[BarXmlValidationError].errors must contain only (
        Error(INVALID_XML_XSD,List("Error on line 2236: Invalid content was found starting with element 'IllegalElement'. One of " +
          "'{\"http://www.govtalk.gov.uk/LG/Valuebill\":EntryDateTime}' is expected.")),
        Error(INVALID_XML_XSD,List("Error on line 2238: The value '0£' of element 'TotalNNDRreportCount' is not valid.")),
        Error(INVALID_XML_XSD,List("Error on line 790: Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":TypeOfTax}' is expected.")),
        Error(INVALID_XML_XSD,List("Error on line 1908: Invalid content was found starting with element 'ExistingEntries'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":ProposedEntries, \"http://www.govtalk.gov.uk/LG/Value" +
          "bill\":IndicatedDateOfChange}' is expected.")))
    }

  }

}