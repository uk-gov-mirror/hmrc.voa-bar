/*
 * Copyright 2020 HM Revenue & Customs
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

import java.net.URL

import javax.inject.{Inject, Singleton}
import org.w3c.dom.Document
import play.api.Logger
import uk.gov.hmrc.voabar.models.{BarError, BarValidationError, Error, UnknownError}

import scala.util.{Failure, Success, Try}
import scala.xml.Node

@Singleton
class ValidationService @Inject()(xmlValidator: XmlValidator,
                                  xmlParser:XmlParser,
                                  businessRules:BusinessRules
                                 ) {

  def validate(xml: Array[Byte], baLogin: String): Either[BarError, (Document, Node)] = {
    for {
      domTree <- xmlParser.parse(xml).right
      _ <- xmlValidator.validate(domTree).right //validate against XML schema

      scalaElement <- xmlParser.domToScala(domTree).right
      _ <- businessValidation(scalaElement, baLogin).right
    } yield {
      (domTree, scalaElement)
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

    val validations:List[Node => List[Error]] = List(
      validationBACode(baLogin),
      validationBusinessRules(baLogin)
    )
    parsedBatch.toList.flatMap{n => validations.flatMap(_.apply(n))}.distinct

  }

  private def validationBACode(baLogin: String)(xml:Node): List[Error] = {
    businessRules.baIdentityCodeErrors(xml, baLogin)
  }

  private def validationBusinessRules(baLogin: String)(xml:Node):List[Error] = {
    val reports:Seq[Node] = xml \ "BApropertyReport"
    reports.flatMap(r => {
      businessRules.reasonForReportErrors(r) ++ businessRules.bAidentityNumber(r, baLogin)
    }).toList
  }
}
