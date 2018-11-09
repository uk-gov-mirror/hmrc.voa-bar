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

import java.io.FileInputStream

import org.apache.commons.io.IOUtils
import org.scalatest.{AsyncWordSpec, MustMatchers, OptionValues}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.WsScalaTestClient
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Matchers.anyString
import org.mockito.Matchers.any
import org.mockito.Matchers.{eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import uk.gov.hmrc.voabar.models._
import uk.gov.hmrc.voabar.util.{ATLEAST_ONE_PROPOSED, CHARACTER, INVALID_XML}

import scala.util.Try

class ReportUploadServiceSpec extends AsyncWordSpec with MockitoSugar with  MustMatchers with OptionValues with WsScalaTestClient {

  val uploadReference = "submissionID"

  val aXml = IOUtils.toString(new FileInputStream("test/resources/xml/CTValid1.xml"))

  "ReportUploadServiceSpec" must {
    "proces request " in {
      val reportUploadService = new ReportUploadService(aCorrectStatusRepository(), aValidationService(), aXmlParser(), aLegacyConnector())
      val res = reportUploadService.upload("username", "password", aXml, uploadReference)
      res.map { result =>
        result mustBe "ok"
      }
    }

    "record error for not valid XML" in {
      val statusRepository = aCorrectStatusRepository()

      val reportUploadService = new ReportUploadService(statusRepository, aValidationService(), aXmlParser(), aLegacyConnector())
      val res = reportUploadService.upload("username", "password", "<xm>ble</xml>", uploadReference)
      res.map { result =>
        verify(statusRepository).updateStatus(meq(uploadReference), meq(Failed))
        result mustBe "failed"
      }
    }

    "stop any work after update error" in {
      val statusRepository = mock[SubmissionStatusRepository]
      when(statusRepository.updateStatus(anyString(), any(classOf[ReportStatusType])))
        .thenReturn(Future.successful(Left(BarMongoError("mongo is broken", None))))
      val validationService = aValidationService()
      val legacyConnector = aLegacyConnector()
      val xmlParser = mock[XmlParser]

      val reportUploadService = new ReportUploadService(statusRepository, validationService, xmlParser, legacyConnector)
      val res = reportUploadService.upload("username", "password", "<xm>ble</xml>", uploadReference)
      res.map { result =>
        verify(statusRepository, times(1)).updateStatus(meq(uploadReference), meq(Pending))
        verifyZeroInteractions(validationService, legacyConnector, xmlParser)
        result mustBe "failed"
      }
    }

    "handle full XML" in {
      val baReport = IOUtils.toString(new FileInputStream("test/resources/xml/CTValid2.xml"))
      val reportUploadService = new ReportUploadService(aCorrectStatusRepository(), aValidationService(), aXmlParser(), aLegacyConnector())
      val res = reportUploadService.upload("username", "password", baReport, uploadReference)
      res.map { result =>
        result mustBe "ok"
      }
    }


    "Send submissions in a Single submission file" in {
      val baReport = IOUtils.toString(new FileInputStream("test/resources/xml/CTValid2.xml"))
      val legacyConnector = aLegacyConnector()

      val reportUploadService = new ReportUploadService(aCorrectStatusRepository(), aValidationService(), aXmlParser(), legacyConnector)
      val res = reportUploadService.upload("username", "password", baReport, uploadReference)

      res.map { result =>
        result mustBe "ok"

        val invocations = Mockito.mockingDetails(legacyConnector).getInvocations

        invocations must have size 1

      }
    }
  }


  "Error handler" should {

    "persis BarXmlError" in {

      val validationService = mock[ValidationService]
      when(validationService.validate(anyString(), anyString())).thenReturn(Left(BarXmlError("validation error")))
      val statusRepository = aCorrectStatusRepository()
      val reportUploadService = new ReportUploadService(statusRepository, validationService, aXmlParser(), aLegacyConnector())
      val resutl = reportUploadService.upload("u", "p", "<xml></xml>", "reference1")

      resutl.map { value =>
        value mustBe("failed")
        verify(statusRepository, times(1)).addError("reference1", Error(INVALID_XML, Seq("validation error")))
        true mustBe(true)
      }
    }


    "persis all Error from BarXmlValidationError" in {

      val errors = List (
        Error(CHARACTER),
        Error(ATLEAST_ONE_PROPOSED)
      )
      val xmlValidationError = BarXmlValidationError(errors)

      val validationService = mock[ValidationService]
      when(validationService.validate(anyString(), anyString())).thenReturn(Left(xmlValidationError))
      val statusRepository = aCorrectStatusRepository()
      val reportUploadService = new ReportUploadService(statusRepository, validationService, aXmlParser(), aLegacyConnector())
      val resutl = reportUploadService.upload("u", "p", "<xml></xml>", "reference1")


      resutl.map { value =>
        value mustBe("failed")
        verify(statusRepository, times(1)).addError("reference1", Error(CHARACTER, Seq()))
        verify(statusRepository, times(1)).addError("reference1", Error(ATLEAST_ONE_PROPOSED, Seq()))
        true mustBe(true)
      }

    }



  }

  def aCorrectStatusRepository(): SubmissionStatusRepository = {
    val repository = mock[SubmissionStatusRepository]
    when(repository.updateStatus(anyString(), any(classOf[ReportStatusType]))).thenAnswer(new Answer[Future[Either[BarError, Boolean]]] {
      override def answer(invocationOnMock: InvocationOnMock): Future[Either[BarError, Boolean]] = Future.successful(Right(true))
    })

    when(repository.update(anyString(), any(classOf[ReportStatusType]), any[Int])).thenAnswer(new Answer[Future[Either[BarError, Boolean]]] {
      override def answer(invocationOnMock: InvocationOnMock): Future[Either[BarError, Boolean]] = Future.successful(Right(true))
    })

    when(repository.addError(anyString(), any(classOf[Error]))).thenAnswer(new Answer[Future[Either[BarError, Boolean]]] {
      override def answer(invocationOnMock: InvocationOnMock): Future[Either[BarError, Boolean]] = Future.successful(Right(true))
    })
    repository
  }

  def aValidationService(): ValidationService = {
    val validationService = mock[ValidationService]
    when(validationService.validate(anyString(), anyString())).thenReturn(Right(true))

    validationService
  }

  def aXmlParser(): XmlParser = {
    new XmlParser()
  }

  def aLegacyConnector(): LegacyConnector = {
    val connector = mock[LegacyConnector]
    when(connector.sendBAReport(any(classOf[BAReportRequest]))(any(classOf[ExecutionContext])))
      .thenAnswer(new Answer[Future[Try[Int]]] {
        override def answer(invocation: InvocationOnMock): Future[Try[Int]] = Future.successful(Try(200))
      })

    when(connector.validate(any(classOf[LoginDetails]))(any(classOf[ExecutionContext])))
      .thenAnswer(new Answer[Future[Try[Int]]] {
        override def answer(invocation: InvocationOnMock): Future[Try[Int]] = Future.successful(Try(200))
      })

    connector
  }

}
