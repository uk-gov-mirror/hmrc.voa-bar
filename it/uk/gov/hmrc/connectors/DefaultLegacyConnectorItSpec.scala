package uk.gov.hmrc.connectors

import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import ebars.xml.BAreports
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.EbarsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class DefaultLegacyConnectorItSpec extends PlaySpec with GuiceOneAppPerSuite {

  private val wiremockPort = 8891

  def legacyConnector = app.injector.instanceOf[LegacyConnector]
  implicit def ec = app.injector.instanceOf[ExecutionContext]

  val ebarsValidator = new EbarsValidator()

  val timeout = 1000 milliseconds

  "LegacyConnector" must {
    "send all request as UTF-8 with encoding in mime type" in {

      import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
      import com.github.tomakehurst.wiremock.client.WireMock._


      val wireMockServer = new WireMockServer(options().port(wiremockPort))
      wireMockServer.start()
      wireMockServer.stubFor(
        post(urlEqualTo("/autobars-stubs/v2/submit"))
          .willReturn(
            aResponse().withStatus(200)
          )
      )

      implicit val hc = HeaderCarrier()

      val jsonString = ebarsValidator.toJson(aBaReport)

      val baReportReques = BAReportRequest(
        UUID.randomUUID().toString,
        jsonString,
        "BA5090",
        "BA5090",
        1
      )

      val result = legacyConnector.sendBAReport(baReportReques)

      val httpResult = Await.result(result, timeout)
      httpResult mustBe 200

      wireMockServer.verify(postRequestedFor(urlEqualTo("/autobars-stubs/v2/submit"))
        .withHeader("Content-Type", equalTo("text/plain; charset=UTF-8")))

      wireMockServer.stop()

      true mustBe true

    }
  }


  def aBaReport: BAreports = {
    val ctx = JAXBContext.newInstance("ebars.xml")
    val unmarshaller = ctx.createUnmarshaller()
    val streamSource = new StreamSource("test/resources/xml/CTValid2.xml")
    unmarshaller.unmarshal( streamSource , classOf[BAreports]).getValue
  }



}
