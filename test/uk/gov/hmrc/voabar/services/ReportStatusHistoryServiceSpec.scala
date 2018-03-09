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

package uk.gov.hmrc.voabar.services

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.voabar.models.ReportStatus
import uk.gov.hmrc.voabar.repositories.{ReactiveMongoRepository, ReportStatusRepository}
import play.api.Configuration
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class ReportStatusHistoryServiceSpec extends PlaySpec with MockitoSugar {
  val submissionId = "sId999"
  val mockConfig = mock[Configuration]

  class FakeRepositoryInsertFalse extends ReportStatusRepository(mockConfig) {
    object objFakeRepositoryInsertFalse extends ReactiveMongoRepository {
      def insert(rs: ReportStatus): Future[Boolean] = Future.successful(false)
      def getAll(id: String): Future[List[ReportStatus]] = ???
    }

    override def apply(): ReactiveMongoRepository = objFakeRepositoryInsertFalse
  }

  class FakeRepositoryInsert extends ReportStatusRepository(mockConfig) {
    object objFakeRepositoryInsert extends ReactiveMongoRepository {
      def insert(rs: ReportStatus): Future[Boolean] = Future.successful(true)
      def getAll(id: String): Future[List[ReportStatus]] = ???
    }

    override def apply(): ReactiveMongoRepository = objFakeRepositoryInsert
  }

  class FakeRepositoryInsertThrows extends ReportStatusRepository(mockConfig) {
    object objFakeRepositoryInsertThrows extends ReactiveMongoRepository {
      def insert(rs: ReportStatus): Future[Boolean] = Future {
        throw new Exception("Mongo not available!")
      }
      def getAll(id: String): Future[List[ReportStatus]] = ???
    }

    override def apply(): ReactiveMongoRepository = objFakeRepositoryInsertThrows
  }

  "ReportStatusHistoryServiceSpec" must {
    "When recording a submission return a false if the mongo db throws an exception when inserting a document" in {
      val service = new ReportStatusHistoryService(new FakeRepositoryInsertFalse)
      val result = await(service.reportSubmitted(submissionId))
      result mustBe false
    }

    "When recording a submission return a false if the mongo db returns a false when inserting the document" in {
      val service = new ReportStatusHistoryService(new FakeRepositoryInsertThrows)
      val result = await(service.reportSubmitted(submissionId))
      result mustBe false
    }

    "When recording a submission return a true if the mongo db returns a true when inserting the document" in {
      val service = new ReportStatusHistoryService(new FakeRepositoryInsert)
      val result = await(service.reportSubmitted(submissionId))
      result mustBe true
    }
  }

}
