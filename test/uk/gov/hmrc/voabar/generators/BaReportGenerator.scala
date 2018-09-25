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

package uk.gov.hmrc.voabar.generators

import java.math.BigInteger
import java.util.GregorianCalendar

import ebars.xml.BApropertySplitMergeStructure.AssessmentProperties
import ebars.xml.BApropertySplitMergeStructure.AssessmentProperties.CurrentTax
import ebars.xml.BApropertySplitMergeStructure.AssessmentProperties.CurrentTax.RateableValue
import ebars.xml.BAreportBodyStructure.TypeOfTax
import ebars.xml.BAreportBodyStructure.TypeOfTax.CtaxReasonForReport
import ebars.xml.{BApropertySplitMergeStructure, PersonNameStructure, _}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import org.scalacheck.Gen

import collection.JavaConverters._
import org.scalacheck.util.Buildable._

object BaReportGenerator {

  val xmlGregorianCalendar = for {
    calendar <- Gen.calendar
  }yield {
    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar.asInstanceOf[GregorianCalendar])
  }

  val objectFactory = new ObjectFactory

  lazy val baReportGenerator: Gen[BAreports] = for {
    header <- reportHeader
    propertyReports <- Gen.containerOfN[List, BAreportBodyStructure](1,reportBodyStructure)
    reportTrailer <- reportTrailerGen
  }yield {
    val report = new BAreports()
    report.setBAreportHeader(header)
    report.getBApropertyReport.addAll(propertyReports.asJava)

    reportTrailer.setRecordCount(BigInteger.valueOf(propertyReports.size))
    reportTrailer.setTotalNNDRreportCount(BigInteger.valueOf(0))
    reportTrailer.setTotalCtaxReportCount(BigInteger.valueOf(propertyReports.size))
    report.setBAreportTrailer(reportTrailer)


    report
  }


  val reportTrailerGen: Gen[ReportTrailerStructure] = for {
    entryDateTime <- xmlGregorianCalendar

  }yield {
    val reportTrailer = new ReportTrailerStructure()
    reportTrailer.setEntryDateTime(entryDateTime)
    reportTrailer
  }


  val reportHeader: Gen[ReportHeaderStructure] = for {
    billingAuthority <- Gen.alphaLowerStr
    billingAuthorityIdentityCode <- Gen.chooseNum(0,100000)
    processDate <- Gen.calendar
    entryDateTime <- Gen.calendar
  }yield {
    val header = new ReportHeaderStructure()
    header.setBillingAuthority(billingAuthority)
    header.setBillingAuthorityIdentityCode(billingAuthorityIdentityCode)

    header.setProcessDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(processDate.asInstanceOf[GregorianCalendar]))
    header.setEntryDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(entryDateTime.asInstanceOf[GregorianCalendar]))

    header
  }

  val reportBodyStructure: Gen[BAreportBodyStructure] = for {
    s <- Gen.alphaLowerStr
    dateSend <- xmlGregorianCalendar
    transactionIdentity <- Gen.chooseNum(100,100000)
    baIdentityNumber <- Gen.chooseNum(1, 9999)
    baReportNumber <- Gen.chooseNum(1,10000)
    typeOfTax <- typeOfTaxGen
    proposedEntries <- proposedEntriesGen
    indicatedDateOfChange <- xmlGregorianCalendar
  }yield {
    val body = new BAreportBodyStructure()
    val f = new ebars.xml.ObjectFactory()

    body.getContent.add(f.createBAreportBodyStructureDateSent(dateSend))
    body.getContent.add(f.createBAreportBodyStructureTransactionIdentityBA(transactionIdentity.toString))
    body.getContent.add(f.createBAreportBodyStructureBAidentityNumber(baIdentityNumber))
    body.getContent.add(f.createBAreportBodyStructureBAreportNumber(baReportNumber.toString))
    body.getContent.add(f.createBAreportBodyStructureTypeOfTax(typeOfTax))
    body.getContent.add(f.createBAreportBodyStructureProposedEntries(proposedEntries))

    body.getContent.add(f.createBAreportBodyStructureIndicatedDateOfChange(indicatedDateOfChange))

    body.getContent.add(f.createBAreportBodyStructureRemarks("THIS IS A BLUEPRINT TEST PLEASE DELETE / NO ACTION THIS REPORT"))

    body
  }

  val currentTaxGen: Gen[CurrentTax] = for {
    currency <- Gen.oneOf(ISOcurrencyType.values())
    value <- Gen.chooseNum(1l, 10000000000l)
    band <- Gen.oneOf(BandType.values())
  }yield {
    val currentTax = new CurrentTax()
    currentTax.setCouncilTaxBand(band)

    val a = new RateableValue()
    a.setCurrency(currency)
    a.setValue(new java.math.BigDecimal(value))

    currentTax.setRateableValue(a)

    currentTax

  }

  val assessmentPropertiesGenerator: Gen[AssessmentProperties] = for {
    currentTax <- currentTaxGen
    propertyIdentity <- propertyIdentityGen
    occupierContact <- occupierContactGen
  }yield {
    val assesmentProperties =  new AssessmentProperties()
    assesmentProperties.setCurrentTax(currentTax)
    assesmentProperties.setPropertyIdentity(propertyIdentity)
    assesmentProperties.setOccupierContact(occupierContact)

    assesmentProperties
  }

  val proposedEntriesGen: Gen[BApropertySplitMergeStructure] = for {
    assesmentProperties <- assessmentPropertiesGenerator

  }yield {
    val propertySplitStructure = new BApropertySplitMergeStructure()
    propertySplitStructure.getAssessmentProperties.add(assesmentProperties)

    propertySplitStructure
  }

  val personNameStructureGen: Gen[PersonNameStructure] = for {
    personTitle <- Gen.oneOf(List("MR", "MRS", "MISS", "MASTER", "MRS", "MS", "MX", "Mistress", "The Most Honourable"))
    personGivenName <- Gen.alphaUpperStr
    personFamilyName <- Gen.alphaUpperStr
  }yield {
    val ret = new PersonNameStructure()
    ret.getPersonNameTitle.add(personTitle)
    ret.getPersonGivenName.add(personGivenName)
    ret.setPersonFamilyName(personFamilyName)
    ret.setPersonRequestedName(s"${personTitle} ${personGivenName} ${personFamilyName}")

    ret
  }

  val occupierContactGen: Gen[OccupierContactStructure] = for {
    occupierName <- personNameStructureGen
  }yield {
    val ret = new OccupierContactStructure()
    ret.setOccupierName(occupierName)

    ret
  }

  val textAddressGen: Gen[TextAddressStructure] = for {
    postcode <- Gen.alphaStr
    addressLile1 <- Gen.alphaLowerStr
    addressLile2 <- Gen.alphaUpperStr
  }yield {
    val textAddress = new TextAddressStructure()
    textAddress.setPostcode(postcode)
    textAddress.getAddressLine.add(addressLile1)
    textAddress.getAddressLine.add(addressLile2)
    textAddress
  }


  val propertyIdentityGen: Gen[BApropertyIdentificationStructure] = for {
    textAddress <- textAddressGen
    uniquePropertyReferenceNumber <- Gen.chooseNum(100l, 100000l)
    baReference <- Gen.chooseNum(100l, 100000l)
  }yield {
    val propertyIdentity = new BApropertyIdentificationStructure()

    propertyIdentity.getContent.add(objectFactory.createBSaddressStructureUniquePropertyReferenceNumber(uniquePropertyReferenceNumber))
    propertyIdentity.getContent.add(objectFactory.createBApropertyIdentificationStructureTextAddress(textAddress))
    propertyIdentity.getContent.add(objectFactory.createBApropertyIdentificationStructureBAreference(baReference.toString))

    propertyIdentity
  }








  val typeOfTaxGen: Gen[TypeOfTax] = for {
    reason <- Gen.oneOf(CtaxReasonForReportCodeContentType.values())
  }yield {
    val typeOfTax = new TypeOfTax

    val ctaxReasonForReport = new CtaxReasonForReport()

    val reasonReportCode = new CtaxReasonForReportCodeStructure()
    reasonReportCode.setValue(reason)
    ctaxReasonForReport.setReasonForReportCode(reasonReportCode)
    ctaxReasonForReport.setReasonForReportDescription("NEW")
    typeOfTax.setCtaxReasonForReport(ctaxReasonForReport)
    typeOfTax
  }


}
