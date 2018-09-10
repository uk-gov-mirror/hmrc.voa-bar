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
import reactivemongo.api.DB
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.voabar.models.{BarError, BarMongoError}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubmissionStatusRepositoryImpl @Inject()(mongo: () => DB)(implicit executionContext: ExecutionContext) extends SubmissionStatusRepository {

  lazy val collection = mongo().collection[JSONCollection]("submission")

  def idSelector(submissionId: String) = BSONDocument("_id" -> submissionId)

  override def addError(submissionId: String, error: String): Future[Either[BarError, Boolean]] = {

    val modifier = BSONDocument(
      "$push" -> BSONDocument(
        "errors" -> BSONDocument(
          "error_code" -> 10,
          "error_message" -> error
        )
      )
    )

    collection.update(idSelector(submissionId), modifier).map { updateResult =>

      if (updateResult.ok && updateResult.n == 1) {
        Right(true)
      } else {
        Left(BarMongoError("unable record error message in mongo", Option(updateResult)))
      }
    }

  }

  override def updateStatus(submissionId: String, status: String): Future[Either[BarError, Boolean]] = {

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "status" -> status
      )
    )

    collection.update(idSelector(submissionId), modifier).map { updateResult =>

      if (updateResult.ok && updateResult.n == 1) {
        Right(true)
      } else {
        Left(BarMongoError("unable to update status in mongo", Option(updateResult)))
      }
    }
  }
}

trait SubmissionStatusRepository {

  def addError(submissionId: String, error: String): Future[Either[BarError, Boolean]]

  def updateStatus(submissionId: String, status: String): Future[Either[BarError, Boolean]]

}



