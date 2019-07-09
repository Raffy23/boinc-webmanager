import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoPackage

enablePlugins(GitVersioning)

name := "Boinc-Webmanager"

scalaVersion in ThisBuild := "2.12.8"
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
val http4sVersion = "0.21.0-M1"
val circeVersion = "0.12.0-M4"
val uPickleVersion = "0.7.5"


lazy val root = project.in(file(".")).
  aggregate(clientJS, serverJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val shared = (project in file("shared"))
  .enablePlugins(ScalaJSPlugin)
  .disablePlugins(AssemblyPlugin)
  .settings(
    aggregate in assembly := false,

    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % uPickleVersion,
    )
  )

lazy val serverJVM = (project in file ("jvm"))
  .enablePlugins(BuildInfoPlugin)
  .settings(mainClass in assembly := Some("at.happywetter.boinc.WebServer"), test in assembly := {})
  .dependsOn(shared)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,
    assemblyMergeStrategy in assembly := {
      case PathList("at", "happywetter", "boinc", "shared", xs @ _*) => MergeStrategy.first
      case PathList("at", "happywetter", "boinc", "BuildInfo$.class") => MergeStrategy.first
      case x => (assemblyMergeStrategy in assembly).value(x)
    },

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "org.http4s" %% "http4s-dsl" % http4sVersion,
      //"org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,

      "com.github.pureconfig" %% "pureconfig" % "0.11.1",

      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",

      "com.auth0" % "java-jwt" % "3.8.1",

      "com.lihaoyi" %% "scalatags" % "0.7.0",

      "org.scalaj" %% "scalaj-http" % "2.4.2",

      "org.jsoup" % "jsoup" % "1.12.1",

      "com.lihaoyi" %%% "upack" % uPickleVersion,
      "com.lihaoyi" %%% "upickle" % uPickleVersion,
      //"io.circe" %% "circe-core" % circeVersion,
      //"io.circe" %% "circe-generic" % circeVersion,
      //"io.circe" %% "circe-parser" % circeVersion
    )
  )

lazy val clientJS = (project in file ("js"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(mainClass := Some("at.happywetter.boinc.web.Main"))
  .dependsOn(shared)
  .settings(
    name := "Boinc-Webmanager",
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",

    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,
    //resourceDirectory in Compile := baseDirectory.value / "resources"

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.7",
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC2",

      "com.github.japgolly.scalacss" %%% "core" % "0.5.6",

      "com.lihaoyi" %%% "upack" % uPickleVersion,
      "com.lihaoyi" %%% "upickle" % uPickleVersion,
      
      //"io.circe" %%% "circe-core" % circeVersion,
      //"io.circe" %%% "circe-generic" % circeVersion,
      //"io.circe" %%% "circe-parser" % circeVersion ,

      //"com.timushev" %%% "scalatags-rx" % "0.4.0",
      //"com.lihaoyi" %%% "scalatags" % "0.7.0",

      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1",
    ),
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