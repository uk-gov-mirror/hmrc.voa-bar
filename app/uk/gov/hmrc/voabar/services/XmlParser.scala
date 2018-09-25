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

import java.io.{ByteArrayInputStream, StringReader}

import javax.xml.parsers.DocumentBuilderFactory
import org.apache.commons.io.input.ReaderInputStream
import org.w3c.dom.Document
import uk.gov.hmrc.voabar.models.{BarError, BarXmlError}

import scala.util.{Failure, Success, Try}
import scala.xml._

class XmlParser {

  val saxFactory = javax.xml.parsers.SAXParserFactory.newInstance()
  saxFactory.setNamespaceAware(false) // Scala parser is little naive. Must be false! Otherwise all namespace information is lost.
  saxFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
  saxFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
  saxFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
  saxFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)


  val documentBuilderFactory = DocumentBuilderFactory.newInstance
  documentBuilderFactory.setNamespaceAware(true)
  documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
  documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
  documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
  documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities",false)
  documentBuilderFactory.setExpandEntityReferences(false) //XXE vulnerable fix


  def parse(xml: String): Either[BarError, Document] = {
    Try {
      val docBuilder = documentBuilderFactory.newDocumentBuilder()
      docBuilder.parse(new ReaderInputStream(new StringReader(xml)))
    } match {
      case Success(value) => Right(value)
      case Failure(x) => Left(BarXmlError(x.getMessage))
    }
  }


  private def addChild(node:Node,newNode:NodeSeq): Node = node match {
    case Elem(prefix,label,attrs,ns,child@_*) => Elem(prefix,label,attrs,ns,false,newNode: _*)
  }

  def oneReportPerBatch(node:Node):Seq[Node] = {
    val batchHeader = node \ "BAreportHeader"
    val batchTrailer = node \ "BAreportTrailer"
    (node \ "BApropertyReport") map {report => addChild(node,batchHeader ++ report ++ batchTrailer)}
  }


  def xmlToNode(xml: String): Either[BarError, Elem] = {
    Try {
      XML.withSAXParser(saxFactory.newSAXParser()).loadString(xml)
    } match {
      case Success(value) => Right(value)
      case Failure(exception) => Left(BarXmlError(exception.getMessage))
    }
  }
}