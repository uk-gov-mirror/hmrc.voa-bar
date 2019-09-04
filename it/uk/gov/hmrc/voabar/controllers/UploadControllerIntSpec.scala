package uk.gov.hmrc.voabar.controllers

import java.nio.file.Paths
import java.time.ZonedDateTime

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, EitherValues, OptionValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import play.api.inject.bind
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voabar.models.{ReportStatus, UploadDetails}
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository

import scala.concurrent.{ExecutionContext, Future}


class UploadControllerIntSpec extends PlaySpec with BeforeAndAfterAll with OptionValues
  with EitherValues with DefaultAwaitTimeout with FutureAwaits with GuiceOneAppPerSuite  with MockitoSugar {

  val legacyConnector = mock[LegacyConnector]

  when(legacyConnector.sendBAReport(any(classOf[BAReportRequest]))(any[ExecutionContext], any[HeaderCarrier])).thenAnswer(new Answer[Future[Int]] {
    override def answer(invocation: InvocationOnMock): Future[Int] = {
      Future.successful(200)
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
  lazy val configuration = app.injector.instanceOf[play.api.Configuration]

  lazy val crypto = new ApplicationCrypto(configuration.underlying).JsonCrypto

  def fakeRequestWithXML = {

    val xmlURL = Paths.get("test/resources/xml/CTValid1.xml").toAbsolutePath.toUri.toURL.toString

    FakeRequest("POST", "/voa-bar/upload")
      .withHeaders(
        "BA-Code" -> "BA5090",
        "password" -> crypto.encrypt(PlainText("BA5090")).value
      )
      .withBody(UploadDetails("1234", xmlURL))
  }


  "Upload controller " must {

    "properly handle correct XML " in {

      val reportStatus = ReportStatus("1234", ZonedDateTime.now)

      await(submissionRepository.insertOrMerge(reportStatus))

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
