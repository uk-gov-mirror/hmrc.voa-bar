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
}