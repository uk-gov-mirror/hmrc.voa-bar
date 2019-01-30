import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

object Dependencies {

  private val mockitoAllVersion = "1.10.19"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val httpCachingClientVersion = "8.0.0"
  private val playReactivemongoVersion = "6.0.0"
  private val reactiveMongoVersion = "6.2.0"
  private val bootstrapVersion = "4.8.0"
  private val autobarsXsdVersion = "8.0.0"
  private val guiceUtilsVersion = "4.2.2"
  private val catsCoreVersion = "1.5.0"

  lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo"           % reactiveMongoVersion,
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25"            % bootstrapVersion,
    "uk.gov.hmrc" %% "autobars-xsd"                 % autobarsXsdVersion,
    "uk.gov.hmrc" %% "http-caching-client"          % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-reactivemongo"           % playReactivemongoVersion,
    "net.codingwell" %% "scala-guice"               % guiceUtilsVersion,
    "org.typelevel" %% "cats-core"                  % catsCoreVersion
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-all" % mockitoAllVersion % scope
  )

}
