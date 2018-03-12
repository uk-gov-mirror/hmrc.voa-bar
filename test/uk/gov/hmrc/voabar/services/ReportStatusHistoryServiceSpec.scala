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
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.voabar.models.ReportStatus
import uk.gov.hmrc.voabar.repositories.ReactiveMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportStatusHistoryServiceSpec extends PlaySpec with MockitoSugar {
  val submissionId = "sId999"
  val mockConfig = mock[Configuration]

  object fakeRepositoryInsertFalse extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = Future.successful(false)

    def getAll(id: String): Future[List[ReportStatus]] = ???
  }

  object fakeRepositoryInsert extends ReactiveMongoRepository {
    var capturedReportStatus: Option[ReportStatus] = None

    def insert(rs: ReportStatus): Future[Boolean] = {
      capturedReportStatus = Some(rs)
      Future.successful(true)
    }

    def getAll(id: String): Future[List[ReportStatus]] = ???

    def clearCaptured() = {
      capturedReportStatus = None
    }
  }

  object fakeRepositoryInsertThrows extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = Future {
      throw new Exception("Mongo not available!")
    }

    def getAll(id: String): Future[List[ReportStatus]] = ???
  }

  "ReportStatusHistoryServiceSpec" must {
    "When recording a submission return a false if the mongo db throws an exception when inserting a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertFalse)
      val result = await(service.reportSubmitted(submissionId))
      result mustBe false
    }

    "When recording a submission return a false if the mongo db returns a false when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertThrows)
      val result = await(service.reportSubmitted(submissionId))
      result mustBe false
    }

    "When recording a submission return a true if the mongo db returns a true when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportSubmitted(submissionId))
      result mustBe true
    }

    "when recording a submission insert a ReportStatus with the submissionId string set to submitted id" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportSubmitted(submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.submissionId mustBe submissionId
        case None => assert(false)
      }
    }

    "when recording a submission insert a ReportStatus with the status string set to SUBMITTED" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportSubmitted(submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.status mustBe "SUBMITTED"
        case None => assert(false)
      }
    }
  }

}
