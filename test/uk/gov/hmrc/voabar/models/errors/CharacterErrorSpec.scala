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

package uk.gov.hmrc.voabar.models.errors

import org.scalatestplus.play.PlaySpec

class CharacterErrorSpec extends PlaySpec {

  val reportNumber = "124456"
  val fieldName = "PropertyDescription"
  val code = 1000
  val additionalInfo = "Some additional information"

  "Given a report number, a field name, a code and some additional information produce a CharacterError model" in {
    val error = CharacterError(reportNumber, fieldName, code, additionalInfo)
    error.reportNumber mustBe reportNumber
    error.fieldName mustBe fieldName
    error.code mustBe code
    error.additionalInformation mustBe additionalInfo
  }

}
