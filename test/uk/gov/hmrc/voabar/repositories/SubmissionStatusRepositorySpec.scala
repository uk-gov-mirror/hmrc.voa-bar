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

package uk.gov.hmrc.voabar.repositories

import org.scalatest.{AsyncFeatureSpec, AsyncFlatSpecLike, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import reactivemongo.api.{DB, MongoConnection}
import reactivemongo.bson.BSONDocument

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import reactivemongo.play.json.ImplicitBSONHandlers._

class SubmissionStatusRepositorySpec extends PlaySpec with BeforeAndAfterAll {

  val mongoDriver = new reactivemongo.api.MongoDriver

  val mongoConnection: MongoConnection = mongoDriver.connection(List("localhost"))

  val db = Await.result(mongoConnection.database("voa-bar"), 500 millisecond)


  "repository" should {
    "add error" in {

      val repo = new SubmissionStatusRepositoryImpl( () => db)

      Await.ready(repo.collection.insert(BSONDocument(
        "_id" -> "111"
      )), 500 millisecond)

      val dbResult = Await.result(repo.addError("111", "error"), 500 millisecond)

      dbResult.writeErrors mustBe empty
      dbResult.n mustBe 1

    }


    "update status" in {
      val repo = new SubmissionStatusRepositoryImpl( () => db)

      Await.ready(repo.collection.insert(BSONDocument(
        "_id" -> "222"
      )), 500 millisecond)

      val dbResult = Await.result(repo.updateStatus("222", "new_status"), 500 millisecond)

      dbResult.writeErrors mustBe empty
      dbResult.n mustBe 1

    }

  }

  override protected def afterAll(): Unit = {
    Await.ready(db.drop(), 500 millisecond )
    mongoConnection.close()
    mongoDriver.close(500 millisecond)
  }
}
