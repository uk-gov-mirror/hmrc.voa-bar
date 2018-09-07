package uk.gov.hmrc.voabar.models

sealed trait BarError


case class BarXmlError(message: String) extends BarError

case class BarValidationError(errors: List[uk.gov.hmrc.voabar.models.Error]) extends BarError
