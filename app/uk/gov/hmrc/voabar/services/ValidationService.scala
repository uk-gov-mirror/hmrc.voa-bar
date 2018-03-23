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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.voabar.models.Error

import scala.xml.Node

@Singleton
class ValidationService @Inject()(
                                   xmlValidator: XmlValidator,
                                   xmlParser:XmlParser,
                                   charValidator:CharacterValidator) {

  def validate(xml:Node, baCode:String):List[Error] = {
    val billingAuthCode:String = (xml \\ "BillingAuthorityIdentityCode").text
    val baCodeResult:List[Error] = validationBACode(billingAuthCode,baCode)
    val parsedBatch:Seq[Node] = xmlParser.oneReportPerBatch(xml)
    val schemaErrors:List[Error] = parsedBatch.flatMap(b => validationSchema(b)).toList.distinct
    val charErrors:List[Error] = validationChars(xml).toList
    baCodeResult ::: schemaErrors ::: charErrors
  }

  private def validationBACode(baCodeInReport:String, baCodeInHeader:String): List[Error] = {
    if (baCodeInReport != baCodeInHeader) Error("1010",Seq()) :: List[Error]()
    else Nil
  }

  private def validationSchema(xml:Node):Seq[Error] = {
    xmlValidator.validate(xml.toString())
  }

  private def validationChars(xml:Node):Seq[Error] = {
    val header:Node = (xml \ "BAreportHeader").head
    val trailer:Node = (xml \ "BAreportTrailer").head
    val reports:Seq[Node] = xml \ "BApropertyReport"
    val headerErrors:List[Error] = charValidator.validateChars(header,"Header")
    val trailerErrors:List[Error] = charValidator.validateChars(trailer,"Trailer")
    val reportErrors:List[Error] = reports.flatMap(r => charValidator.validateChars(r,(r \ "BAreportNumber").text)).toList
    headerErrors ::: reportErrors ::: trailerErrors
  }

  private def validationBusinessRules(xml:Node):Seq[Error] = {
    ???
  }



}
