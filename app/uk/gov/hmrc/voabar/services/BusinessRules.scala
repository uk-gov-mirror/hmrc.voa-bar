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
import uk.gov.hmrc.voabar.models.BAPropertyReport

import scala.collection.mutable.ListBuffer
import scala.xml._

class BusinessRules {

  def reasonForReportErrors(baReport:BAPropertyReport):List[String] = {
    (baReport.node \\ "TypeOfTax" \ "_").headOption match {
      case Some(node) => node.label match {
        case "CtaxReasonForReport" => validateCTaxCode(baReport.node)
        case _ => throw new RuntimeException(s"Unsupported tax type: ${node.label} ")
      }
      case None => throw new RuntimeException("Xml element not found: TypeOfTax")
    }
  }

  private def validateCTaxCode(implicit node:NodeSeq): List[String] = {

    val repCode:String = (node \\ "ReasonForReportCode").text
    val lb = new ListBuffer[String]

    repCode match {
      case "CR03" => // (New) must be no existing- only 1 proposed
        if (proposedEntries != 1) lb += "There must be one proposed entry for reason code CR03"
        if (existingEntries != 0) lb += "There must be no existing entries for reason code CR03"
      case "CR04" => // (Change to Domestic Use) must be either 1 existing or 1 proposed
        if (((proposedEntries == 1) && (existingEntries ==0 )) ||
          ((proposedEntries == 0) && (existingEntries == 1))){}
        else
          lb += "There must be either one existing entry or one proposed entry for reason code CR04"
      case "CR05" => // (Reconstituted Property) must be at least 1 existing and at least 1 proposed
        if (proposedEntries == 0) lb += "There must be at least one proposed entry for reason code CR05"
        if (existingEntries == 0) lb += "There must be at least one existing entry for reason code CR05"
      case "CR08" => lb += s"report code: ${repCode} NOT IN USE"
      case "CR11" => lb += s"report code: ${repCode} NOT IN USE"
      case "CR12" => // must be 1 existing and 1 proposed
        if (proposedEntries != 1) lb += "There must be one proposed entry for reason code CR12"
        if (existingEntries != 1) lb += "There must be one existing entry for reason code CR12"
      case "CR13" => lb += s"report code: ${repCode} NOT IN USE"

      case _ => // default - must be 1 existing and none proposed
        if (proposedEntries != 0) lb += s"There must be no proposed entries for reason code ${repCode}"
        if (existingEntries != 1) lb += s"There must be one existing entry for reason code ${repCode}"
    }
    lb.toList
  }

  private def proposedEntries(implicit node:NodeSeq):Int = (node \\ "ProposedEntries").size
  private def existingEntries(implicit node:NodeSeq):Int = (node \\ "ExistingEntries").size



}
