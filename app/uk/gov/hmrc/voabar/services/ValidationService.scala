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
import play.api.mvc.Request
import uk.gov.hmrc.voabar.models.Error

import scala.xml.Node

@Singleton
class ValidationService @Inject()(xmlValidator: XmlValidator,
                                  xmlParser:XmlParser,
                                  charValidator:CharacterValidator,
                                  businessRules:BusinessRules) {
//
//  def validate(xml:Node, baCode:String):List[Error] = {
//    val billingAuthCode:String = (xml \\ "BillingAuthorityIdentityCode").text
//    val baCodeResult:List[Error] = validationBACode(billingAuthCode,baCode)
//    val parsedBatch:Seq[Node] = xmlParser.oneReportPerBatch(xml)
//    val schemaErrors:List[Error] = parsedBatch.flatMap(b => validationSchema(b)).toList.distinct
//    val charErrors:List[Error] = validationChars(xml)
//    baCodeResult ::: schemaErrors ::: charErrors
//  }


  def validate(xml:Node):List[Error] = {
    val f:List[(Node) => List[Error]] = List(
      validationBACode,
      validationSchema,
      validationChars
    )
    f.flatMap(_.apply(xml))
  }


  // no replace with call to BR svc
  private def validationBACode(xml:Node): List[Error] = {

    businessRules.baIdentityCodeErrors(xml)
  }


  private def validationSchema(xml:Node):List[Error] = {
    xmlValidator.validate(xml.toString()).toList
  }

  private def validationChars(xml:Node):List[Error] = {
    val header:Node = (xml \ "BAreportHeader").head
    val trailer:Node = (xml \ "BAreportTrailer").head
    val reports:Seq[Node] = xml \ "BApropertyReport"
    val headerErrors:List[Error] = charValidator.validateChars(header,"Header")
    val trailerErrors:List[Error] = charValidator.validateChars(trailer,"Trailer")
    val reportErrors:List[Error] = reports.flatMap(r => charValidator.validateChars(r,(r \ "BAreportNumber").text)).toList
    headerErrors ::: reportErrors ::: trailerErrors
  }

  private def validationBusinessRules(xml:Node):List[Error] = {
    ???

  }

// use?
//  def validate(node:Node, f:List[((Node) => List[Error])]): List[Error] = {
//    def reduce(errors:List[Error], xs:List[(Node) => List[Error]]): List[Error] = xs match {
//      case Nil => errors
//      case hd :: tl => reduce(hd(node) ::: errors,tl)
//    }
//    reduce(List[Error](),f)
//  }

}
