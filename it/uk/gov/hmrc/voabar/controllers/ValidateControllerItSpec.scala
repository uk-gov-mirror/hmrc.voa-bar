package uk.gov.hmrc.voabar.controllers

import java.nio.file.Paths
import java.util.UUID

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}


class ValidateControllerItSpec extends PlaySpec with OneServerPerSuite with DefaultAwaitTimeout with FutureAwaits {

  val BA_LOGIN = "BA5090"

  val requestId = "mdtp-request-" + UUID.randomUUID().toString().replaceAll("-", "")

  val path = Paths.get("test/resources/xml/CTValid1.xml")

  val invalidXml = Paths.get("test/resources/xml/CTInvalid1.xml")

  def wsClient = app.injector.instanceOf[WSClient]


  "Validate controller" should {
    "validate correct xml" in {
      val url = s"http://localhost:${port}/voa-bar/validate-upload/${BA_LOGIN}"

      val response = await(wsClient.url(url)
        .withHeaders("X-Request-ID" -> requestId)
        .put(path.toFile))

      Console.println(response.body)

      response.status mustBe(Status.OK)
    }

    "validate incorrect XML" in {
      val url = s"http://localhost:${port}/voa-bar/validate-upload/7777"

      val response = await(wsClient.url(url)
        .withHeaders("X-Request-ID" -> requestId)
        .put(invalidXml.toFile))

      Console.println(response.body)

      response.status mustBe(Status.OK)


    }
  }



}
