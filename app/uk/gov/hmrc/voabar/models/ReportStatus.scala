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

package uk.gov.hmrc.voabar.models

import java.time.ZonedDateTime

import play.api.libs.json._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

sealed trait ReportStatusType {
  val value: String = getClass.asSubclass(getClass).getSimpleName.replace("$", "")
}

case object Pending extends ReportStatusType
case object Verified extends ReportStatusType
case object Failed extends ReportStatusType
case object Unknown extends ReportStatusType
case object Submitted extends ReportStatusType
case object Cancelled extends ReportStatusType
case object Done extends ReportStatusType

final case class ReportStatus(
                               id: String,
                               created: ZonedDateTime,
                               url: Option[String] = None,
                               checksum: Option[String] = None,
                               errors: Option[Seq[Error]] = Some(Seq()),
                               baCode: Option[String] = None,
                               status: Option[String] = Some(Pending.value),
                               filename: Option[String] = None
                             )

object ReportStatus {
  import ReactiveMongoFormats.mongoEntity

  implicit val format =  mongoEntity {

    implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  Json.format[ReportStatus]
  }
}
