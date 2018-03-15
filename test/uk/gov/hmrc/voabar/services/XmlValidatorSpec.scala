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

  "A valid xml file" should {
    "validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml/CTValid1.xml")))
      errors should have size 0
    }
  }

  "An invalid xml file" should {
    "not validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml//CTInvalid1.xml")))
      errors should have size 4
    }
  }

  "A valid CT xml file" should {
    "validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))
      errors should have size 0
    }
  }

  "An invalid CT xml file" should {
    "not validate successfully and contain a CouncilTaxBand related error" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml/CTInvalid2.xml")))
      errors should have size 18
      assert(errors.toString.contains("CouncilTaxBand"))
    }
  }

  "A valid batch submission containing 4 reports" should {
    "validate successfully when parsed into smaller batches" in {
      val report:Node = XML.loadString(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))
      val smallBatches = parser.parseBatch(report)

      val result = smallBatches.map{report => validator.validate(report.toString)}
      result.forall(_.isEmpty) // no errors
    }
  }

}
