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
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.voabar.models.Error
import uk.gov.hmrc.voabar.util._

import scala.collection.mutable.ListBuffer
import scala.xml._

@Singleton
class BusinessRules @Inject()()(implicit request:Request[_]) {

  def reasonForReportErrors(baReport:NodeSeq):List[Error] = {
    (baReport \\ "TypeOfTax" \ "_").headOption match {
      case Some(node) => node.label match {
        case "CtaxReasonForReport" => validateCTaxCode(baReport)
        case _ =>
          Logger.warn(s"Unsupported tax type: ${node.label}")
          throw new RuntimeException(s"Unsupported tax type: ${node.label} ")
      }
      case None =>
        Logger.warn("Xml element not found: TypeOfTax")
        throw new RuntimeException("Xml element not found: TypeOfTax")
    }
  }

  private def validateCTaxCode(implicit node:NodeSeq): List[Error] = {

    val repCode:String = (node \\ "ReasonForReportCode").text
    val reportNumber:String = (node \\ "BAreportNumber").text
    val lb = new ListBuffer[Error]

    repCode match {
      case "CR03" => // no existing- only 1 proposed
        if (proposedEntries != 1) lb += Error(ErrorCodes.ONE_PROPOSED, Seq(s"$reportNumber",s"$repCode"))
        if (existingEntries != 0) lb += Error(ErrorCodes.NONE_EXISTING, Seq(s"$reportNumber",s"$repCode"))
      case "CR04" => // either 1 existing or 1 proposed
        if (((proposedEntries == 1) && (existingEntries ==0 )) ||
          ((proposedEntries == 0) && (existingEntries == 1))){}
        else
          lb += Error("1003",Seq(s"$reportNumber", s"$repCode"))
      case "CR05" => // at least 1 existing and at least 1 proposed
        if (proposedEntries == 0) lb += Error(ErrorCodes.ATLEAST_ONE_PROPOSED, Seq(s"$reportNumber", s"$repCode"))
        if (existingEntries == 0) lb += Error(ErrorCodes.ATLEAST_ONE_EXISTING, Seq(s"$reportNumber", s"$repCode"))
      case "CR08" => lb += Error(ErrorCodes.NOT_IN_USE, Seq(s"$reportNumber", s"$repCode"))
      case "CR11" => lb += Error(ErrorCodes.NOT_IN_USE, Seq(s"$reportNumber", s"$repCode"))
      case "CR12" => // 1 existing and 1 proposed
        if (proposedEntries != 1) lb += Error(ErrorCodes.ONE_PROPOSED, Seq(s"$reportNumber", s"$repCode"))
        if (existingEntries != 1) lb += Error(ErrorCodes.ONE_EXISTING, Seq(s"$reportNumber", s"$repCode"))
      case "CR13" => lb += Error(ErrorCodes.NOT_IN_USE, Seq(s"$reportNumber", s"$repCode"))

      case _ => // default - 1 existing and none proposed
        if (proposedEntries != 0) lb += Error(ErrorCodes.NONE_PROPOSED, Seq(s"$reportNumber", s"$repCode"))
        if (existingEntries != 1) lb += Error(ErrorCodes.ONE_EXISTING, Seq(s"$reportNumber", s"$repCode"))
    }
    lb.toList
  }

  private def proposedEntries(implicit node:NodeSeq):Int = (node \\ "ProposedEntries").size
  private def existingEntries(implicit node:NodeSeq):Int = (node \\ "ExistingEntries").size


  def baIdentityCodeErrors(xml:Node): List[Error] = {
    val lb = new ListBuffer[String]()
    val codeInReport:String = (xml \\ "BillingAuthorityIdentityCode").text
      if (codeInReport.isEmpty) lb += ErrorCodes.BA_CODE_REPORT
        request.headers.get("BA-Code") match {
          case Some(code) => if (codeInReport.length > 0 && code != codeInReport) lb +=
            ErrorCodes.BA_CODE_MATCH
          case None => lb += ErrorCodes.BA_CODE_REQHDR
        }
    lb.toList.map{err => Error(err)}
  }
}
