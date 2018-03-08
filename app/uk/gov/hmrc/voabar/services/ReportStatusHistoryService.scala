package uk.gov.hmrc.voabar.services

import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class ReportStatusHistoryService @Inject() (){
  def reportSubmitted(submissionId: String): Try[Unit] = ???

  def reportCheckedWithNoErrorsFound(submissionId: String): Try[Unit] = ???

  def reportCheckedWithErrorsFound(submissionId: String, errors: Seq[]): Try[Unit] = ???

  def reportForwarded(submissionId: String): Try[Unit] = ???

}
