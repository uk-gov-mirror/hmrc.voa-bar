package uk.gov.hmrc.voabar.controllers

import java.io.FileInputStream
import java.time.ZonedDateTime
import java.util.UUID

import org.apache.commons.io.IOUtils
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, EitherValues, OptionValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

import scala.concurrent.ExecutionContext.Implicits.global
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.ReadPreference
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import play.api.inject.bind
import uk.gov.hmrc.voabar.models.ReportStatus
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}


class UploadControllerIntSpec extends PlaySpec with BeforeAndAfterAll with OptionValues
  with EitherValues with DefaultAwaitTimeout with FutureAwaits with GuiceOneAppPerSuite  with MockitoSugar {

  val legacyConnector = mock[LegacyConnector]

  when(legacyConnector.sendBAReport(any(classOf[BAReportRequest]))(any(classOf[ExecutionContext]))).thenAnswer(new Answer[Future[Try[Int]]] {
    override def answer(invocation: InvocationOnMock): Future[Try[Int]] = {
      Future.successful(Success(200))
    }
  })

  override def fakeApplication() = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> ("mongodb://localhost:27017/voa-bar"))
    .bindings(bind[LegacyConnector].to(legacyConnector))
    .build()

  lazy val controller = app.injector.instanceOf[UploadController]
  lazy val mongoComponent = app.injector.instanceOf(classOf[ReactiveMongoComponent])
  lazy val collection = mongoComponent.mongoConnector.db().collection[JSONCollection]("submissions")
  lazy val submissionRepository = app.injector.instanceOf[SubmissionStatusRepository]

  def fakeRequestWithXML = {

    val xmlNode = IOUtils.toString(new FileInputStream("test/resources/xml/CTValid1.xml"))
    FakeRequest("POST", "/request?reference=1234")
      .withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Length" -> s"${xmlNode.length}",
        "BA-Code" -> "9999",
        "password" -> "pass1")
      .withTextBody(xmlNode)
  }


  "Upload controller " must {

    "properly handle correct XML " in {

      val reportStatus = ReportStatus("1234", ZonedDateTime.now)

      await(submissionRepository.saveOrUpdate(reportStatus, true))

      controller.upload()(fakeRequestWithXML)

      Thread.sleep(3000)

      val report = await(submissionRepository.getByReference("1234"))

      report must be('right)

      Console.println(report)

      report.right.value.status.value mustBe "Done"

    }

  }

  override protected def afterAll(): Unit = {
 //   mongoComponent.mongoConnector.db().drop()
    mongoComponent.mongoConnector.close()
  }

}
