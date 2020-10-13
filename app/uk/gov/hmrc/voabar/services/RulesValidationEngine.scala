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

import ebars.xml.BAreports
import ebars.xml.CtaxReasonForReportCodeContentType._
import models.EbarsBAreports._
import models.Purpose
import play.api.Play
import uk.gov.hmrc.voabar.models.{EmptyReportValidation, JobStatusErrorFromStub, ReportValidation}
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesProvider}
import play.api.i18n.Messages.Implicits._

import scala.language.postfixOps

/**
  * Created by rgallet on 16/02/16.
  */
class RulesValidationEngine {

  def applyRules(baReports: BAreports) = {
    val v = baReports.purpose match {
      case Purpose.CT => ReportValidation(Seq.empty[JobStatusErrorFromStub], baReports) map
        CtValidationRules.Cr01AndCr02MissingExistingEntryValidation.apply map
        CtValidationRules.Cr03AndCr04MissingProposedEntryValidation.apply map
        CtValidationRules.Cr05AndCr12MissingProposedEntryValidation.apply map
        CtValidationRules.Cr06AndCr07AndCr09AndCr10AndCr14MissingProposedEntryValidation.apply map
        CtValidationRules.Cr08InvalidCodeValidation.apply map
        TextAddressPostcodeValidation.apply map
        OccupierContactAddressesPostcodeValidation.apply map
        RemarksValidation.apply map
        PropertyPlanReferenceNumberValidation.apply
      case Purpose.NDR => ReportValidation(Seq.empty[JobStatusErrorFromStub], baReports) map
        NdrValidationRules.Rt01AndRt04AndRt03AndRt04MissingProposedEntryValidation.apply map
        NdrValidationRules.Rt05AndRt06AndRt07AndRt08AndRt9AndRt11MissingExistingEntryValidation.apply map
        TextAddressPostcodeValidation.apply map
        OccupierContactAddressesPostcodeValidation.apply map
        RemarksValidation.apply map
        PropertyPlanReferenceNumberValidation.apply
      case _ => EmptyReportValidation()
    }

    v.get
  }
}

sealed trait ValidationRule {

  /** This is temporary. We should return only codes for error and then let frontend to format error message.
   * I put this here to make this code compile, then we need to refactor everything and merge with current BusinessRUlesValidation.
   */
  implicit def messagesProvider: MessagesProvider = {
    Play.current.injector.instanceOf[MessagesApi].preferred(Seq(Lang.defaultLang))
  }

  def apply: (BAreports) => Option[JobStatusErrorFromStub]
}

case object NdrValidationRules {

  case object Rt01AndRt04AndRt03AndRt04MissingProposedEntryValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      lazy val proposed = EbarsXmlCutter.findFirstProposedEntriesIdx(baReports)

      EbarsXmlCutter.CR(baReports) match {
        case Some("1") | Some("2") | Some("3") | Some("4") if proposed.isEmpty =>
          Some(JobStatusErrorFromStub("Rt01AndRt04AndRt03AndRt04MissingProposedEntryValidation", Messages("Rt01AndRt04AndRt03AndRt04MissingProposedEntryValidation")))
        case _ => None
      }
    }
  }

  case object Rt05AndRt06AndRt07AndRt08AndRt9AndRt11MissingExistingEntryValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      lazy val existing = EbarsXmlCutter.findFirstExistingEntriesIdx(baReports)

      EbarsXmlCutter.CR(baReports) match {
        case Some(v) if Seq("5", "6", "7", "8", "9", "11").contains(v) && existing.isEmpty =>
          Some(JobStatusErrorFromStub("Rt05AndRt06AndRt07AndRt08AndRt9AndRt11MissingExistingEntryValidation", Messages("Rt05AndRt06AndRt07AndRt08AndRt9AndRt11MissingExistingEntryValidation")))
        case _ => None
      }
    }
  }

}

case object CtValidationRules {

  case object Cr01AndCr02MissingExistingEntryValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      lazy val existing = EbarsXmlCutter.findFirstExistingEntriesIdx(baReports)

      EbarsXmlCutter.CR(baReports) match {
        case Some(CR_01) | Some(CR_02) if existing.isEmpty =>
          Some(JobStatusErrorFromStub("Cr01AndCr02MissingExistingEntryValidation", Messages("Cr01AndCr02MissingExistingEntryValidation")))
        case _ => None
      }
    }
  }

  case object Cr03AndCr04MissingProposedEntryValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      lazy val proposed = EbarsXmlCutter.findFirstProposedEntriesIdx(baReports)

      EbarsXmlCutter.CR(baReports) match {
        case Some(CR_03) | Some(CR_04) if proposed.isEmpty =>
          Some(JobStatusErrorFromStub("Cr03AndCr04MissingProposedEntryValidation", Messages("Cr03AndCr04MissingProposedEntryValidation")))
        case _ => None
      }
    }
  }

  case object Cr05AndCr12MissingProposedEntryValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      lazy val existing = EbarsXmlCutter.findFirstExistingEntriesIdx(baReports)
      lazy val proposed = EbarsXmlCutter.findFirstProposedEntriesIdx(baReports)

      EbarsXmlCutter.CR(baReports) match {
        case Some(CR_05) | Some(CR_12) if proposed.isEmpty || existing.isEmpty =>
          Some(JobStatusErrorFromStub("Cr05AndCr12MissingProposedEntryValidation", Messages("Cr05AndCr12MissingProposedEntryValidation")))
        case _ => None
      }
    }
  }

  case object Cr06AndCr07AndCr09AndCr10AndCr14MissingProposedEntryValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      lazy val existing = EbarsXmlCutter.findFirstExistingEntriesIdx(baReports)

      EbarsXmlCutter.CR(baReports) match {
        case Some(v) if Seq(CR_06, CR_07, CR_09, CR_10, CR_14).contains(v) && existing.isEmpty =>
          Some(JobStatusErrorFromStub("Cr06AndCr07AndCr09AndCr10AndCr14MissingProposedEntryValidation", Messages("Cr06AndCr07AndCr09AndCr10AndCr14MissingProposedEntryValidation")))
        case _ => None
      }
    }
  }

  case object Cr08InvalidCodeValidation extends ValidationRule {
    override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

      EbarsXmlCutter.CR(baReports) match {
        case Some(CR_08) => Some(JobStatusErrorFromStub("Cr08InvalidCodeValidation", Messages("Cr08InvalidCodeValidation")))
        case Some(CR_11) => Some(JobStatusErrorFromStub("Cr11InvalidCodeValidation", Messages("Cr11InvalidCodeValidation")))
        case Some(CR_13) => Some(JobStatusErrorFromStub("Cr13InvalidCodeValidation", Messages("Cr13InvalidCodeValidation")))
        case _ => None
      }
    }
  }

}


case object TextAddressPostcodeValidation extends ValidationRule {
  val postcodePattern = "([A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][A-Z-[CIKMOV]]{2})".r

  override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

    val f = EbarsXmlCutter.TextAddressStructures(baReports) map (_.getPostcode) map {
      case v if v == null || v.isEmpty => None
      case postcodePattern(zip) => None
      case v => Some(JobStatusErrorFromStub("TextAddressPostcodeValidation", Messages("TextAddressPostcodeValidation", v)))
    } flatten

    f headOption
  }
}

case object OccupierContactAddressesPostcodeValidation extends ValidationRule {
  val postcodePattern = "([A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][A-Z-[CIKMOV]]{2})".r

  override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>

    val f = EbarsXmlCutter.OccupierContactAddresses(baReports) map (_.getPostCode) map {
      case v if v == null || v.isEmpty => None
      case postcodePattern(zip) => None
      case v => Some(JobStatusErrorFromStub("OccupierContactAddressesPostcodeValidation", Messages("OccupierContactAddressesPostcodeValidation", v)))
    } flatten

    f headOption
  }
}

case object RemarksValidation extends ValidationRule {
  override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>
    EbarsXmlCutter.Remarks(baReports) flatMap {
      case v if v.length <= 1 => Some(JobStatusErrorFromStub("RemarksValidation", Messages("RemarksValidationNotEmpty")))
      case v if v.length > 240 => Some(JobStatusErrorFromStub("RemarksValidation", Messages("RemarksValidationTooLong")))
      case _ => None
    } headOption
  }
}

case object PropertyPlanReferenceNumberValidation extends ValidationRule {
  override def apply: (BAreports) => Option[JobStatusErrorFromStub] = { baReports =>
    EbarsXmlCutter.PropertyPlanReferenceNumber(baReports) flatMap {
      case v if v.length > 25 => Some(JobStatusErrorFromStub("PropertyPlanReferenceNumberValidation", Messages("PropertyPlanReferenceNumberValidation")))
      case _ => None
    } headOption
  }
}
