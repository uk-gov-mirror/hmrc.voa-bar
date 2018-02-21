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

class XmlValidatorSpec extends WordSpec {

  val validator =new XmlValidator

  "A valid xml file" should {
    "validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml/valid.xml")))
      errors.error should have size 0
    }
  }

  "An invalid xml file" should {
    "not validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml//invalid.xml")))
      errors.error should have size 4
    }
  }

  "A valid CT xml file" should {
    "validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml/CTValid.xml")))
      errors.error should have size 0
    }
  }

  "An invalid CT xml file" should {
    "not validate successfully" in {
      val errors = validator.validate(IOUtils.toString(getClass.getResource("/xml/CTInvalid.xml")))
      errors.error should have size 18
      assert(errors.error.toString.contains("CouncilTaxBand"))
    }
  }

}