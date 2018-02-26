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


import com.typesafe.config.ConfigFactory
import play.api.Configuration
import uk.gov.hmrc.voabar.SpecBase
import uk.gov.hmrc.voabar.utils.Initialize

import scala.collection.JavaConversions._

class EmailSpec extends SpecBase {

  val init = injector.instanceOf[Initialize]
  val baRefNumber = "13432121232"
  val fileName = "sample.xml"
  val dateSubmitted = "2017-08-30"
  val errorList = ""
  val email = Email(baRefNumber, fileName, dateSubmitted, errorList, init)

  "creating an email from valid input results in a map of parameters containing a baRefNumber key set to a value of 13432121232" in {
    email.parameters.getOrElse("baRefNumber", "") mustBe s"BA : $baRefNumber"
  }

  "creating an email from valid input results in a map of parameters containing a fileName key set to a value of sample.xml" in {
    email.parameters.getOrElse("fileName", "") mustBe s"File name : $fileName"
  }

  "creating an email from valid input results in a map of parameters containing a dateSubmitted key set to a value of 2017-08-30" in {
    email.parameters.getOrElse("dateSubmitted", "") mustBe s"Date Submitted : $dateSubmitted"
  }

  "creating an email from valid input results in a map of parameters containing an empty errorList key value" in {
    email.parameters.getOrElse("errorList", "") mustBe s"Errors : $errorList"
  }

  "the voa email address retrieved from configuration should be BARS@voa.gsi.gov.uk" in {
    val data = Map("email.voa" -> "BARS@voa.gsi.gov.uk")
    val conf = new Configuration(ConfigFactory.parseMap(data))
    val init = new Initialize(conf)
    Email(baRefNumber, fileName, dateSubmitted, errorList, init).to(0) mustBe "BARS@voa.gsi.gov.uk"
  }
}
