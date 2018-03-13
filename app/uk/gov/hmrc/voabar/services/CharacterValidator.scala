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

import play.api.Logger
import uk.gov.hmrc.voabar.models.{Error, _}

import scala.util.matching.Regex
import scala.xml.{Node, NodeSeq}

class CharacterValidator {

  val validCharacterRegex = """(['A-Z0-9\s\-&+\.@\(\):\/])+""".r

  def elementNodes(nodes: NodeSeq) = nodes.headOption match {
    case Some(n: Node) => n.descendant.collect {
      case n@Node(_, _, child@_*) if child.size == 1 && child.head.isAtom => n
    }
    case None => {
      Logger.warn("The parsed XML reached Character Validation with an empty NodeSeq")
      throw new RuntimeException("The parsed XML reached Character Validation with an empty NodeSeq")
    }
  }

  def validateHeader(header: BatchHeader): Seq[Error] = {
    val location = "Header"
    val elements = elementNodes(header.node)

    val errors = elements.collect {
      case e: Node if (validateString(e.text) == false) => Error("1000", Seq(location, e.label, e.text))
    }
    Seq(errors: _*)
  }

  def validateTrailer(trailer: BatchTrailer): Seq[Error] = {
    val location = "Trailer"
    val elements = elementNodes(trailer.node)

    val errors = elements.collect {
      case e: Node if (validateString(e.text) == false) => Error("1000", Seq(location, e.label, e.text))
    }
    Seq(errors: _*)
  }

  def validateBAPropertyReports(reports: Seq[BAPropertyReport]): (Seq[BAPropertyReport], Seq[Error]) = {
    val result = reports.map(propertyReport => validatePropertyReport(propertyReport))

    val errors: Seq[Error] = result.collect { case Left(l) => l }.flatten
    val remainingReports: Seq[BAPropertyReport] = result.collect { case Right(l) => l }

    (remainingReports, errors)
  }

  def validatePropertyReport(bAPropertyReport: BAPropertyReport): Either[Seq[Error], BAPropertyReport] = {
    val reportNumber = getPropertyReportNumber(bAPropertyReport)
    val elements = elementNodes(bAPropertyReport.node)

    val errors = elements.collect {
      case e: Node if (validateString(e.text) == false) => Error("1000", Seq(reportNumber, e.label, e.text))
    }

    if (errors.isEmpty) Right(bAPropertyReport) else Left(errors)
  }

  def getPropertyReportNumber(bAPropertyReport: BAPropertyReport): String = (bAPropertyReport.node \ "BAreportNumber").text

  def validateString(input: String): Boolean = {
    val result = validCharacterRegex.findAllIn(input).toList
    val resultLength = result.size

    resultLength match {
      case length if (length > 1 || length == 0) => false
      case length if (length == 1 && result.mkString.size != input.size) => false
      case _ => true
    }
  }

  def charactersValidationStatus(batch: BatchSubmission) = {
    val headerErrors: Seq[Error] = validateHeader(batch.batchHeader)
    val trailerErrors: Seq[Error] = validateTrailer(batch.batchTrailer)
    val reportsResult = validateBAPropertyReports(batch.baPropertyReports)

    val remainingReports = reportsResult._1
    val reportsErrors = reportsResult._2

    val allErrors: Seq[Error] = (headerErrors :: trailerErrors :: reportsErrors :: Nil).flatten

    ValidationResult(remainingReports, allErrors)
  }
}
