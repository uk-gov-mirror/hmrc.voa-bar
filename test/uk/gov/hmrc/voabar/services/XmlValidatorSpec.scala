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

import scala.xml.{Node, XML}

class XmlValidatorSpec extends WordSpec {

  val validator = new XmlValidator
  val parser = new XmlParser
  val builder = new MockBAReportBuilder

  val valid1 = IOUtils.toString(getClass.getResource("/xml/CTValid1.xml"))
  val valid2 = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))
  val invalid1 = IOUtils.toString(getClass.getResource("/xml/CTInvalid1.xml"))
  val invalid2 = IOUtils.toString(getClass.getResource("/xml/CTInvalid2.xml"))

  "A valid xml file" should {
    "validate successfully" in {
      val errors = validator.validate(valid1)
      errors should have size 0
    }
  }

  "An invalid xml file" should {
    "not validate successfully" in {
      val errors = validator.validate(invalid1)
      errors should have size 4
    }
  }

  "A valid CT xml file" should {
    "validate successfully" in {
      val errors = validator.validate(valid2)
      errors should have size 0
    }
  }

  "An invalid CT xml file" should {
    "not validate successfully and contain a CouncilTaxBand related error" in {
      val errors = validator.validate(invalid2)
      errors should have size 18
      assert(errors.toString.contains("CouncilTaxBand"))
    }
  }

  "A valid batch submission containing 4 reports" should {
    "validate successfully when parsed into smaller batches" in {
      val report:Node = XML.loadString(valid2)
      val smallBatches = parser.parseBatch(report)

      val result = smallBatches.map{report => validator.validate(report.toString)}
      result.forall(_.isEmpty) shouldBe true
    }
  }

  "An invalid batch submission containing 1 report" should {
    "return with 1 error when parsed" in {
      val report:Seq[Node] = builder.invalidateBatch(XML.loadString(valid1),"ProcessDate","ProcessData")
      val smallBatches = parser.parseBatch(report.head)
      smallBatches.map(batch => validator.validate(batch.toString)).size shouldBe 1
    }

    "An invalid batch submission containing 4 reports" should {
      "return with 4 errors when parsed" in {
        val report:Seq[Node] = builder.invalidateBatch(XML.loadString(valid2),"ProcessDate","ProcessData")
        val smallBatches = parser.parseBatch(report.head)
        smallBatches.map(batch => validator.validate(batch.toString)).size shouldBe 4
      }
    }
  }

}
