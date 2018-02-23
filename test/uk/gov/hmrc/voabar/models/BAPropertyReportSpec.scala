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

import org.scalatestplus.play.PlaySpec
import scala.xml.NodeSeq

class BAPropertyReportSpec extends PlaySpec {

  val propertyReportNode: NodeSeq = <BApropertyReport>
    <DateSent>2018-01-30</DateSent>
    <TransactionIdentityBA>22121746115111</TransactionIdentityBA>
    <BAidentityNumber>9999</BAidentityNumber>
    <BAreportNumber>211909</BAreportNumber>
    <TypeOfTax>
      <CtaxReasonForReport>
        <ReasonForReportCode>CR03</ReasonForReportCode>
        <ReasonForReportDescription>New</ReasonForReportDescription>
      </CtaxReasonForReport>
    </TypeOfTax>
    <ProposedEntries>
      <AssessmentProperties>
        <PropertyIdentity>
          <bs7666:UniquePropertyReferenceNumber>121102276285</bs7666:UniquePropertyReferenceNumber>
          <TextAddress>
            <AddressLine>5 VALID STREET</AddressLine>
            <AddressLine>AREA</AddressLine>
            <AddressLine>TOWN</AddressLine>
            <Postcode>AA11 1AA</Postcode>
          </TextAddress>
          <BAreference>22121746115611</BAreference>
        </PropertyIdentity>
        <OccupierContact>
          <OccupierName>
            <pdt:PersonNameTitle>MR</pdt:PersonNameTitle>
            <pdt:PersonGivenName>NAME</pdt:PersonGivenName>
            <pdt:PersonFamilyName>SURNAME</pdt:PersonFamilyName>
            <pdt:PersonRequestedName>MR NAME SURNAME</pdt:PersonRequestedName>
          </OccupierName>
        </OccupierContact>
      </AssessmentProperties>
    </ProposedEntries>
    <IndicatedDateOfChange>2018-05-01</IndicatedDateOfChange>
    <Remarks>THIS IS A BLUEPRINT TEST PLEASE DELETE / NO ACTION THIS REPORT</Remarks>
  </BApropertyReport>

  "Given a NodeSeq representing the property report produce a BAPropertyReport model" in {
    val batchHeader = BatchTrailer(propertyReportNode)
    batchHeader.node mustBe propertyReportNode
  }

}
