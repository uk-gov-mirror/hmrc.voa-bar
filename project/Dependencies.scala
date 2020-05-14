import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

object Dependencies {

  private val mockitoAllVersion = "1.10.19"
  private val scalaTestPlusPlayVersion = "3.1.3"
  private val httpCachingClientVersion = "9.0.0-play-26"
  private val reactiveMongoVersion = "7.22.0-play-26"
  private val bootstrapVersion = "1.3.0"
  private val autobarsXsdVersion = "8.0.0"
  private val guiceUtilsVersion = "4.2.2"
  private val catsCoreVersion = "1.6.1"
  private val saxonHeVersion = "9.9.1-6"
  private val xercesVersion = "2.12.0"

  private val akkaVersion     = "2.5.23"
  private val akkaHttpVersion = "10.0.15"

  lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo"           % reactiveMongoVersion,
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26"            % bootstrapVersion,
    "uk.gov.hmrc" %% "autobars-xsd"                 % autobarsXsdVersion,
    "uk.gov.hmrc" %% "http-caching-client"          % httpCachingClientVersion,
    "net.codingwell" %% "scala-guice"               % guiceUtilsVersion,
    "org.typelevel" %% "cats-core"                  % catsCoreVersion,
    "net.sf.saxon" % "Saxon-HE"                     % saxonHeVersion,
    "xerces" % "xercesImpl"                         % xercesVersion

  )

  val dependencyOverrides = Seq(
    "com.typesafe.akka" %% "akka-stream"    % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-actor"     % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion force()
  )

  def test(scope: String = "test,it") = Seq(
    //"org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
    "org.scalacheck" %% "scalacheck" % "1.14.1" % scope
  )

}
