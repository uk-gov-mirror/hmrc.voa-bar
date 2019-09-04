/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.voabar.controllers

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.voabar.repositories.SubmissionStatusRepository
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.voabar.models.{BarMongoError, ReportStatus}
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionStatusControllerSpec extends PlaySpec with MockitoSugar {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val id = "id"
  val date = ZonedDateTime.now
  val userId = "userId"
  val reportStatus = ReportStatus(
    id = id,
    created = date,
    url = Some("url.com"),
    baCode = Some(userId)
  )
  val reportStatusJson = Json.toJson(reportStatus)
  val reportStatusesJson = Json.toJson(Seq(reportStatus))
  val fakeRequest = FakeRequest("", "").withBody(reportStatusJson).withHeaders(("BA-Code", userId))
  val error = BarMongoError("error")
  "SubmissionStatusController" should {
    "save a new report status successfully" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.insertOrMerge(any[ReportStatus])) thenReturn(Future.successful(Right(())))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.save()(fakeRequest)

      status(response) mustBe NO_CONTENT
    }
    "return invalid status when saving fails" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.insertOrMerge(any[ReportStatus])) thenReturn(Future.successful(Left(error)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.save()(fakeRequest)

      status(response) mustBe INTERNAL_SERVER_ERROR
    }
    "save a new report status user info successfully" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.saveOrUpdate(any[String], any[String], any[Boolean])) thenReturn(Future.successful(Right(Unit)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.saveUserInfo()(fakeRequest)

      status(response) mustBe NO_CONTENT
    }
    "return invalid status when saving user info fails" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.saveOrUpdate(any[String], any[String], any[Boolean])) thenReturn(Future.successful(Left(error)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.saveUserInfo()(fakeRequest)

      status(response) mustBe INTERNAL_SERVER_ERROR
    }
    "returns report statuses when search by user id" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.getByUser(any[String], any[Option[String]])) thenReturn(Future.successful(Right(Seq(reportStatus))))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.getByUser()(fakeRequest).run()

      status(response) mustBe OK
      contentAsJson(response) mustBe reportStatusesJson
    }
    "returns invalid error when search by user id unsuccessfully" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.getByUser(any[String], any[Option[String]])) thenReturn(Future.successful(Left(error)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.getByUser()(fakeRequest).run()

      status(response) mustBe INTERNAL_SERVER_ERROR
    }
    "returns report statuses when search by submission id" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.getByReference(any[String])) thenReturn(Future.successful(Right(reportStatus)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.getByReference(id)(fakeRequest).run()

      status(response) mustBe OK
      contentAsJson(response) mustBe reportStatusJson
    }
    "returns invalid error when search by submission id unsuccessfully" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.getByReference(any[String])) thenReturn(Future.successful(Left(error)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.getByReference(id)(fakeRequest).run()

      status(response) mustBe INTERNAL_SERVER_ERROR
    }
    "returns all report statuses" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.getAll()) thenReturn (Future.successful(Right(Seq(reportStatus))))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.getAll()(fakeRequest).run()

      status(response) mustBe OK
      contentAsJson(response) mustBe reportStatusesJson
    }

    "returns invalid error when search all unsuccessfully" in {
      val submissionStatusRepositoryMock = mock[SubmissionStatusRepository]
      when(submissionStatusRepositoryMock.getAll()) thenReturn(Future.successful(Left(error)))
      val submissionStatusController = new SubmissionStatusController(submissionStatusRepositoryMock)

      val response = submissionStatusController.getAll()(fakeRequest).run()

      status(response) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
