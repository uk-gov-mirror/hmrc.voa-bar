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

import scala.xml._

class XmlParser {

  private def addChild(node:Node,newNode:NodeSeq): Node = node match {
    case Elem(prefix,label,attrs,ns,child@_*) => Elem(prefix,label,attrs,ns,false,newNode: _*)
  }

  def oneReportPerBatch(node:Node):Seq[Node] = {
    val batchHeader = node \ "BAreportHeader"
    val batchTrailer = node \ "BAreportTrailer"
    (node \ "BApropertyReport") map {report => addChild(node,batchHeader ++ report ++ batchTrailer)}
  }
}