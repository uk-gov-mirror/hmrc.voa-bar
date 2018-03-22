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
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatestplus.play.PlaySpec

import scala.xml.{Node, XML}

class XmlValidatorSpec extends PlaySpec {

  val validator = new XmlValidator
  val parser = new XmlParser
  val reportBuilder = new MockBAReportBuilder

  val valid1 = IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))
  val valid2 = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))
  val invalid1 = IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
  val invalid2 = IOUtils.toString(getClass.getResource("/xml/CTInvalid2.xml"))

  "A valid ba batch submission xml file (valid1)" must {
    "validate successfully" in {
      val errors = validator.validate(valid1)
      errors.size mustBe 0
    }
  }

  "An invalid ba batch submission xml file (invalid1)" must {
    "not validate successfully" in {
      val errors = validator.validate(invalid1)
      errors.size mustBe 4
    }
  }

  "A valid ba batch submission xml file (valid2)" must {
    "validate successfully" in {
      val errors = validator.validate(valid2)
      errors.size mustBe 0
    }
  }

  "An invalid ba batch submission xml file (invalid2)" must {
    "not validate successfully and contain a CouncilTaxBand related error" in {
      val errors = validator.validate(invalid2)
      errors.size mustBe 18
      assert(errors.toString.contains("CouncilTaxBand"))
    }
  }

  "A valid batch submission containing 4 reports (valid2)" must {
    "validate successfully when parsed into smaller batches" in {
      val report:Node = XML.loadString(valid2)
      val smallBatches = parser.parseBatch(report)

      val errors = smallBatches.map{report => validator.validate(report.toString)}
      errors.forall(_.isEmpty) mustBe true
    }
  }

  "A batch submission containing 1 report and 1 illegal element within the header" must {
    "return with 1 error when parsed" in {
      val invalidReport: Seq[Node] = reportBuilder.invalidateBatch(XML.loadString(valid1), "ProcessDate", "BadElement")
      val batch = parser.parseBatch(invalidReport.head)
      batch.map(b => validator.validate(b.toString)).size mustBe 1
    }
  }

    "A batch submission containing 4 reports and 1 illegal element within the header" must {
      "return with 4 errors when parsed" in {
        val invalidReport:Seq[Node] = reportBuilder.invalidateBatch(XML.loadString(valid2),"ProcessDate","BadElement")
        val batch = parser.parseBatch(invalidReport.head)
        batch.map(b => validator.validate(b.toString)).size mustBe 4

    }
  }





}
