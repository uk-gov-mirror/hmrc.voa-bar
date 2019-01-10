/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.voabar.util._

import scala.xml.{NodeSeq, XML}

class CharacterValidatorSpec extends PlaySpec {

  val validHeader: NodeSeq = <BAreportHeader>
    <BillingAuthority>VALID</BillingAuthority>
    <BillingAuthorityIdentityCode>9999</BillingAuthorityIdentityCode>
    <ProcessDate>2018-01-30</ProcessDate>
    <EntryDateTime>2018-01-30T23:00:22</EntryDateTime>
  </BAreportHeader>

  val invalidTrailer: NodeSeq = <BAreportTrailer>
    <RecordCount>4</RecordCount>
    <EntryDateTime>2017-12-05T16:14:33</EntryDateTime>
    <TotalNNDRreportCount>0£</TotalNNDRreportCount>
    <TotalCtaxReportCount>4</TotalCtaxReportCount>
  </BAreportTrailer>

  val validTestBatchXml =
    """<BAreports SchemaId="BAtoVOA" SchemaVersion="4-0"
            xmlns="http://www.govtalk.gov.uk/LG/Valuebill"
            xmlns:apd="http://www.govtalk.gov.uk/people/AddressAndPersonalDetails"
            xmlns:pdt="http://www.govtalk.gov.uk/people/PersonDescriptives"
            xmlns:bs7666="http://www.govtalk.gov.uk/people/bs7666"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <BAreportHeader>
       <BillingAuthority>VALID</BillingAuthority>
        </BAreportHeader>
        <BApropertyReport>
        <BAreportNumber>200333</BAreportNumber>
        </BApropertyReport>
        <BAreportTrailer>
        <RecordCount>4</RecordCount>
        </BAreportTrailer>
      </BAreports>"""

  val report = <BApropertyReport><DateSent>2018-03-03</DateSent></BApropertyReport>
  val validText = "SOME123'/:@ .-+&()"
  val invalidText = "SOME? <INVALID> £"

  val characterValidator = new CharacterValidator
  val xmlParser = new XmlParser
  val reportBuilder = new MockBAReportBuilder

  val ctValid2 = XML.loadString(IOUtils.toString(getClass.getResource("/xml/CTValid2.xml")))
  val invalid2 = IOUtils.toString(getClass.getResource("/xml/CTInvalid2.xml"))


    "The character validator" must {


      "identify a single invalid char in a batch report containing multiple reports (CTValid2)" in {
        val invalidatedReport:NodeSeq = reportBuilder.invalidateBatch(ctValid2,Map("SOME VALID COUNCIL" ->"SOME VALID£COUNCIL"))
        val errors:Seq[Error] = characterValidator.validateChars(invalidatedReport.head, "test")
        errors mustBe List[Error](Error(CHARACTER ,Seq("test", "BillingAuthority","SOME VALID£COUNCIL")))
      }

      "identify multiple invalid chars in a batch report containing multiple reports (CTValid2)" in {
        val invalid = reportBuilder.invalidateBatch(ctValid2, Map(
          "SOME VALID COUNCIL" -> "SOME VALID£COUNCIL",
          "SOME ADMIN AREA" -> "some admin area"))
        val errors:Seq[Error] = characterValidator.validateChars(invalid.head, "test")
        errors mustBe List[Error](Error(CHARACTER, Seq("test", "BillingAuthority", "SOME VALID£COUNCIL")),
          Error(CHARACTER,Seq("test", "AdministrativeArea", "some admin area")))
      }
    }
}
