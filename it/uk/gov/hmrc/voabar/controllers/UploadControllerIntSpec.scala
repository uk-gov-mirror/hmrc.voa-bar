package uk.gov.hmrc.voabar.controllers

import java.io.FileInputStream
import java.util.UUID

import org.apache.commons.io.IOUtils
import org.eclipse.persistence.config.ResultType
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
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest
import play.api.inject.bind

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
    .configure("mongodb.uri" -> ("mongodb://localhost:27017/voa-bar" + UUID.randomUUID().toString))
    .bindings(bind[LegacyConnector].to(legacyConnector))
    .build()

  lazy val controller = app.injector.instanceOf[UploadController]
  lazy val mongoComponent = app.injector.instanceOf(classOf[ReactiveMongoComponent])
  lazy val collection = mongoComponent.mongoConnector.db().collection[JSONCollection]("reportstatus")

  def fakeRequestWithXML = {

    val xmlNode = IOUtils.toString(new FileInputStream("test/resources/xml/CTValid1.xml"))
    FakeRequest("POST", "/request?reference=1234")
      .withHeaders(
        "Content-Type" -> "text/plain",
        "Content-Length" -> s"${xmlNode.length}",
        "BA-Code" -> "1234",
        "password" -> "pass1")
      .withTextBody(xmlNode)
  }


  "Upload controller " must {

    "properly handle correct XML " ignore {

      await(collection.insert(BSONDocument(
        "_id" -> "1234"
      )))

      controller.upload()(fakeRequestWithXML)

      Thread.sleep(5000)

      checkDatabaseStatus("1234", "Done")

      true mustBe(true)
    }

  }


  private def checkDatabaseStatus(id: String, expectedStatus: String) = {

    import reactivemongo.play.json._
    import reactivemongo.play.json.collection.{
      JSONCollection, JsCursor
    }, JsCursor._
    type ResultType = JsObject

    val collection = mongoComponent.mongoConnector.db().collection[JSONCollection]("submission")

    val query = BSONDocument("_id" -> BSONDocument("$eq" -> id))

    val result = await(collection.find(query).cursor[ResultType](ReadPreference.Primary).jsArray())

    val status = (result(0) \ "status").as[String]

    status mustBe(expectedStatus)

  }


  override protected def afterAll(): Unit = {
    mongoComponent.mongoConnector.db().drop()
    mongoComponent.mongoConnector.close()
  }

}
