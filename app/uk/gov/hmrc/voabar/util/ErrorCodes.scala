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

package uk.gov.hmrc.voabar.util

object ErrorCodes {

  val CHARACTER = "1000"
  val ONE_PROPOSED = "1001"
  val NONE_EXISTING = "1002"
  val EITHER_ONE_EXISTING_OR_ONE_PROPOSED = "1003"
  val ATLEAST_ONE_PROPOSED = "1004"
  val ATLEAST_ONE_EXISTING = "1005"
  val NOT_IN_USE = "1006"
  val ONE_EXISTING = "1007"
  val NONE_PROPOSED = "1008"
  val BA_CODE_MATCH = "1010"
  val BA_CODE_REQHDR = "1011"
  val BA_CODE_REPORT = "1012"
  val UNSUPPORTED_TAX_TYPE = "1020"
  val UNKNOWN_TYPE_OF_TAX = "1021"
  val UNKNOWN_DATA_OBJECT = "1022"

}
