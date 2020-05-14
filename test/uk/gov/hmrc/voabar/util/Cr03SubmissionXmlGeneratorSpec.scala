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

package uk.gov.hmrc.voabar.util

import java.io.StringWriter
import java.nio.file.Files
import java.time.LocalDate
import java.util.UUID

import ebars.xml.BAreports
import javax.xml.bind.{JAXBContext, Marshaller}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.voabar.models.{Address, ContactDetails, Cr03Submission, NoPlanningApplicationSubmitted, NotApplicablePlanningPermission, NotRequiredPlanningPermission, PermittedDevelopment, WithoutPlanningPermission}
import uk.gov.hmrc.voabar.services.{XmlParser, XmlValidator}

class Cr03SubmissionXmlGeneratorSpec extends FlatSpec with MustMatchers with EitherValues with ScalaCheckPropertyChecks{

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSuccessful = 2000)

  val parser = new XmlParser()
  val validator = new XmlValidator()

  val jaxb = JAXBContext.newInstance(classOf[BAreports])
  val jaxbMarshaller = jaxb.createMarshaller()
  jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

  def genRestrictedString(min: Int = 1, max: Int = 8) = for {
    lenght <- Gen.chooseNum(min,max)
    str <- Gen.containerOfN[List,Char](lenght, Gen.alphaChar)
  }yield (str.mkString)

  def genEffectiveDate = for {
    year <- Gen.chooseNum(1900, 2010)
    month <- Gen.chooseNum(1,12)
    day <- Gen.chooseNum(1,28)
  } yield(LocalDate.of(year, month, day))

  def genAddress = for {
    line1 <- genRestrictedString()
    line2 <- genRestrictedString()
    line3 <- Gen.option(genRestrictedString())
    line4 <- Gen.option(genRestrictedString())
  }yield (Address(line1, line2, line3, line4, "BN12 4AX"))

  def genContactDetails = for {
    firstName <- genRestrictedString()
    lastName <- genRestrictedString()
    email <- Gen.option(genRestrictedString())
    phone <- Gen.option(Gen.chooseNum(1l,4851025447l))
  }yield (ContactDetails(firstName, lastName, email, phone.map("0" + _.toString)))

  def genPlanningReference = for {
    planningRef <- Gen.option(genRestrictedString())
    noPlanningRef <- Gen.oneOf(WithoutPlanningPermission, NotApplicablePlanningPermission, NotRequiredPlanningPermission, PermittedDevelopment, NoPlanningApplicationSubmitted)
  }yield {
    if(planningRef.isDefined) {
      (planningRef, None)
    }else {
      (planningRef, Some(noPlanningRef))
    }
  }

  def getSubmission = for {
    baReport <- genRestrictedString()
    baRef <- genRestrictedString()
    uprn <- Gen.option(Gen.posNum[Int])
    address <- genAddress
    contactAddress <- Gen.option(genAddress)
    contactDetails <- genContactDetails
    effectiveDate <- genEffectiveDate
    (planningRef, noPlanningRef) <- genPlanningReference
    comment <- Gen.option(genRestrictedString(max=226))
  }yield (Cr03Submission(baReport, baRef, uprn.map(_.toString), address, contactDetails, contactAddress.isEmpty, contactAddress, effectiveDate, planningRef.isDefined, planningRef, noPlanningRef, comment))

  implicit val arbt = Arbitrary(getSubmission)

  "CR03 generator" should "generate valid xml" in {
    val jaxbStructure = new Cr03SubmissionXmlGenerator(aCR03Submission(), 1010, "Brighton and Hove", UUID.randomUUID().toString).generateXml()
    val xml = printXml(jaxbStructure)

    validateXml(xml)

    true must be(true)
  }

  it should "generate valid XML for all generated submissions" in {
    val id = UUID.randomUUID().toString
    forAll {(submission: Cr03Submission) =>
      val jaxbStructure = new Cr03SubmissionXmlGenerator(submission, 1010, "Brighton and Hove", id).generateXml()
      val xml = printXml(jaxbStructure)

      validateXml(xml)

      true must be(true)
    }

  }

  def validateXml(xml: String): Unit = {
      val file = Files.createTempFile("test-xml", ".xml")
      Files.write(file, xml.getBytes("UTF-8"))
      val dom = parser.parse(file.toUri.toURL).right.get
      val validation = validator.validate(dom)

      if(validation.isLeft) {
        Console.println(s"\n\n\n${validation.left}\n\n${xml}")
      }

      validation.right.value mustBe(true)

  }

  def printXml(report: BAreports): String = {
    val sw = new StringWriter()
    jaxbMarshaller.marshal(report, sw)
    sw.toString
  }


  def aCR03Submission(): Cr03Submission = {
    val address = Address("line 1", "line2", Option("line3"), None, "BN12 4AX")
    val contactDetails = ContactDetails("John", "Doe", Option("john.doe@example.com"), Option("054252365447"))
    Cr03Submission("baReport", "baRef", Option("112541"), address, contactDetails,
      true, None, LocalDate.now(), true, Some("22212"), None, Option("comment")
    )

  }


}
