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

package uk.gov.hmrc.voabar.models

import ebars.xml.BAreports
import play.api.libs.json.Json

/**
  * Created by rgallet on 16/02/16.
  *
  * Monad style baby!
  */

case class JobStatusErrorFromStub(code: String, value: String)

case class JobStatusErrorsFromStub(message: String, propertyReferenceNumbers: Option[Seq[Long]], errors: Seq[JobStatusErrorFromStub])

case class JobStatusErrors(transactionId: String, reportNumber: String, message: String, code: Option[String], propertyReferenceNumbers: Option[Seq[Long]])

case object JobStatusErrorFromStub {
  implicit val errorFormat = Json.format[JobStatusErrorFromStub]
  implicit val errorReads = Json.reads[JobStatusErrorFromStub]
}

case object JobStatusErrorsFromStub {
  implicit val errorsFormat = Json.format[JobStatusErrorsFromStub]
}

sealed trait ExceptionsAccumulator[A <: JobStatusErrorFromStub, B <: BAreports] {

  def get: JobStatusErrorsFromStub

  def map(f: B => Option[A]): ExceptionsAccumulator[A, B]

  def flatMap(f: B => ExceptionsAccumulator[A, B]): ExceptionsAccumulator[A, B]
}

case class EmptyReportValidation[A <: JobStatusErrorFromStub, B <: BAreports]() extends ExceptionsAccumulator[A, B] {

  override def get: JobStatusErrorsFromStub = JobStatusErrorsFromStub("EmptyReportValidation", None, Seq.empty[JobStatusErrorFromStub])

  override def flatMap(f: (B) => ExceptionsAccumulator[A, B]): ExceptionsAccumulator[A, B] = EmptyReportValidation()

  override def map(f: (B) => Option[A]): ExceptionsAccumulator[A, B] = EmptyReportValidation()
}

case class ReportValidation[A <: JobStatusErrorFromStub, B <: BAreports](errors: Seq[A], report: B) extends ExceptionsAccumulator[A, B] {

  import models.EbarsBAreports._

  override def get: JobStatusErrorsFromStub = JobStatusErrorsFromStub("ReportValidation", Some(report.uniquePropertyReferenceNumbers), errors)

  override def map(f: (B) => Option[A]): ExceptionsAccumulator[A, B] = {
    f(report) match {
      case Some(newErrors) => copy(errors = errors :+ newErrors)
      case _ => this
    }
  }

  override def flatMap(f: (B) => ExceptionsAccumulator[A, B]): ExceptionsAccumulator[A, B] = f(report)
}
