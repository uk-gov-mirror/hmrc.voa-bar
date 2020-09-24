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

package uk.gov.hmrc.repositories

import java.time.ZonedDateTime
import java.util.UUID

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.voabar.models.{BarMongoError, Done, Error, Failed, Pending, ReportStatus, Submitted}
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepositoryImpl
import uk.gov.hmrc.voabar.util.{CHARACTER, ErrorCode, INVALID_XML_XSD, TIMEOUT_ERROR}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionStatusRepositorySpec extends PlaySpec with BeforeAndAfterAll
  with EitherValues with DefaultAwaitTimeout with FutureAwaits  with GuiceOneAppPerSuite with MockitoSugar {

  override def fakeApplication() = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> ("mongodb://localhost:27017/voa-bar" + UUID.randomUUID().toString))
    .build()

  lazy val mongoComponent = app.injector.instanceOf(classOf[ReactiveMongoComponent])


  val repo = app.injector.instanceOf(classOf[SubmissionStatusRepositoryImpl])

  "repository" should {

    "add error" in {
      await(repo.collection.insert(BSONDocument(
        "_id" -> "111"
      )))

      val reportStatusError = Error(CHARACTER , Seq( "message", "detail"))

      val dbResult = await(repo.addError("111", reportStatusError))

      dbResult must be('right)

    }

    "add error without description" in {
      await(repo.collection.insert(BSONDocument(
        "_id" -> "ggggg"
      )))

      val reportStatusError = Error(CHARACTER , List())

      val dbResult = await(repo.addError("ggggg", reportStatusError))

      dbResult must be('right)

    }

    "update status" in {
      await(repo.collection.insert(BSONDocument(
        "_id" -> "222"
      )))

      val dbResult = await(repo.updateStatus("222", Submitted))

      dbResult must be('right)

    }

    "failed for nonExisting UUID" ignore {
        val dbResul = await(repo.updateStatus("nonExistingSubmissionID", Submitted))

        dbResul mustBe('Left)

        dbResul.left.value mustBe a [BarMongoError]

    }

    "serialise and deserialize ReportStatus" in {

      val dateTime = ZonedDateTime.now()

      val guid = UUID.randomUUID().toString

      val reportStatus = ReportStatus(guid, dateTime, None, None, None, Option("BA2220"), None)

      await(repo.insert(reportStatus))


      val res = await(repo.getByReference(guid))

      res mustBe ('right)

      res.right.value mustBe reportStatus


    }

    "Change status to failed for submission after timeout" in {

      val report = aReport().copy(created = ZonedDateTime.now().minusMinutes(121))

      await(repo.saveOrUpdate(report, true))

      val reportFromDb = await(repo.getByReference(report.id))

      reportFromDb.right.value.status.value mustBe(Failed.value)

      reportFromDb.right.value.errors.value mustBe(Seq(Error(TIMEOUT_ERROR)))

    }

    "Not change status or anything else for final submission state" in {
      import org.scalatest.prop.TableDrivenPropertyChecks._
      val finalStates = Table(("Final state", "errors"),
        (Submitted.value, Option.empty),
        (Done.value, Option.empty),
        (Failed.value, Option(Seq(Error(INVALID_XML_XSD, Seq("Additional", "Parameters")))))
      )

      forAll (finalStates) { case (finalState: String, errors: Option[Seq[Error]]) =>
        val report = aReport().copy(created = ZonedDateTime.now().minusDays(21), status = Option(finalState), errors = errors)

        await(repo.insert(report))

        val reportFromDb = await(repo.getByReference(report.id))

        reportFromDb.right.value.status.value mustBe(finalState)

        reportFromDb.right.value.errors mustBe errors

        reportFromDb.right.value mustBe report
      }
    }
  }

  def aReport(): ReportStatus = {
    ReportStatus(UUID.randomUUID().toString, ZonedDateTime.now(), None, None, None, Option("BA1010"), Some(Pending.value), None, None, None
    )
  }

  override protected def afterAll(): Unit = {
    mongoComponent.mongoConnector.db().drop()
    mongoComponent.mongoConnector.close()
  }
}
