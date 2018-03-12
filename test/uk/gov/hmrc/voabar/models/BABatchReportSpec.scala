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

package uk.gov.hmrc.voabar.models

import org.scalatestplus.play.PlaySpec
import scala.xml.NodeSeq

class BABatchReportSpec extends PlaySpec {

  val baReports:BAReports = BAReports(<a b="1"></a>.attributes)
  val headerNode: NodeSeq = <BAreportHeader>TEST SAMPLE1</BAreportHeader>
  val trailerNode: NodeSeq = <BAreportTrailer>TEST SAMPLE2</BAreportTrailer>
  val report1: NodeSeq = <BApropertyReport>SAMPLE1</BApropertyReport>
  val report2: NodeSeq = <BApropertyReport>SAMPLE2</BApropertyReport>
  val report3: NodeSeq = <BApropertyReport>SAMPLE3</BApropertyReport>
  val report4: NodeSeq = <BApropertyReport>SAMPLE4</BApropertyReport>

  "Given a header node, trailer node and a list of property report nodes produce a BatchSubmission model containing 4 reports" in {
    val batchHeader = BAReportHeader(headerNode)
    val batchTrailer = BAReportTrailer(trailerNode)
    val bAPropertyReports = List(BAPropertyReport(report1), BAPropertyReport(report2), BAPropertyReport(report3), BAPropertyReport(report4))

    val batchSubmission = BABatchReport(baReports,batchHeader, bAPropertyReports, batchTrailer)

    batchSubmission.baReports mustBe baReports
    batchSubmission.baReportTrailer.node mustBe trailerNode
    batchSubmission.baPropertyReport.size mustBe 4
  }

}
