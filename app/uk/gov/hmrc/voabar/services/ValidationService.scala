/*
 * Copyright 2020 HM Revenue & Customs
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

import ebars.xml.{BApropertySplitMergeStructure, BAreportBodyStructure, BAreports, TextAddressStructure}
import javax.inject.Singleton
import javax.xml.bind.JAXBElement
import play.api.Logger
import services.EbarsValidator
import uk.gov.hmrc.voabar.models.{BaLogin, BarError, BarValidationError, Error}
import uk.gov.hmrc.voabar.util._

import scala.collection.JavaConverters._
import scala.util.{Random, Try}

@Singleton
class ValidationService {
  val x = new EbarsValidator()
  val log = Logger(this.getClass)

  val randomForDevelopment = Random //TODO remove after full development !!!!!
                                    // DO NOT MERGE WITH THIS

  def validate(submissions: BAreports, baLogin: BaLogin): Either[BarError, Unit] = {

    //TODO make functional
    val headerErros = validateHeaderTrailer(submissions, baLogin)
    if(headerErros.isEmpty) {
      val bodyErrros = validateBody(submissions)
      if(bodyErrros.isEmpty) {
        Right(())
      }else {
        Left(BarValidationError(bodyErrros))
      }
    }else {
      Left(BarValidationError(headerErros))
    }
  }

  def validateBody(submissions: BAreports): List[Error] = {

    x.split(submissions).flatMap { submission =>
      validateSubmission(submission)
    }.toList

  }


  /**
   *
   * @param submission only one submission!!!!.
   * @return
   */
  def validateSubmission(submission: BAreports): List[Error] = {
    assert(submission.getBApropertyReport.size() == 1, "Single submission validation can contain only one submission")

    if(randomForDevelopment.nextBoolean()) {
      List.empty
    }else {
      List(
        Error(NONE_EXISTING, Seq.empty, createSubmissionDetailDescription(submission))
      )
    }

  }

  def createSubmissionDetailDescription(submission: BAreports): Option[String] = {
    //TODO should we have assert or just return None, or take head ???
    //TODO maybe delete after full development.
    assert(submission.getBApropertyReport.size() == 1, "createPropertyDescription can create description for only one submission")

    submission.getBApropertyReport.asScala.headOption.map { submission =>
      val planningReference = submission.getContent.asScala
        .find(x => x.getName.getLocalPart == "PropertyPlanReferenceNumber" && !x.isNil)
        .flatMap { planningReference =>
          Option(planningReference.asInstanceOf[JAXBElement[String]].getValue).map(_.trim).filter(_ != "")
        }

      val proposedEntries = extractUprn(submission, "ProposedEntries")
        .map(x => s"UPRN: ${x._1.map(_.toString).getOrElse("NONE")}, Address: ${x._2.getOrElse("NONE")}")

      val existingEntries = extractUprn(submission, "ExistingEntries")
        .map(x => s"UPRN: ${x._1.map(_.toString).getOrElse("NONE")}, Address: ${x._2.getOrElse("NONE")}")

      s"Planning ref: ${planningReference.getOrElse("NONE")}, Proposed: ${proposedEntries.mkString("|  ")},   " +
        s"   Existing: ${existingEntries.mkString("|  ")}"
    }
  }

  def extractUprn(submission: BAreportBodyStructure, entries: String) = {
    Try {
      submission.getContent.asScala.find(x => x.getName.getLocalPart == entries && !x.isNil)
        .map(x => x.asInstanceOf[JAXBElement[BApropertySplitMergeStructure]].getValue)
        .map(x => x.getAssessmentProperties.asScala.toList)
        .fold(List.empty[BApropertySplitMergeStructure.AssessmentProperties])(identity)
        .map { x =>
          val address = x.getPropertyIdentity.getContent.asScala
            .find(z => z.getName.getLocalPart == "TextAddress" && !z.isNil) //TextAddressStructure
            .map(z => z.asInstanceOf[JAXBElement[TextAddressStructure]].getValue)
            .map(z => {
              z.getAddressLine.asScala.map(_.trim).filter(_ != "").mkString(", ") + Option(z.getPostcode).getOrElse("")
            })

          val propertyNumber = x.getPropertyIdentity.getContent.asScala
            .find(z => z.getName.getLocalPart == "UniquePropertyReferenceNumber" && !z.isNil)
            .flatMap(x => Option(x.asInstanceOf[JAXBElement[Long]].getValue))

          (propertyNumber, address)
        }
    }.fold(t => {
      Logger.warn("Unable to extract UPRN: ", t)
      List.empty[(Option[Long], Option[String])]
    }, identity)
  }


  def validateHeaderTrailer(submission: BAreports, baLogin: BaLogin): List[Error] = {
    validationBACode(submission, baLogin)

  }


  def validationBACode(submission: BAreports, baLogin: BaLogin): List[Error]  = {

    Option(submission.getBAreportHeader.getBillingAuthorityIdentityCode) match {
      case None => List(Error(BA_CODE_REPORT, Seq("'BAidentityNumber' missing.")))
      case Some(baCode) if (baCode == 0) => List(Error(BA_CODE_REPORT, Seq("'BAidentityNumber' missing.")))
      case Some(baCode) if (baCode == baLogin.baCode) => List.empty
      case Some(wrongBaNumber) => List(Error(BA_CODE_MATCH, Seq(wrongBaNumber.toString)))
    }

  }

}
