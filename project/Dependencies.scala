import sbt._

object Dependencies {

  val serviceDependencies = Seq(
    "dev.zio" %% "zio-http" % Version.zioHttp,

    "dev.zio" %% "zio" % Version.zio,
    "dev.zio" %% "zio-streams" % Version.zio,

    "dev.zio" %% "zio-logging" % Version.logging,
    "dev.zio" %% "zio-logging-slf4j" % Version.logging,
    "org.slf4j" % "slf4j-api" % Version.slf4j,

    "ch.qos.logback" % "logback-classic" % Version.logBack,
    "ch.qos.logback.contrib" % "logback-jackson" % Version.logbackContrib,
    "ch.qos.logback.contrib" % "logback-json-classic" % Version.logbackContrib,

    "dev.zio" %% "zio-test" % Version.zio % Test,
    "dev.zio" %% "zio-test-sbt" % Version.zio % Test,

    "com.softwaremill.sttp.client3" %% "core" % Version.sttp,
    "com.softwaremill.sttp.client3" %% "zio" % Version.sttp,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % Version.sttp,
  )

}

object Version {
  val zio = "2.0.10"
  val slf4j = "1.7.32"
  val logBack = "1.2.5"
  val zioHttp = "3.0.0-RC9"
  val logging = "2.1.1"
  val logbackContrib = "0.1.5"
  val sttp = "3.9.7"
}
