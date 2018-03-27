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
import uk.gov.hmrc.voabar.models.{BAPropertyReport, Error}

import scala.collection.mutable.ListBuffer
import scala.xml._

@Singleton
class BusinessRules @Inject()()(implicit request:Request[_]) {

  def reasonForReportErrors(baReport:BAPropertyReport):List[Error] = {
    (baReport.node \\ "TypeOfTax" \ "_").headOption match {
      case Some(node) => node.label match {
        case "CtaxReasonForReport" => validateCTaxCode(baReport.node)
        case _ => throw new RuntimeException(s"Unsupported tax type: ${node.label} ")
      }
      case None => throw new RuntimeException("Xml element not found: TypeOfTax")
    }
  }

  private def validateCTaxCode(implicit node:NodeSeq): List[Error] = {

    val repCode:String = (node \\ "ReasonForReportCode").text
    val reportNumber:String = (node \\ "BAreportNumber").text
    val lb = new ListBuffer[Error]

    repCode match {
      case "CR03" => // no existing- only 1 proposed
        if (proposedEntries != 1) lb += Error("1001", Seq(s"$reportNumber",s"$repCode"))
        if (existingEntries != 0) lb += Error("1002", Seq(s"$reportNumber",s"$repCode"))
      case "CR04" => // either 1 existing or 1 proposed
        if (((proposedEntries == 1) && (existingEntries ==0 )) ||
          ((proposedEntries == 0) && (existingEntries == 1))){}
        else
          lb += Error("1003",Seq(s"$reportNumber", s"$repCode"))
      case "CR05" => // at least 1 existing and at least 1 proposed
        if (proposedEntries == 0) lb += Error("1004", Seq(s"$reportNumber", s"$repCode"))
        if (existingEntries == 0) lb += Error("1005", Seq(s"$reportNumber", s"$repCode"))
      case "CR08" => lb += Error("1006", Seq(s"$reportNumber", s"$repCode"))
      case "CR11" => lb += Error("1006", Seq(s"$reportNumber", s"$repCode"))
      case "CR12" => // 1 existing and 1 proposed
        if (proposedEntries != 1) lb += Error("1001", Seq(s"$reportNumber", s"$repCode"))
        if (existingEntries != 1) lb += Error("1007", Seq(s"$reportNumber", s"$repCode"))
      case "CR13" => lb += Error("1006", Seq(s"$reportNumber", s"$repCode"))

      case _ => // default - 1 existing and none proposed
        if (proposedEntries != 0) lb += Error("1008", Seq(s"$reportNumber", s"$repCode"))
        if (existingEntries != 1) lb += Error("1007", Seq(s"$reportNumber", s"$repCode"))
    }
    lb.toList
  }

  private def proposedEntries(implicit node:NodeSeq):Int = (node \\ "ProposedEntries").size
  private def existingEntries(implicit node:NodeSeq):Int = (node \\ "ExistingEntries").size


  def baIdentityCodeErrors(xml:Node): List[Error] = {
    val lb = new ListBuffer[String]()
    val codeInReport:String = (xml \\ "BillingAuthorityIdentityCode").text
      if (codeInReport.isEmpty) lb += "1012BA Code missing from batch submission"
        request.headers.get("BA-Code") match {
          case Some(code) => if (codeInReport.length > 0 && code != codeInReport) lb +=
            "1010BA code in request header does not match with that in the batch report"
          case None => lb += "1011BA Code missing from request header"
        }
    lb.toList.map{s => Error(s.take(4),Seq("BAIdentityCode",s.drop(4)))}
  }


}
