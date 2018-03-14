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

import uk.gov.hmrc.voabar.models._

import scala.xml._

class XmlParser {

  def fromXml(xmlString:String):BABatchReport = {

    val xml:Node = XML.loadString(xmlString)

    BABatchReport(
      BAReports(xml),
      BAReportHeader(xml \ "BAreportHeader"),
      List[BAPropertyReport]((xml \ "BApropertyReport") map {i => BAPropertyReport(i)}: _*),
      BAReportTrailer(xml \ "BAreportTrailer")
    )
  }

//  def toXml(bABatchReport:BABatchReport): Node = {
//    val rootNode:Node = <BAreports></BAreports>
//    val rootWithAttrs = addAttributes(bABatchReport.baReports.attributes,rootNode)
//    val propertyReports = bABatchReport.baPropertyReport.foldLeft(NodeSeq.Empty)((acc, elem) =>
//    acc ++ elem.node)
//    val newNode = bABatchReport.baReportHeader.node ++
//      propertyReports ++ bABatchReport.baReportTrailer.node
//    val completeReport = addChild(newNode,rootWithAttrs)
//    completeReport
//  }

//  private[services] def addAttributes(attributes:MetaData, node:Node): Node = node match {
//    case e:Elem => e%attributes
//  }

//  private[services] def addChild(childs:NodeSeq, node:Node): Node = node match {
//    case Elem(prefix,label,attr,scope,child@_*) => Elem.apply(prefix,label,attr,scope,false,child ++ childs: _*)
//  }

//  def splitBatch(baBatchReport:BABatchReport): Seq[Node] = {
//    smallBatch(baBatchReport).map{batch => toXml(batch)}
//  }

  private def addChild(node:Node,newNode:NodeSeq): Node = node match {
    case Elem(prefix,label,attrs,ns,child@_*) => Elem(prefix,label,attrs,ns,false,newNode: _*)
  }


  private def split(node:Node):Seq[Node] = {
    val batchHeader = node \ "BAreportHeader"
    val batchTrailer = node \ "BAreportTrailer"
    val propertyReports:Seq[Node] = (node \ "BApropertyReport") map {report =>
      val newNode = batchHeader ++ report ++ batchTrailer
      addChild(node,newNode)
    }
    propertyReports
  }


}