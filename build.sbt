// (5) shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoPackage
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtcrossproject.CrossType

enablePlugins(GitVersioning)

name := "Boinc-Webmanager"

scalaVersion in ThisBuild := "2.13.1"
scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
)

git.gitTagToVersionNumber := { tag: String =>
  if(tag matches "[0-9]+\\..*") Some(tag)
  else None
}

//scalaJSUseMainModuleInitializer := true
val http4sVersion  = "0.21.0-RC2"
val circeVersion   = "0.12.3" // unused
val uPickleVersion = "0.9.8"

lazy val root = project.in(file(".")).
  aggregate(clientJS, serverJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % uPickleVersion,
    )
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS  = shared.js

lazy val serverJVM = (project in file ("jvm"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .disablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJVM, cssRenderer)
  .settings(
    name := "Boinc-Webmanager (Server)",
    mainClass := Some("at.happywetter.boinc.WebServer"),

    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    //test in assembly := {},
    //assemblyMergeStrategy in assembly := {
      //case PathList("ch", "qos", "logback", "core", xs @ _*) => println(xs); MergeStrategy.first
      //case PathList("at", "happywetter", "boinc", "shared", xs @ _*) => MergeStrategy.first
      //case PathList("at", "happywetter", "boinc", "BuildInfo$.class") => MergeStrategy.first
      //case x => (assemblyMergeStrategy in assembly).value(x)
    //},

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3",

      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,

      "com.github.pureconfig" %% "pureconfig" % "0.12.2",

      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",

      "com.auth0" % "java-jwt" % "3.9.0",

      "com.lihaoyi" %% "scalatags" % "0.8.4",

      "org.scalaj" %% "scalaj-http" % "2.4.2",

      "org.jsoup" % "jsoup" % "1.12.1",

      "com.lihaoyi" %%% "upack" % uPickleVersion,
      "com.lihaoyi" %%% "upickle" % uPickleVersion,
    )
  )

lazy val clientJS = (project in file ("js"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(mainClass := Some("at.happywetter.boinc.web.Main"))
  .dependsOn(sharedJS, clientCssJS)
  .settings(
    name := "Boinc-Webmanager (Client)",
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",

    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,
    //resourceDirectory in Compile := baseDirectory.value / "resources"

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.8",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC3",

      "com.lihaoyi" %%% "upack" % uPickleVersion,
      "com.lihaoyi" %%% "upickle" % uPickleVersion,

      "in.nvilla" %%% "monadic-html" % "0.4.0",
    ),

    // Additional Javascript dependencies
    resolvers += "WebJars-BinTray" at "https://dl.bintray.com/webjars/maven",
    jsDependencies ++= Seq(
      "org.webjars.bower" % "navigo" % "7.0.0" / "navigo.js" commonJSName "Navigo" minified "navigo.min.js",
      "org.webjars.bower" % "nprogress" % "0.2.0" / "nprogress.js" commonJSName "NProgress",
      "org.webjars.bower" % "chart.js" % "2.8.0" / "Chart.js" commonJSName "ChartJS" minified "Chart.min.js",

      // Polyfill Dependencies needed for IE / Edge to be able to run it
      "org.webjars.npm" % "text-encoding" % "0.6.4" / "encoding.js",
      ProvidedJS / "polyfill-nodelist.js"
    )
  )

lazy val clientCSS = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("css"))
lazy val clientCssJVM = clientCSS.jvm
lazy val clientCssJS  = clientCSS.js

lazy val cssRenderer = (project in file("css-renderer"))
  .dependsOn(clientCssJVM)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalacss" %% "core" % "0.6.0",
    ),
  )