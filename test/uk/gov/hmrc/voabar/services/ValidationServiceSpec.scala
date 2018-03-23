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
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.voabar.models.Error

import scala.xml.{Node, NodeSeq, XML}

class ValidationServiceSpec extends PlaySpec {

  val batchWith4Reports = IOUtils.toString(getClass.getResource("/xml/CTValid2.xml"))

  val xmlParser = new XmlParser
  val xmlValidator = new XmlValidator
  val charValidator = new CharacterValidator
  val reportBuilder = new MockBAReportBuilder
  val validationService = new ValidationService(xmlValidator, xmlParser, charValidator)

  "Validation service" must {

    "return a list of 1 error when a batch report containing 4 reports and: " +
      "-the BACode in the report header does not match that in the HTTP request header" in {

      val validBatch = XML.loadString(batchWith4Reports)
      validationService.validate(validBatch, "XXXX") mustBe List[Error](Error("1010", Seq()))

    }

    "return a list of 2 errors when a batch report containing 4 reports and: " +
      "-the BACode in the report header does not match that in the HTTP request header; " +
      "-the report header contains 1 illegal element" in {

      val validBatch = XML.loadString(batchWith4Reports)
      val invalidBatch = reportBuilder.invalidateBatch(validBatch.head, "BillingAuthority", "BadElement")
      validationService.validate(invalidBatch.head, "XXXX") mustBe List[Error](
        Error("1010", Seq()),
        Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BillingAuthority}' is expected."))
      )

    }

    "return a list of 3 errors when a batch report containing 4 reports and: " +
      "-the BACode in the report header does not match that in the HTTP request header; " +
      "-the report header contains 1 illegal element;" +
      "-each of the 4 reports contains 1 illegal element" in {

      val validBatch = XML.loadString(batchWith4Reports)
      val b = reportBuilder.invalidateBatch(validBatch.head, "BillingAuthority", "BadElement")
      val invalidBatch = reportBuilder.invalidateBatch(b.head, "DateSent", "BadElement")
      validationService.validate(invalidBatch.head, "XXXX") mustBe List[Error](
        Error("1010", Seq()),
        Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":BillingAuthority}' is expected.")),
        Error("cvc-complex-type.2.4.a", Seq("Invalid content was found starting with element 'BadElement'. " +
          "One of '{\"http://www.govtalk.gov.uk/LG/Valuebill\":DateSent}' is expected."))
      )
    }

  }
}
