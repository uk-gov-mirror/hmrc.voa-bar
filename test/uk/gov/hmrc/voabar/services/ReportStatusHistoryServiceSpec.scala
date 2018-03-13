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
import uk.gov.hmrc.voabar.models.{Error, ReportStatus}
import uk.gov.hmrc.voabar.repositories.ReactiveMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportStatusHistoryServiceSpec extends PlaySpec with MockitoSugar {
  val baCode = "ba1221"
  val submissionId = "sId999"
  val mockConfig = mock[Configuration]
  val errors = Seq(Error("BAD-CHAR", Seq("ba1221")))
  val rs0 = ReportStatus(baCode, submissionId, "SUBMITTED")
  val rs1 = ReportStatus(baCode, submissionId, "INVALIDATED", errors)
  val rs2 = ReportStatus(baCode, "altSubmission", "SUBMITTED")
  val statuses = List(rs0, rs1)
  val altStatuses = List(rs2)

  object fakeRepositoryInsertFalse extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = Future.successful(false)

    def getSubmission(id: String): Future[List[ReportStatus]] = ???

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = ???
  }

  object fakeRepositoryInsert extends ReactiveMongoRepository {
    var capturedReportStatus: Option[ReportStatus] = None

    def insert(rs: ReportStatus): Future[Boolean] = {
      capturedReportStatus = Some(rs)
      Future.successful(true)
    }

    def getSubmission(id: String): Future[List[ReportStatus]] = ???

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = ???

    def clearCaptured() = {
      capturedReportStatus = None
    }
  }

  object fakeRepositoryInsertThrows extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = Future {
      throw new Exception("Mongo not available!")
    }

    def getSubmission(id: String): Future[List[ReportStatus]] = ???

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = ???
  }

  object fakeRepositoryFindSubmissionThrows extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = ???

    def getSubmission(id: String): Future[List[ReportStatus]] = Future {
      throw new Exception("Mongo not available!")
    }

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = ???
  }

  object fakeRepositoryFindBySubmissionNothing extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = ???

    def getSubmission(id: String): Future[List[ReportStatus]] = Future.successful(List())

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = ???
  }

  object fakeRepositoryFindSubmission extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = ???

    def getSubmission(id: String): Future[List[ReportStatus]] = Future.successful(statuses)

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = ???
  }

  object fakeRepositoryFindBaThrows extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = ???

    def getSubmission(id: String): Future[List[ReportStatus]] = ???

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = Future {
      throw new Exception("Mongo not available!")
    }
  }

  object fakeRepositoryFindByBaNothing extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = ???

    def getSubmission(id: String): Future[List[ReportStatus]] = ???

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = Future.successful(List())
  }

  object fakeRepositoryFindByBa extends ReactiveMongoRepository {
    def insert(rs: ReportStatus): Future[Boolean] = ???

    def getSubmission(id: String): Future[List[ReportStatus]] = ???

    def getReportsByBaCode(code: String): Future[List[ReportStatus]] = Future.successful(statuses ++ altStatuses)
  }

  "ReportStatusHistoryService" must {
    "When recording a submission return a false if the mongo db throws an exception when inserting a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertThrows)
      val result = await(service.reportSubmitted(baCode, submissionId))
      result mustBe false
    }

    "When recording a submission return a false if the mongo db returns a false when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertFalse)
      val result = await(service.reportSubmitted(baCode, submissionId))
      result mustBe false
    }

    "When recording a submission return a true if the mongo db returns a true when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportSubmitted(baCode, submissionId))
      result mustBe true
    }

    "when recording a submission insert a ReportStatus with the submissionId string set to submitted id" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportSubmitted(baCode, submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.submissionId mustBe submissionId
        case None => assert(false)
      }
    }

    "when recording a submission insert a ReportStatus with the status string set to SUBMITTED" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportSubmitted(baCode, submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.status mustBe "SUBMITTED"
        case None => assert(false)
      }
    }

    "When recording a check with no errors found return a false if the mongo db throws an exception when inserting a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertThrows)
      val result = await(service.reportCheckedWithNoErrorsFound(baCode, submissionId))
      result mustBe false
    }

    "When recording a check with no errors found return a false if the mongo db returns a false when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertFalse)
      val result = await(service.reportCheckedWithNoErrorsFound(baCode, submissionId))
      result mustBe false
    }

    "When recording a check with no errors found return a true if the mongo db returns a true when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithNoErrorsFound(baCode, submissionId))
      result mustBe true
    }

    "when recording a check with no errors insert a ReportStatus with the submissionId string set to submitted id" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithNoErrorsFound(baCode, submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.submissionId mustBe submissionId
        case None => assert(false)
      }
    }

    "when recording a check with no errors insert a ReportStatus with the status string set to SUBMITTED" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithNoErrorsFound(baCode, submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.status mustBe "VALIDATED"
        case None => assert(false)
      }
    }

    "When recording a check with errors found return a false if the mongo db throws an exception when inserting a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertThrows)
      val result = await(service.reportCheckedWithErrorsFound(baCode, submissionId, errors))
      result mustBe false
    }

    "When recording a check with errors found return a false if the mongo db returns a false when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertFalse)
      val result = await(service.reportCheckedWithErrorsFound(baCode, submissionId, errors))
      result mustBe false
    }

    "When recording a check with errors found return a true if the mongo db returns a true when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithErrorsFound(baCode, submissionId, errors))
      result mustBe true
    }

    "when recording a check with errors insert a ReportStatus with the submissionId string set to submitted id" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithErrorsFound(baCode, submissionId, errors))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.submissionId mustBe submissionId
        case None => assert(false)
      }
    }

    "when recording a check with errors insert a ReportStatus with the status string set to SUBMITTED" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithErrorsFound(baCode, submissionId, errors))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.status mustBe "INVALIDATED"
        case None => assert(false)
      }
    }

    "when recording a check with errors insert a ReportStatus with the errors sequence set to Seq(Error(BAD-CHAR, Seq(ba1221)))" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportCheckedWithErrorsFound(baCode, submissionId, errors))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.errors mustBe errors
        case None => assert(false)
      }
    }

    "When recording a forwarded return a false if the mongo db throws an exception when inserting a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertThrows)
      val result = await(service.reportForwarded(baCode, submissionId))
      result mustBe false
    }

    "When recording a forwarded found return a false if the mongo db returns a false when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsertFalse)
      val result = await(service.reportForwarded(baCode, submissionId))
      result mustBe false
    }

    "When recording a forwarded found return a true if the mongo db returns a true when inserting the document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportForwarded(baCode, submissionId))
      result mustBe true
    }

    "when recording a forwarded insert a ReportStatus with the submissionId string set to submitted id" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportForwarded(baCode, submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.submissionId mustBe submissionId
        case None => assert(false)
      }
    }

    "when recording a forwarded insert a ReportStatus with the status string set to FORWARDED" in {
      fakeRepositoryInsert.clearCaptured
      val service = new ReportStatusHistoryService(fakeRepositoryInsert)
      val result = await(service.reportForwarded(baCode, submissionId))
      fakeRepositoryInsert.capturedReportStatus match {
        case Some(rs) => rs.status mustBe "FORWARDED"
        case None => assert(false)
      }
    }

    "When finding the details of a submission return a None if the mongo throws an exception when finding a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryFindSubmissionThrows)
      val result = await(service.findReportsBySubmission(submissionId))
      result mustBe None
    }

    "When finding the details of a submission return a Some empty list if the mongo db returns an empty list of matching documents" in {
      val service = new ReportStatusHistoryService(fakeRepositoryFindBySubmissionNothing)
      val result = await(service.findReportsBySubmission(submissionId))
      result match {
        case Some(list) => list.isEmpty mustBe true
        case None => assert(false)
      }
    }

    "When finding the details of a submission return a Some list if the mongo db returns an list of matching documents" in {
      val service = new ReportStatusHistoryService(fakeRepositoryFindSubmission)
      val result = await(service.findReportsBySubmission(submissionId))
      result match {
        case Some(list) => list mustBe statuses
        case None => assert(false)
      }
    }

    "When finding the reports for a BA return a None if the mongo throws an exception when finding a document" in {
      val service = new ReportStatusHistoryService(fakeRepositoryFindBaThrows)
      val result = await(service.findReportsByBaCode(baCode))
      result mustBe None
    }

    "When finding the reports for a BA return a Some empty list if the mongo db returns an empty map of matching documents" in {
      val service = new ReportStatusHistoryService(fakeRepositoryFindByBaNothing)
      val result = await(service.findReportsByBaCode(baCode))
      result match {
        case Some(map) => map.isEmpty mustBe true
        case None => assert(false)
      }
    }

    "When finding the reports for a BA return a Some list if the mongo db returns an list of matching documents" in {
      val service = new ReportStatusHistoryService(fakeRepositoryFindByBa)
      val result = await(service.findReportsByBaCode(baCode))
      result match {
        case Some(map) =>
          map.isDefinedAt(submissionId) mustBe true
          val reports0 = map.getOrElse(submissionId, Seq())
          reports0.contains(rs0) mustBe true
          reports0.contains(rs1) mustBe true

          map.isDefinedAt("altSubmission") mustBe true
          val reports1 = map.getOrElse("altSubmission", Seq())
          reports1.contains(rs2) mustBe true

        case None => assert(false)
      }
    }
  }
}
