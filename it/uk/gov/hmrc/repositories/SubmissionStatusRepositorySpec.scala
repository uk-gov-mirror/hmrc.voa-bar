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

import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.MongoConnection
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.voabar.models.{BarMongoError, Failed, ReportStatusError, Submitted}
import uk.gov.hmrc.voabar.repositories.{ReactiveMongoRepository, SubmissionStatusRepositoryImpl}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionStatusRepositorySpec extends PlaySpec with BeforeAndAfterAll
  with EitherValues with DefaultAwaitTimeout with FutureAwaits {

  val mongoConponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = new MongoConnector(
      "mongodb://localhost:27017/voa-bar" + UUID.randomUUID().toString)
  }

  "repository" should {

    val repo = new SubmissionStatusRepositoryImpl(mongoConponent)

    "add error" in {
      await(repo.collection.insert(BSONDocument(
        "_id" -> "111"
      )))

      val reportStatusError = ReportStatusError("ERR_CODE", "message", "detail")

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
