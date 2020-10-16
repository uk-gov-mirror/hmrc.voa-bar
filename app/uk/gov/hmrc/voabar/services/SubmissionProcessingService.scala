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

import java.io.ByteArrayInputStream
import java.net.URL

import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.StreamSource
import org.apache.commons.io.IOUtils
import play.api.Logger
import services.EbarsValidator
import uk.gov.hmrc.voabar.models.BarError

import scala.util.Try
import collection.JavaConverters._

@Singleton
class SubmissionProcessingService @Inject() (validationService: ValidationService) {

  val log = Logger(getClass)

  val correctionEngine = new RulesCorrectionEngine

  val xmlValidator = new EbarsValidator

  def processAsV1(url: String, baLogin: String, requestId: String, error: BarError): Boolean = {
    log.info(s"Unable to process V2 upload with V2 validation. ${baLogin}, ${requestId}, error: ${error}")
    val xml = IOUtils.toByteArray(new URL(url))
    processAsV1(xml, baLogin, requestId)
  }

  def processAsV1(xml: Array[Byte], baLogin: String, requestId: String): Boolean = {

    Try {
      val source = new StreamSource(CorrectionInputStream(new ByteArrayInputStream(xml)))

      val submission = xmlValidator.fromXml(source)

      val allReports = xmlValidator.split(submission)

      allReports.foreach { report =>
        correctionEngine.applyRules(report)
      }

      submission.getBApropertyReport.clear()
      submission.getBApropertyReport.addAll(allReports.map(_.getBApropertyReport.get(0)).asJava)

      FixHeader(submission)
      FixCTaxTrailer(submission)

      val correctedXml = xmlValidator.toXml(submission).getBytes("UTF-8")

      validateAsV2(correctedXml, baLogin, requestId)

    }.recover {
      case e: Exception => {
        log.warn("Unable to process XML", e)
        false
      }
    }.getOrElse(false)

  }

  def validateAsV2(correctedXml: Array[Byte], baLogin: String, requestId: String): Boolean = {
    validationService.validate(correctedXml, baLogin) match {
      case Left(errors) => {
        log.info(s"Validation of fixed XML, baLogin: ${baLogin}, requestId: ${requestId}, errors: ${errors}")
        false
      }
      case Right((document, node)) => {
        log.info(s"Validation of fixed XML successful, baLogin: ${baLogin}, requestId: ${requestId}")
        true
      }
    }
  }
}
