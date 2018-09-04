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

package uk.gov.hmrc.voabar

import org.apache.commons.codec.binary.Base64
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.voabar.models.LoginDetails
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

@Singleton
class Utils {
  private lazy val crypto = ApplicationCrypto.JsonCrypto
  def decryptPassword(password: String) : String = crypto.decrypt(Crypted(password)).value


  def generateHeader(loginDetails: LoginDetails)(implicit ec: ExecutionContext): HeaderCarrier = {
    val decryptedPassword = decryptPassword(loginDetails.password)
    val encodedAuthHeader = Base64.encodeBase64String(s"${loginDetails.username}:${decryptedPassword}".getBytes("UTF-8"))
    HeaderCarrier(authorization = Some(Authorization(s"Basic $encodedAuthHeader")))
  }
}
