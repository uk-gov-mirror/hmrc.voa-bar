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

import uk.gov.hmrc.voabar.models._
import uk.gov.hmrc.voabar.models.errors.CharacterError

import scala.xml.{Node, NodeSeq}

class CharacterValidator {

  val validCharacterRegex = """^["'A-Z0-9\s\-&+\.@\(\):\/]+$"""

  def elementNodes(nodes: NodeSeq) = nodes.head.descendant.collect {
    case n@Node(_, _, child@_*) if child.size == 1 && child.head.isAtom => n
  }

  def validateHeader(header: BatchHeader): List[CharacterError] = {
    val reportNumber = "Header"
    val elements = elementNodes(header.node)

    elements.flatMap(e => validateString(e.text, e.label, reportNumber))
  }

  def validateTrailer(trailer: BatchTrailer): List[CharacterError] = {
    val reportNumber = "Trailer"
    val elements = elementNodes(trailer.node)

    elements.flatMap(e => validateString(e.text, e.label, reportNumber))
  }

  def validateBAPropertyReports(reports: List[BAPropertyReport]): List[CharacterError] = {
    reports.flatMap(propertyReport => validatePropertyReport(propertyReport))
  }

  def validatePropertyReport(bAPropertyReport: BAPropertyReport): List[CharacterError] = {
    val reportNumber = getPropertyReportNumber(bAPropertyReport)
    val elements = elementNodes(bAPropertyReport.node)

    elements.flatMap(e => validateString(e.text, e.label, reportNumber))
  }

  def getPropertyReportNumber(bAPropertyReport: BAPropertyReport): String = (bAPropertyReport.node \ "BAreportNumber").text

  def validateString(input: String, label: String, reportNumber: String): List[CharacterError] = {
    val strings = input.split("")
    val result = strings.collect {
      case e if (validateCharacter(e) == false) => CharacterError(reportNumber, label, 1000, e)
    }
    List(result: _*)
  }

  def validateCharacter(character: String): Boolean = character.matches(validCharacterRegex)

    def charactersValidationStatus(batch: BatchSubmission) = {
      val headerErrors: List[CharacterError] = validateHeader(batch.batchHeader)
      val trailerErrors: List[CharacterError] = validateTrailer(batch.batchTrailer)
      val reportsErrors: List[CharacterError] = validateBAPropertyReports(batch.baPropertyReports)

      val remainingReports = if(reportsErrors.isEmpty) batch.baPropertyReports
      else {
        val invalidReportNumbers = reportsErrors.map(e => e.reportNumber).distinct
        batch.baPropertyReports.filter(p => invalidReportNumbers.contains((p.node \ "BAreportNumber").text) == false)
      }

      val allErrors: List[CharacterError] = (headerErrors :: trailerErrors :: reportsErrors :: Nil).flatten

      CharacterValidationResult(remainingReports, allErrors)
    }
}
