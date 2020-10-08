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

import java.time.LocalDate

import play.api.libs.json.Json

object Address { implicit val format = Json.format[Address] }
case class Address(line1: String, line2: String, line3: Option[String], line4: Option[String], postcode: String)
object ContactDetails {implicit val format = Json.format[ContactDetails] }
case class ContactDetails(firstName: String, lastName: String, email: Option[String], phoneNumber: Option[String])
object Cr01Cr03Submission { val format = Json.format[Cr01Cr03Submission] }
case class Cr01Cr03Submission(reasonReport: Option[ReasonReportType], removalReason: Option[RemovalReasonType], otherReason: Option[String],
                              baReport: String, baRef: String, uprn: Option[String], address: Address,
                              propertyContactDetails: ContactDetails,
                              sameContactAddress: Boolean, contactAddress: Option[Address],
                              effectiveDate: LocalDate, havePlaningReference: Boolean,
                              planningRef: Option[String], noPlanningReference: Option[NoPlanningReferenceType], comments: Option[String])
