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

import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import org.xml.sax.{ErrorHandler, SAXParseException}
import uk.gov.hmrc.voabar.models.Error

class XmlValidator {

  val schemaFile1 = new StreamSource(getClass.getResourceAsStream("/xsd/ValuebillBAtoVOA-v3-1d.xsd"))
  val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
  factory.setResourceResolver(new ResourceResolver)
  val schema = factory.newSchema(schemaFile1)

  def validate(xml: String):Seq[Error] = {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance
    documentBuilderFactory.setNamespaceAware(true)
    val parser = documentBuilderFactory.newDocumentBuilder

    val errors = scala.collection.mutable.Buffer.empty[Error]

    val errorHandler = new ErrorHandler {
      private def addError(exception: SAXParseException) {
        val split = exception.getMessage.split(":", 2) map (_.trim)
        errors += Error(split(0), Seq(split(1)))
      }

      override def warning(exception: SAXParseException) {
        addError(exception)
      }

      override def error(exception: SAXParseException) {
        addError(exception)
      }

      override def fatalError(exception: SAXParseException) {
        addError(exception)
      }
    }

    parser.setErrorHandler(errorHandler)

    val document = parser.parse(new ByteArrayInputStream(xml.getBytes))

    val validator = schema.newValidator

    validator.setErrorHandler(errorHandler)

    validator.validate(new DOMSource(document))

    Seq(errors: _*)
  }
}
