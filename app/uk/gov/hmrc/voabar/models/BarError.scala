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

import reactivemongo.api.commands.UpdateWriteResult

sealed trait BarError


case class BarXmlError(message: String) extends BarError

case class BarXmlValidationError(errors: List[Error]) extends BarError

case class BarValidationError(errors: List[Error]) extends BarError

case class BarSubmissionValidationError(errors: List[ReportError]) extends BarError

case class BarMongoError(error: String, updateWriteResult: Option[UpdateWriteResult] = None) extends BarError

case class BarEbarError(ebarError: String) extends BarError

case class BarEmailError(ebarError: String) extends BarError

case class UnknownError(detail: String) extends BarError
