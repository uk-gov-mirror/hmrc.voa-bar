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

import uk.gov.hmrc.voabar.models.{Error, _}

import scala.util.matching.Regex
import scala.xml.Node

class CharacterValidator {

  private val validCharacterRegex:Regex = """(['A-Z0-9\s\-&+\.@\(\):\/])+""".r

  private def stringIsValid(input: String): Boolean = {
    val result = validCharacterRegex.findAllIn(input).toList
    val resultLength = result.length

    resultLength match {
      case length if length > 1 || length == 0 => false
      case length if length == 1 && result.mkString.length != input.length => false
      case _ => true
    }
  }

  def validateChars(node:Node, location:String): List[Error] = {
    def validate(n: Node, errors: List[Error]): List[Error] = n match {
      case e: Node if e.isAtom => errors
      case f: Node if f.child.size == 1 && f.child.head.isAtom && !stringIsValid(f.text) =>
        validate(f.child.head, Error("1000", Seq(location, f.label, f.text)) :: errors)
      case g: Node => g.child.toList.flatMap(c => validate(c, errors))
      case _ => throw new RuntimeException("Character validation failed on an unknown data object")
    }
    validate(node,List[Error]())
  }
}
