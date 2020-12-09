package uk.gov.hmrc

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

trait WiremockHelper {

  def withWiremock[T](stubbing: WireMockServer => Unit)(block: Int => T):T = {

    val wireMockServer = new WireMockServer(WireMockConfiguration
      .options()
      .dynamicPort()
    )
    wireMockServer.start()
    stubbing(wireMockServer)
    val result = block(wireMockServer.port())
    wireMockServer.stop()
    result
  }

  def withWiremockServer[T](stubbing: WireMockServer => Unit)(block: (Int, WireMockServer) => T):T = {

    val wireMockServer = new WireMockServer(WireMockConfiguration
      .options()
      .dynamicPort()
    )
    wireMockServer.start()
    stubbing(wireMockServer)
    val result = block(wireMockServer.port(), wireMockServer)
    wireMockServer.stop()
    result
  }

}
