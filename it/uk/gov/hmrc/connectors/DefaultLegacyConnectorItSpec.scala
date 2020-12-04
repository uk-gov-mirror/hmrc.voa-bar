package uk.gov.hmrc.connectors

import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import ebars.xml.BAreports
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.Injecting
import services.EbarsValidator
import uk.gov.hmrc.WiremockHelper
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.voabar.Utils
import uk.gov.hmrc.voabar.connectors.{DefaultLegacyConnector, LegacyConnector}
import uk.gov.hmrc.voabar.models.EbarsRequests.BAReportRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class DefaultLegacyConnectorItSpec extends PlaySpec with WiremockHelper with GuiceOneAppPerSuite with Injecting {

  def legacyConnector(port: Int) = {

    val config = inject[Configuration]

    val servicesConfig = new ServicesConfig(config ++ Configuration("microservice.services.autobars-stubs.port" -> port),
      inject[RunMode])

    new DefaultLegacyConnector(inject[HttpClient], servicesConfig, inject[Utils], inject[ApplicationCrypto])

  }
  implicit def ec = app.injector.instanceOf[ExecutionContext]

  val ebarsValidator = new EbarsValidator()

  val timeout = 1000 milliseconds

  "LegacyConnector" must {
    "send all request as UTF-8 with encoding in mime type" in {

      withWiremockServer { wireMockServer: WireMockServer =>
        import com.github.tomakehurst.wiremock.client.WireMock._
        wireMockServer.stubFor(
          post(urlEqualTo("/autobars-stubs/v2/submit"))
            .willReturn(
              aResponse().withStatus(200)
            )
        )
      }{ (port: Int, wireMockServer: WireMockServer) =>
        import com.github.tomakehurst.wiremock.client.WireMock._
        implicit val hc = HeaderCarrier()

        val jsonString = ebarsValidator.toJson(aBaReport)

        val baReportReques = BAReportRequest(
          UUID.randomUUID().toString,
          jsonString,
          "BA5090",
          "BA5090",
          1
        )

        val result = legacyConnector(port).sendBAReport(baReportReques)

        val httpResult = Await.result(result, timeout)
        httpResult mustBe 200

        wireMockServer.verify(postRequestedFor(urlEqualTo("/autobars-stubs/v2/submit"))
                .withHeader("Content-Type", equalTo("text/plain; charset=UTF-8")))

      }

    }
  }


  def aBaReport: BAreports = {
    val ctx = JAXBContext.newInstance("ebars.xml")
    val unmarshaller = ctx.createUnmarshaller()
    val streamSource = new StreamSource("test/resources/xml/CTValid2.xml")
    unmarshaller.unmarshal( streamSource , classOf[BAreports]).getValue
  }



}
