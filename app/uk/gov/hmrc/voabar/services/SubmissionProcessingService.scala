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

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL
import java.nio.file.{Files, Path, Paths}

import javax.inject.{Inject, Singleton}
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
    val xml = IOUtils.toString(new URL(url), "UTF-8")
    processAsV1(xml, baLogin, requestId)
  }

  def processAsV1(xml: String, baLogin: String, requestId: String): Boolean = {

    Try {
      val submission = xmlValidator.fromXml(xml)

      val allReports = xmlValidator.split(submission)

      allReports.foreach { report =>
        correctionEngine.applyRules(report)
      }

      submission.getBApropertyReport.clear()
      submission.getBApropertyReport.addAll(allReports.map(_.getBApropertyReport.get(0)).asJava)

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
    import cats.effect._

    def acquire = IO {
      Files.createTempFile("SubmissionProcessingService", "xml")
    }

    def release(path: Path) = IO {
      Files.deleteIfExists(path)
      ()
    }.handleErrorWith { e =>
      log.warn(s"Unable to delete file :${path}", e)
      IO.unit
    }

    def run(tmpFile: Path) = IO {
      Files.write(tmpFile, correctedXml)
      validationService.validate(tmpFile.toUri.toURL.toString, baLogin) match {
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

    Resource.make(acquire)(release).use(run).unsafeRunSync()
  }

}
