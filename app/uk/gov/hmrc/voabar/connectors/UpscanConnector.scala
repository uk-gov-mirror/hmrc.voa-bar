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

package uk.gov.hmrc.voabar.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voabar.models.{BarError, UnknownError}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class UpscanConnector @Inject() (httpClient: WSClient)(implicit ec: ExecutionContext) {

  val logger = Logger(this.getClass)

  def downloadReport(url: String)(implicit hc: HeaderCarrier): Future[Either[BarError, Array[Byte]]] = {

    httpClient.url(url)
      .withHttpHeaders(hc.headers: _*)
      .get().map { wsResponse =>
      Right(wsResponse.body[Array[Byte]])
    }.recover {
      case e: Exception => {
        logger.warn("Unable to download file from upscan", e)
        Left(UnknownError("Unable to download file, please try later"))
      }
    }
  }
}
