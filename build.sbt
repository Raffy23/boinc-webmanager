// (5) shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoPackage
import sbtcrossproject.{CrossType, crossProject}

enablePlugins(ScalaJSPlugin)
enablePlugins(GitVersioning)

name := "Boinc-Webmanager"

scalaVersion in ThisBuild := "2.12.3"
scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Ypartial-unification"
)

git.gitTagToVersionNumber := { tag: String =>
  if(tag matches "[0-9]+\\..*") Some(tag)
  else None
}

//scalaJSUseMainModuleInitializer := true
val http4sVersion = "0.18.0"
val circeVersion = "0.9.1"


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
    name := "Boinc-Webmanager"
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.7.2",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.auth0" % "java-jwt" % "3.2.0",
      "com.lihaoyi" %% "scalatags" % "0.6.7",
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.jsoup" % "jsoup" % "1.10.3",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.4",
      "com.github.japgolly.scalacss" %%% "core" % "0.5.5",
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion ,
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1"
    ),
    resolvers += "WebJars-BinTray" at "https://dl.bintray.com/webjars/maven",
    jsDependencies ++= Seq(
      "org.webjars.bower" % "navigo" % "7.0.0" / "navigo.js" commonJSName "Navigo" minified "navigo.min.js",
      "org.webjars.bower" % "nprogress" % "0.2.0" / "nprogress.js" commonJSName "NProgress",
      "org.webjars.bower" % "chart.js" % "2.6.0" / "Chart.js" commonJSName "ChartJS" minified "Chart.min.js",

      // Polyfill Dependencies needed for IE / Edge to be able to run it
      "org.webjars.npm" % "text-encoding" % "0.6.4" / "encoding.js",
      ProvidedJS / "polyfill-nodelist.js"
    )
  )

lazy val shared = (project in file("shared"))
  .settings(
    aggregate in assembly := false
  ).disablePlugins(AssemblyPlugin)

lazy val serverJVM = manager.jvm.dependsOn(shared)
  .settings(mainClass in assembly := Some("at.happywetter.boinc.WebServer"), test in assembly := {})
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,
    assemblyMergeStrategy in assembly := {
      case PathList("at", "happywetter", "boinc", "shared", xs @ _*) => MergeStrategy.first
      case PathList("at", "happywetter", "boinc", "BuildInfo$.class") => MergeStrategy.first
      case x => (assemblyMergeStrategy in assembly).value(x)
    }
  )

lazy val clientJS = manager.js.dependsOn(shared)
  .settings(mainClass := Some("at.happywetter.boinc.web.Main"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime
    //resourceDirectory in Compile := baseDirectory.value / "resources"
  )