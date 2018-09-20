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
import java.util
import java.util.GregorianCalendar

import ebars.xml.BAreportBodyStructure.TypeOfTax
import ebars.xml.BAreportBodyStructure.TypeOfTax.CtaxReasonForReport
import ebars.xml.{BApropertySplitMergeStructure, _}
import javax.xml.bind.JAXBElement
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import org.scalacheck.Gen

import collection.JavaConverters._
import org.scalacheck.util.Buildable._

object BaReportGenerator {

  lazy val baReportGenerator: Gen[BAreports] = for {
    header <- reportHeader
    propertyReports <- Gen.containerOf[List, BAreportBodyStructure](reportBodyStructure)
  }yield {
    val report = new BAreports()
    report.setBAreportHeader(header)
    report.getBApropertyReport.addAll(propertyReports.asJava)

    report
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
  }yield {
    val body = new BAreportBodyStructure()
    val f = new ebars.xml.ObjectFactory()

    body.getContent.add(f.createBAreportBodyStructureDateSent(dateSend))
    body.getContent.add(f.createBAreportBodyStructureTransactionIdentityBA(transactionIdentity.toString))
    body.getContent.add(f.createBAreportBodyStructureBAidentityNumber(baIdentityNumber))
    body.getContent.add(f.createBAreportBodyStructureBAreportNumber(baReportNumber.toString))
    body.getContent.add(f.createBAreportBodyStructureTypeOfTax(typeOfTax))


    body.getContent.add(f.createBAreportBodyStructureProposedEntries(proposedEntries)) //TODO maybe wrong position, we need to create something sooner


    body
  }

  val proposedEntriesGen: Gen[BApropertySplitMergeStructure] = for {
    _ <- Gen.alphaChar
    //TODO generate properly
  }yield {
    val a = new BApropertySplitMergeStructure()


    a
  }




  val typeOfTaxGen: Gen[TypeOfTax] = for {
    reason <- Gen.oneOf(CtaxReasonForReportCodeContentType.values())
  }yield {
    val typeOfTax = new TypeOfTax

    val ctaxReasonForReport = new CtaxReasonForReport()

    val reasonReportCode = new CtaxReasonForReportCodeStructure()
    reasonReportCode.setValue(reason)
    reasonReportCode.setDescription("NEW")
    ctaxReasonForReport.setReasonForReportCode(reasonReportCode)
    typeOfTax.setCtaxReasonForReport(ctaxReasonForReport)
    typeOfTax
  }



  val xmlGregorianCalendar = for {
    calendar <- Gen.calendar
  }yield {
    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar.asInstanceOf[GregorianCalendar])
  }

}
