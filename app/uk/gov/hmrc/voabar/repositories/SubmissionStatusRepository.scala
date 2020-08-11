/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.ZonedDateTime

import com.google.inject.ImplementedBy
import com.typesafe.config.ConfigException
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.{BSONBuilderHelpers, ReactiveRepository}
import uk.gov.hmrc.voabar.models.{BarError, BarMongoError, Error, ReportStatus, ReportStatusType}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionStatusRepositoryImpl @Inject()(
                                                mongo: ReactiveMongoComponent,
                                                config: Configuration
                                              )
                                              (implicit executionContext: ExecutionContext)
  extends ReactiveRepository[ReportStatus, String](
    collectionName = "submissions",
    mongo = mongo.mongoConnector.db,
    domainFormat = ReportStatus.format,
    idFormat = implicitly[Format[String]]
  ) with SubmissionStatusRepository with BSONBuilderHelpers {

  private val ttlPath = s"$collectionName.timeToLiveInSeconds"
  private val ttl = config.get[Int](ttlPath)

  private val log = Logger(this.getClass)
  override def indexes: Seq[Index] = Seq (
    Index(Seq("baCode" -> IndexType.Hashed), name = Some(s"${collectionName}_baCodeIdx")),
    Index(Seq("created" -> IndexType.Descending), name = Some(s"${collectionName}_createdIdx")
      ,options = BSONDocument("expireAfterSeconds" -> ttl))
  )


  def saveOrUpdate(reportStatus: ReportStatus, upsert: Boolean): Future[Either[BarError, Unit.type]] = {
    val finder = BSONDocument(_Id -> reportStatus.id)
    val modifierBson = set(BSONDocument(
      "created" -> reportStatus.created.toString,
      "checksum" -> reportStatus.checksum,
      "url" -> reportStatus.url,
      "errors" -> reportStatus.errors.getOrElse(Seq()).map(e => BSONDocument(
        "values" -> e.values,
        "code" -> e.code
      )),
      "filename" -> reportStatus.filename.getOrElse(""),
      "status" -> reportStatus.status,
      "report" -> reportStatus.report)
    )

    atomicSaveOrUpdate(reportStatus.id, upsert, finder, modifierBson)
  }

  def saveOrUpdate(userId: String, reference: String, upsert: Boolean)
  : Future[Either[BarError, Unit.type]] = {
    val finder = BSONDocument(_Id -> reference)
    val modifierBson = set(BSONDocument(
      "created" -> ZonedDateTime.now.toString,
      "baCode" -> userId)
    )

    atomicSaveOrUpdate(reference, upsert, finder, modifierBson)
  }

  override def getByUser(baCode: String, filter: Option[String] = None)
  : Future[Either[BarError, Seq[ReportStatus]]] = {
    val finder = filter.fold(BSONDocument("baCode" -> baCode))(f =>
      BSONDocument("baCode" -> baCode, "status" -> f)
    )
    collection.find(finder).sort(Json.obj("created" -> -1)).cursor[ReportStatus](ReadPreference.primary)
      .collect[Seq](-1, Cursor.FailOnError[Seq[ReportStatus]]())
      .map(Right(_))
      .recover {
        case ex: Throwable => {
          val errorMsg = s"Couldn't retrieve BA reports with '$baCode'"
          Logger.warn(s"$errorMsg\n${ex.getMessage}")
          Left(BarMongoError(errorMsg))
        }
      }
  }

  override def getByReference(reference: String)
  : Future[Either[BarError, ReportStatus]] = {
    val finder = BSONDocument(_Id -> reference)
    collection.find(finder).sort(Json.obj("created" -> -1)).cursor[ReportStatus](ReadPreference.primary)
      .collect[Seq](1, Cursor.FailOnError[Seq[ReportStatus]]())
      .map(r => Right(r.head))
      .recover {
        case ex: Throwable => {
          val errorMsg = s"Couldn't retrieve BA reports for reference $reference"
          Logger.warn(s"$errorMsg\n${ex.getMessage}")
          Left(BarMongoError(errorMsg))
        }
      }
  }

  override def getAll(): Future[Either[BarError, Seq[ReportStatus]]] = {
    collection.find(Json.obj()).sort(Json.obj("created" -> -1)).cursor[ReportStatus](ReadPreference.primary)
      .collect[Seq](-1, Cursor.FailOnError[Seq[ReportStatus]]())
      .map(Right(_))
      .recover {
        case ex: Throwable => {
          val errorMsg = s"Couldn't retrieve all BA reports"
          Logger.warn(s"$errorMsg\n${ex.getMessage}")
          Left(BarMongoError(errorMsg))
        }
      }
  }

  protected def atomicSaveOrUpdate(reference: String, upsert: Boolean, finder: BSONDocument, modifierBson: BSONDocument) = {
    val updateDocument = if (upsert) {
      modifierBson ++ setOnInsert(BSONDocument(_Id -> reference))
    } else {
      modifierBson
    }
    val modifier = collection.updateModifier(updateDocument, upsert = upsert)
    collection.findAndModify(finder, modifier)
      .map(response => Either.cond(
        !response.lastError.isDefined || !response.lastError.get.err.isDefined,
        Unit,
        getError(response.lastError.get.err.get))
      )
      .recover {
        case ex: Throwable => {
          val errorMsg = "Error while saving submission"
          Logger.error(errorMsg, ex)
          Left(BarMongoError(errorMsg))
        }
      }
  }

  private def getError(error: String): BarError = {
    val errorMsg = "Error while saving report status"
    Logger.error(s"$errorMsg\n$error")
    BarMongoError(errorMsg)
  }

  override def addError(submissionId: String, error: Error): Future[Either[BarError, Boolean]] = {

    val modifier = BSONDocument(
      "$push" -> BSONDocument(
        "errors" -> error
      )
    )

    collection.update(_id(submissionId), modifier).map { updateResult =>
      if (updateResult.ok && updateResult.n == 1) {
        Right(true)
      } else {
        Left(BarMongoError("unable record error message in mongo", Option(updateResult)))
      }
    }
  }


  override def updateStatus(submissionId: String, status: ReportStatusType): Future[Either[BarError, Boolean]] = {

    val modifier = set(BSONDocument(
        "status" -> status.value
      )
    )

    collection.update.one(_id(submissionId), modifier, upsert = true, multi = false).map { updateResult =>

      if (updateResult.ok && updateResult.n == 1) {
        Right(true)
      } else {
        Left(BarMongoError("unable to update status in mongo", Option(updateResult)))
      }
    }
  }


  override def update(submissionId: String, status: ReportStatusType, totalReports: Int): Future[Either[BarError, Boolean]] = {
    val modifier = set(BSONDocument(
        "status" -> status.value,
        "totalReports" -> totalReports
      )
    )

    collection.update.one(_id(submissionId), modifier, upsert = true, multi = false).map { updateResult =>
      if (updateResult.ok && updateResult.n == 1) {
        Right(true)
      } else {
        Left(BarMongoError("unable to update status in mongo", Option(updateResult)))
      }
    }
  }

  override def deleteByReference(reference: String, user: String): Future[Either[BarError, JsValue]] = {
    val deleteSelector = Json.obj(_Id -> reference, "baCode" -> user)
    log.warn(s"Performing deletion on ${collectionName} with selector: ${deleteSelector}")
    collection.delete.one(deleteSelector).map { deleteResult =>
      val response = Json.obj(
        "code" -> deleteResult.code,
        "n" -> deleteResult.n,
        "writeErrors" -> deleteResult.writeErrors.mkString(","),
        "writeConcernError" -> deleteResult.writeConcernError.map(_.toString)
      )
      log.warn(s"Deletion on ${collectionName} done, returning response : ${response}")
      Right(response)
    }
  }
}

@ImplementedBy(classOf[SubmissionStatusRepositoryImpl])
trait SubmissionStatusRepository {

  def addError(submissionId: String, error: Error): Future[Either[BarError, Boolean]]

  def updateStatus(submissionId: String, status: ReportStatusType): Future[Either[BarError, Boolean]]

  def update(submissionId: String, status: ReportStatusType, totalReports: Int): Future[Either[BarError, Boolean]]

  def getByUser(userId: String, filter: Option[String] = None) : Future[Either[BarError, Seq[ReportStatus]]]

  def getByReference(reference: String) : Future[Either[BarError, ReportStatus]]

  def deleteByReference(reference: String, user: String) : Future[Either[BarError, JsValue]]

  def getAll(): Future[Either[BarError, Seq[ReportStatus]]]

  def saveOrUpdate(reportStatus: ReportStatus, upsert: Boolean): Future[Either[BarError, Unit.type]]

  def saveOrUpdate(userId: String, reference: String, upsert: Boolean): Future[Either[BarError, Unit.type]]
}



