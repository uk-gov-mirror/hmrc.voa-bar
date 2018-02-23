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

package uk.gov.hmrc.voabar.models

import org.apache.commons.io.IOUtils
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.voabar.services.XmlParser
import uk.gov.hmrc.voabar.models.errors.Error

class ValidationResultSpec extends PlaySpec {

  val xmlParser = new XmlParser
  val reports = xmlParser.parseXml(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))).baPropertyReports
  val errors = Seq(Error("1", Seq("date", "£")))


  "Given a list of BAPropertyReport and a list of Character Errors produce a CharacterValidationResult model" in {
    val result = ValidationResult(reports, errors)
    result.baPropertyReports.size mustBe 4
    result.baPropertyReports mustBe reports
    result.errors.size mustBe 1
    result.errors mustBe errors
  }

}
