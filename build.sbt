name := "sttp-zio-streaming-response"

version := "1.0.0"

scalaVersion := "2.13.6"

enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .settings(
    Defaults.itSettings,
    scalacOptions := Settings.scalacOpts,
    logLevel := Level.Info,
    Global / cancelable := true,
    Global / fork := true,
    libraryDependencies ++= Dependencies.serviceDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
