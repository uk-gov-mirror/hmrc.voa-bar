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

import java.util.UUID

import org.scalatest.mockito.MockitoSugar
import play.api.inject.Injector
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.voabar.models.{BarMongoError, Error, Submitted}
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepositoryImpl
import uk.gov.hmrc.voabar.util.ErrorCode

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionStatusRepositorySpec extends PlaySpec with BeforeAndAfterAll
  with EitherValues with DefaultAwaitTimeout with FutureAwaits  with GuiceOneAppPerSuite with MockitoSugar {

  val mongoConponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = new MongoConnector(
      "mongodb://localhost:27017/voa-bar" + UUID.randomUUID().toString)
  }

  "repository" should {

    val config = app.injector.instanceOf(classOf[Configuration])
    val repo = new SubmissionStatusRepositoryImpl(mongoConponent, config)

    "add error" in {
      await(repo.collection.insert(BSONDocument(
        "_id" -> "111"
      )))

      val reportStatusError = Error(ErrorCode.CHARACTER , Seq( "message", "detail"))

      val dbResult = await(repo.addError("111", reportStatusError))

      dbResult must be('right)

    }

    "update status" in {
      await(repo.collection.insert(BSONDocument(
        "_id" -> "222"
      )))

      val dbResult = await(repo.updateStatus("222", Submitted))

      dbResult must be('right)

    }

    "failed for nonExisting UUID" in {
        val dbResul = await(repo.updateStatus("nonExistingSubmissionID", Submitted))

        dbResul mustBe('Left)

        dbResul.left.value mustBe a [BarMongoError]

    }

  }

  override protected def afterAll(): Unit = {
    await(mongoConponent.mongoConnector.db().drop())
    mongoConponent.mongoConnector.close()
  }
}
