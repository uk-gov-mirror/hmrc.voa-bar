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

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import play.api.Logger
import uk.gov.hmrc.voabar.models.ReportStatus
import uk.gov.hmrc.voabar.repositories.ReactiveMongoRepository

import scala.concurrent.Future
import uk.gov.hmrc.voabar.models.Error

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ReportStatusHistoryServiceImpl @Inject() (statusRepository: ReactiveMongoRepository) extends ReportStatusHistoryService {
  def reportSubmitted(baCode: String, submissionId: String): Future[Boolean] = {
    val status = ReportStatus(baCode, submissionId, "SUBMITTED")
    statusRepository.insert(status) map identity recover {
      case t: Throwable => {
        Logger.warn(s"Mongo exception while inserting SUBMITTED status for $submissionId with message ${t.getMessage}")
        false
      }
    }
  }

  def reportCheckedWithNoErrorsFound(baCode: String, submissionId: String): Future[Boolean] = {
    val status = ReportStatus(baCode, submissionId, "VALIDATED")
    statusRepository.insert(status) map identity recover {
      case t: Throwable => {
        Logger.warn(s"Mongo exception while inserting VALIDATED status for $submissionId with message ${t.getMessage}")
        false
      }
    }
  }

  def reportCheckedWithErrorsFound(baCode: String, submissionId: String, errors: Seq[Error]): Future[Boolean] = {
    val status = ReportStatus(baCode, submissionId, "INVALIDATED", errors)
    statusRepository.insert(status) map identity recover {
      case t: Throwable => {
        Logger.warn(s"Mongo exception while inserting INVALIDATED status for $submissionId with message ${t.getMessage}")
        false
      }
    }
  }

  def reportForwarded(baCode: String, submissionId: String): Future[Boolean] = {
    val status = ReportStatus(baCode, submissionId, "FORWARDED")
    statusRepository.insert(status) map identity recover {
      case t: Throwable => {
        Logger.warn(s"Mongo exception while inserting FORWARDED status for $submissionId with message ${t.getMessage}")
        false
      }
    }
  }

  def findReportsBySubmission(submissionId: String): Future[Option[List[ReportStatus]]] = {
    statusRepository.getSubmission(submissionId) map { statuses => Some(statuses) } recover {
      case t: Throwable => {
        Logger.warn(s"Mongo exception while inserting FORWARDED status for $submissionId with message ${t.getMessage}")
        None
      }
    }
  }

  def findReportsByBaCode(code: String): Future[Option[Map[String, List[ReportStatus]]]] = {
    statusRepository.getReportsByBaCode(code) map { statuses => Some(statuses.groupBy(_.submissionId)) } recover {
      case t: Throwable => {
        Logger.warn(s"Mongo exception while inserting FORWARDED status for $code with message ${t.getMessage}")
        None
      }
    }
  }
}

@Deprecated
@ImplementedBy(classOf[ReportStatusHistoryServiceImpl])
trait ReportStatusHistoryService {
  def reportSubmitted(baCode: String, submissionId: String): Future[Boolean]

  def reportCheckedWithNoErrorsFound(baCode: String, submissionId: String): Future[Boolean]

  def reportCheckedWithErrorsFound(baCode: String, submissionId: String, errors: Seq[Error]): Future[Boolean]

  def reportForwarded(baCode: String, submissionId: String): Future[Boolean]

  def findReportsBySubmission(submissionId: String): Future[Option[List[ReportStatus]]]

  def findReportsByBaCode(code: String): Future[Option[Map[String, List[ReportStatus]]]]
}

