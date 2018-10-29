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
import uk.gov.hmrc.voabar.models.{BarError, BarValidationError, Error}

import scala.xml.Node

@Singleton
class ValidationService @Inject()(xmlValidator: XmlValidator,
                                  xmlParser:XmlParser,
                                  charValidator:CharacterValidator,
                                  businessRules:BusinessRules
                                 ) {

  def validate(xml: String, baLogin: String): Either[BarError, Boolean] = {

    for {
      _ <- xmlValidator.validate(xml).right //validate against XML schema
      scalaElement <- xmlParser.xmlToNode(xml).right
      _ <- businessValidation(scalaElement, baLogin).right
    }yield {
      true
    }
  }

  private def businessValidation(xml:Node, baLogin: String):Either[BarError, Boolean] = {
    val errors = xmlNodeValidation(xml, baLogin)

    if(errors.isEmpty) {
      Right(true)
    }else {
      Left(BarValidationError(errors))
    }

  }

  def xmlNodeValidation(xml:Node, baLogin: String): List[Error] = {

    val parsedBatch:Seq[Node] = xmlParser.oneReportPerBatch(xml)

    val validations:List[(Node) => List[Error]] = List(
      validationBACode(baLogin),
      validationChars,
      validationBusinessRules
    )
    parsedBatch.toList.flatMap{n => validations.flatMap(_.apply(n))}.distinct

  }

  private def validationBACode(baLogin: String)(xml:Node): List[Error] = {
    businessRules.baIdentityCodeErrors(xml, baLogin)
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
    val reports:Seq[Node] = xml \ "BApropertyReport"
    reports.flatMap(r => businessRules.reasonForReportErrors(r)).toList
  }
}
