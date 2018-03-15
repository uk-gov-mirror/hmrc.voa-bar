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

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, DefaultDB}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.voabar.models.ReportStatus

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ReactiveMongoRepositoryImpl @Inject() (config: Configuration)
  extends ReactiveRepository[ReportStatus, BSONObjectID](config.getString("appName").get, new MongoDbConnection(){}.db, ReportStatus.format)
  with ReactiveMongoRepository {

  val fieldName = "created"
  val createdIndexName = "userAnswersExpiry"
  val expireAfterSeconds = "expireAfterSeconds"
  val timeToLiveInSeconds: Int = config.getInt("mongodb.timeToLiveInSeconds").get
  val findSubmissionCursorMax = config.getInt("mongodb.findSubmissionCursorMax").get
  val findBaCursorMax = config.getInt("mongodb.findBaCursorMax").get


  createIndex(fieldName, createdIndexName, timeToLiveInSeconds)

  private def createIndex(field: String, indexName: String, ttl: Int): Future[Boolean] = {
    collection.indexesManager.ensure(Index(Seq((field, IndexType.Ascending)), Some(indexName),
      options = BSONDocument(expireAfterSeconds -> ttl))) map {
      result => {
        Logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }

  def insert(rs: ReportStatus): Future[Boolean] = {
    collection.insert[ReportStatus](rs).map {
      lastError => lastError.ok
    }
  }


  def getSubmission(submissionId: String): Future[List[ReportStatus]] = {
    val cursor = collection.find(Json.obj("submissionId" -> submissionId)).cursor[ReportStatus]()
    cursor.collect(findSubmissionCursorMax, Cursor.FailOnError[List[ReportStatus]]())
  }

  def getReportsByBaCode(code:String): Future[List[ReportStatus]] = {
    val cursor = collection.find(Json.obj("baCode" -> code)).cursor[ReportStatus]()
    cursor.collect(findBaCursorMax, Cursor.FailOnError[List[ReportStatus]]())
  }
}

@ImplementedBy(classOf[ReactiveMongoRepositoryImpl])
trait ReactiveMongoRepository {
  def insert(rs: ReportStatus): Future[Boolean]
  def getSubmission(submissionId: String): Future[List[ReportStatus]]
  def getReportsByBaCode(code:String): Future[List[ReportStatus]]
}


