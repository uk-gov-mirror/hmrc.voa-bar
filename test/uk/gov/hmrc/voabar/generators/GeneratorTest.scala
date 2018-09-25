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

package uk.gov.hmrc.voabar.generators

import java.io.StringWriter

import javax.xml.bind.{JAXBContext, Marshaller}
import org.scalacheck.Gen.Parameters
import org.scalacheck.rng.Seed
import org.scalatest.{FlatSpec, Matchers}

class GeneratorTest extends FlatSpec with Matchers {



  "generator" should "generate some values" in {


    val repGen = BaReportGenerator.baReportGenerator

    val res = repGen(Parameters.default, Seed(100L))




    val context = JAXBContext.newInstance("ebars.xml")

    val marshaller = context.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

    val stringWriter = new StringWriter()

    marshaller.marshal(res.get, stringWriter)

    Console.println(stringWriter.toString)


//    JAXBContext context = JAXBContext
//      .newInstance(CreateExemptionCertificate.class);
//    Marshaller m = context.createMarshaller();
//    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//
//    m.marshal(cc, System.out)




    true shouldBe true
  }


}
