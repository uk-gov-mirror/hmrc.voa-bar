package uk.gov.hmrc.voabar.services

import org.scalatest.{AsyncWordSpec, AsyncWordSpecLike, MustMatchers, OptionValues}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{PlaySpec, WsScalaTestClient}
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.Future
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Matchers.anyString
import org.mockito.Matchers.any
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest

import scala.util.Try

class ReportUploadServiceSpec extends AsyncWordSpec with MockitoSugar with  MustMatchers with OptionValues with WsScalaTestClient {



  "ReportUploadServiceSpec" must {

    "proces request " in {

      val writeResult = mock[WriteResult]
      val statusRepository = mock[SubmissionStatusRepository]
      when(statusRepository.updateStatus(anyString(), anyString())).thenReturn(Future.successful(writeResult))
      when(statusRepository.addError(anyString(), anyString())).thenReturn(Future.successful(writeResult))

      val validationService = new ValidationService(new XmlValidator(), new XmlParser(), new CharacterValidator(), new BusinessRules()(null))
      val legacyConnector = mock[LegacyConnector]
      when(legacyConnector.sendBAReport(any(classOf[BAReportRequest]))(any())).thenReturn(Future.successful(Try(100)))

      val reportUploadService = new ReportUploadService(statusRepository, validationService, legacyConnector)

      val res = reportUploadService.upload("username", "password", "<xml>ble</xml>", "reference1")

      res.map { result =>
        result mustBe "ok"
//        verify(statusRepository, times(4)).updateStatus(anyString(), anyString())
//        result mustBe "ok"
      }

    }

  }


}
