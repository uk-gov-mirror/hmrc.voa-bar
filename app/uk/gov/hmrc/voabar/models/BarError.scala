package uk.gov.hmrc.voabar.models

import reactivemongo.api.commands.UpdateWriteResult

sealed trait BarError


case class BarXmlError(message: String) extends BarError

case class BarValidationError(errors: List[uk.gov.hmrc.voabar.models.Error]) extends BarError

case class BarMongoError(error: String, updateWriteResult: Option[UpdateWriteResult]) extends BarError

case class BarEbarError(ebarError: String) extends BarError
