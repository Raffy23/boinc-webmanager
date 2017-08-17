// (5) shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

enablePlugins(ScalaJSPlugin)

name := "Boinc-Webmanager"
version := "0.1b"
scalaVersion in ThisBuild := "2.12.2"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

//scalaJSUseMainModuleInitializer := true

val akkaVersion = "2.4.19"
val akkHttpVersion = "10.0.9"
val http4sVersion = "0.15.14a"

lazy val root = project.in(file(".")).
  aggregate(clientJS, serverJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val manager = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(
    name := "Boinc-Webmanager",
    version := "0.1b-SNAPSHOT"
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "com.github.benhutchison" %% "prickle" % "1.1.13",
      "com.github.pureconfig" %% "pureconfig" % "0.7.2",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.3",
      "com.github.benhutchison" %%% "prickle" % "1.1.13",
      "com.github.japgolly.scalacss" %%% "core" % "0.5.3",
      "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.5.3",
      "com.lihaoyi" %%% "scalatags" % "0.6.5"
    ),
    resolvers += "WebJars-BinTray" at "https://dl.bintray.com/webjars/maven",
    jsDependencies ++= Seq(
      "org.webjars.npm" % "navigo" % "5.3.1" / "navigo.js" commonJSName "Navigo" minified "navigo.min.js",
      "org.webjars.bower" % "nprogress" % "0.2.0" / "nprogress.js" commonJSName "NProgress"
    )
  )

lazy val shared = project in file("shared")

lazy val serverJVM = manager.jvm.dependsOn(shared).settings(mainClass in assembly := Some("at.happywetter.boinc.WebServer"), test in assembly := {})
lazy val clientJS = manager.js.dependsOn(shared).settings(mainClass := Some("at.happywetter.boinc.web.Main"))