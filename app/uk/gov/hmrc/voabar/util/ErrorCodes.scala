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

package uk.gov.hmrc.voabar.util

import scala.reflect.runtime.universe._
import play.api.libs.json._
import reactivemongo.bson.{BSONReader, BSONString, BSONWriter}

sealed trait ErrorCode { val errorCode: String }
case object CHARACTER extends ErrorCode {val errorCode = "1000"}
case object ONE_PROPOSED extends ErrorCode { val errorCode = "1001"}
case object NONE_EXISTING extends ErrorCode {val errorCode = "1002"}
case object EITHER_ONE_EXISTING_OR_ONE_PROPOSED extends ErrorCode { val errorCode = "1003"}
case object ATLEAST_ONE_PROPOSED extends ErrorCode { val errorCode= "1004" }
case object ATLEAST_ONE_EXISTING extends ErrorCode { val errorCode= "1005" }
case object NOT_IN_USE extends ErrorCode { val errorCode= "1006" }
case object ONE_EXISTING extends ErrorCode { val errorCode= "1007" }
case object NONE_PROPOSED extends ErrorCode { val errorCode = "1008" }
case object BA_CODE_MATCH extends ErrorCode { val errorCode= "1010" }
case object BA_CODE_REQHDR extends ErrorCode { val errorCode= "1011" }
case object BA_CODE_REPORT extends ErrorCode { val errorCode= "1012" }
case object UNSUPPORTED_TAX_TYPE extends ErrorCode { val errorCode= "1020"}
case object UNKNOWN_TYPE_OF_TAX extends ErrorCode { val errorCode= "1021" }
case object UNKNOWN_DATA_OBJECT extends ErrorCode { val errorCode= "1022" }

case object INVALID_XML_XSD extends ErrorCode { val errorCode = "2000"}
case object INVALID_XML extends ErrorCode { val errorCode = "2001"}

case object EBARS_UNAVAILABLE extends ErrorCode { val errorCode = "3000" }

object ErrorCode {
  private lazy val errorCodeClasses: Map[String, ErrorCode] = typeOf[ErrorCode]
    .typeSymbol
    .asClass
    .knownDirectSubclasses.foldLeft(Map[String, ErrorCode]()) { (acc, c) =>
      val classSymbol = c.asType.asClass
      val classMirror = scala.reflect.runtime.currentMirror.reflectClass(classSymbol)
      val primaryConstructor = classSymbol.primaryConstructor.asMethod
      val constructorMirror = classMirror.reflectConstructor(primaryConstructor)
      val instance = constructorMirror().asInstanceOf[ErrorCode]
      acc + (instance.errorCode -> instance)
    }
  implicit val reader: Reads[ErrorCode] = new Reads[ErrorCode] {
    override def reads(json: JsValue): JsResult[ErrorCode] = {
      val value = json.validate[String].get
      JsSuccess(errorCodeClasses.get(value).get)
    }
  }
  implicit val writer: Writes[ErrorCode] = new Writes[ErrorCode] {
    override def writes(o: ErrorCode): JsValue = Json.toJson[String](o.errorCode)
  }
  implicit val errorCodeReader = new BSONReader[BSONString, ErrorCode] {

    override def read(bson: BSONString): ErrorCode =
      errorCodeClasses.get(bson.value).get
  }

  implicit val erorrCodeWriter = BSONWriter[ErrorCode, BSONString](e => BSONString(e.errorCode))
}

