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

import org.scalatest.{AsyncWordSpec, AsyncWordSpecLike, MustMatchers, OptionValues}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{PlaySpec, WsScalaTestClient}
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Matchers.anyString
import org.mockito.Matchers.any
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.models.LoginDetails

import scala.util.Try

class ReportUploadServiceSpec extends AsyncWordSpec with MockitoSugar with  MustMatchers with OptionValues with WsScalaTestClient {

  "ReportUploadServiceSpec" must {
    "proces request " in {
      val reportUploadService = new ReportUploadService(aCorrectStatusRepository(), aValidationService(), aXmlParser(), aLegacyConnector())
      val res = reportUploadService.upload("username", "password", "<xml>ble</xml>", "reference1")
      res.map { result =>
        result mustBe "ok"
      }
    }
  }

  def aCorrectStatusRepository(): SubmissionStatusRepository = {
    val repository = mock[SubmissionStatusRepository]
    when(repository.updateStatus(anyString(), anyString())).thenReturn(Future.successful(Right(true)))
    when(repository.addError(anyString(), anyString())).thenReturn(Future.successful(Right(true)))
    repository
  }

  def aValidationService(): ValidationService = {
    val validationService = mock[ValidationService]
    when(validationService.validate(anyString())).thenReturn(Right(true))

    validationService
  }

  def aXmlParser(): XmlParser = {
    new XmlParser()
  }

  def aLegacyConnector(): LegacyConnector = {
    val connector = mock[LegacyConnector]
    when(connector.sendBAReport(any(classOf[BAReportRequest]))(any(classOf[ExecutionContext])))
      .thenReturn(Future.successful(Try(200)))

    when(connector.validate(any(classOf[LoginDetails]))(any(classOf[ExecutionContext])))
        .thenReturn(Future.successful(Try(200)))

    connector
  }

}
