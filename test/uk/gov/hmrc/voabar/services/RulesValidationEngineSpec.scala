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

import javax.xml.transform.stream.StreamSource
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import services.EbarsValidator

/**
  * Created by rgallet on 09/12/15.
  */
class RulesValidationEngineSpec extends PlaySpec with GuiceOneAppPerSuite {

  "RulesValidationEngine" must {
    "populate PRNs and uuid from json" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR01_ProposedEntries.json")))

      new RulesValidationEngine().applyRules(reports).propertyReferenceNumbers must contain(List(10091165174L))
      new RulesValidationEngine().applyRules(reports).message must be("ReportValidation")
    }

    "populate PRNs and uuid from xml" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromXml(new StreamSource(getClass.getResourceAsStream("/xml/CARDIFF_EDITED_CRCD_RMRKS_CR08.xml")))

      new RulesValidationEngine().applyRules(reports).propertyReferenceNumbers must contain(List(100100069553L))
      new RulesValidationEngine().applyRules(reports).message must be("ReportValidation")
    }

    "not have postcode errors" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromXml(new StreamSource(getClass.getResourceAsStream("/xml/RulesValidationEngine/ShouldNotHavePostcodeErrors.xml")))

      val result = new RulesValidationEngine().applyRules(reports)

      result.errors must have size (0)
    }
  }

  "TextAddressPostcodeValidation" must {
    "valid postcode" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR08_BothEntries.json")))

      val result = TextAddressPostcodeValidation.apply(reports)

      result must be(None)
    }

    "have no error" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromXml(new StreamSource(getClass.getResourceAsStream("/xml/RulesValidationEngine/SingleReport_Postcodeerror.xml")))

      val result = TextAddressPostcodeValidation.apply(reports)

      result must be(None)
    }

    "invalid postcode" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR08_InvalidPostcode.json")))

      val result = TextAddressPostcodeValidation.apply(reports)

      result.get.code must be("TextAddressPostcodeValidation")
      // result.get.value must be("Postcode CrapCrap in this report is invalid.") // no messages
    }
  }

  "OccupierContactAddressesPostcodeValidation" must {
    "valid postcode" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR08_BothEntries.json")))

      val result = OccupierContactAddressesPostcodeValidation.apply(reports)

      result must be(None)
    }

    "have no error" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromXml(new StreamSource(getClass.getResourceAsStream("/xml/RulesValidationEngine/SingleReport_Postcodeerror.xml")))

      val result = OccupierContactAddressesPostcodeValidation.apply(reports)

      result must be(None)
    }

    "invalid postcode" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR08_InvalidPostcode.json")))

      val result = OccupierContactAddressesPostcodeValidation.apply(reports)

      result.get.code must be("OccupierContactAddressesPostcodeValidation")
      //result.get.value must be("Postcode CropCrop in this report is invalid.") //We don't have messages in place
    }
  }

  "RemarksValidation" must {
    "valid remarks" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR08_BothEntries.json")))

      val result = RemarksValidation.apply(reports)

      result must be(None)
    }

    "invalid remarks - too long" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesValidationEngine/Cornwall_CTax_InvalidStreetDescription1.json")))

      val result = RemarksValidation.apply(reports)

      result.get.code must be("RemarksValidation")
      //result.get.value must be("The remarks cannot exceed 240 characters. Please shorten your comments and resubmit this report.") //no messages
    }

    "invalid remarks - too short" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesValidationEngine/Cornwall_CTax_InvalidStreetDescription2.json")))

      val result = RemarksValidation.apply(reports)

      result.get.code must be("RemarksValidation")
      //result.get.value must be("Remarks must not be empty.") //no messages
    }
  }

  "PropertyPlanReferenceNumberValidation" must {
    "valid remarks" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesCorrectionEngine/Cornwall_CTax_CR08_BothEntries.json")))

      val result = PropertyPlanReferenceNumberValidation.apply(reports)

      result must be(None)
    }

    "invalid PropertyPlanReferenceNumber - too long" in {
      val ebarsValidator = new EbarsValidator
      val reports = ebarsValidator.fromJson(new StreamSource(getClass.getResourceAsStream("/json/RulesValidationEngine/Cornwall_CTax_InvalidStreetDescription1.json")))

      val result = PropertyPlanReferenceNumberValidation.apply(reports)

      result.get.code must be("PropertyPlanReferenceNumberValidation")
      //result.get.value must be("The Property Plan Reference Number must be between 1 and 25 characters.") //no messages
    }
  }
}
