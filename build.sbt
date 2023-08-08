import java.io.{FileInputStream, FileOutputStream}
import java.util.zip.GZIPOutputStream

enablePlugins(GitVersioning)

name := "Boinc-Webmanager"

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-explain"
)

git.gitTagToVersionNumber := { tag: String =>
  if (tag.matches("[0-9]+\\..*")) Some(tag)
  else None
}

val http4sVersion = "1.0.0-M39"
val uPickleVersion = "3.0.0"
val doobieVersion = "1.0.0-RC2"

lazy val root = project
  .in(file("."))
  .aggregate(clientJS, serverJVM)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % uPickleVersion
    )
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

// lazy val gzip = taskKey[Unit]("GZip managed resources")

lazy val serverJVM = (project in file("jvm"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging)
  // .enablePlugins(JDKPackagerPlugin)
  // .enablePlugins(SbtWeb)
  // .enablePlugins(WebScalaJSBundlerPlugin)
  .disablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJVM, cssRenderer)
  .settings(
    name := "Boinc-Webmanager_server",

    // Build info:
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    // Compile
    Compile / mainClass := Some("at.happywetter.boinc.WebServer"),

    // triggers scalaJSPipeline when using compile or continuous compilation
    // Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
    // scalaJSProjects := Seq(clientJS),
    // pipelineStages := Seq(gzip),
    // Assets / pipelineStages := Seq(scalaJSPipeline),
    // Assets / WebKeys.packagePrefix := "public/",
    // Runtime / managedClasspath += (Assets / packageBin).value,

    // Gzip options:
    // gzip / includeFilter := "*.html" ||  "*.css" || "*.js",

    // Testing options:
    Test / logBuffered := false,

    // javaAgents  += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.26.0",
    // javaOptions ++= Seq(
    //  "-Dotel.java.global-autoconfigure.enabled=true",
    //  "-Dotel.service.name=boinc-webmanager",
    //  "-Dotel.metrics.exporter=none",
    // ),

    // Include the client web resources in universal bundle:
    Compile / packageBin / mappings ++= (clientJS / Compile / fullOptJS / webpack).value.map { f =>
      f.data -> s"public/${f.data.getName()}"
    },
    // Also package related files: (somehow scaljs-bundler doesn't detect them ...)
    Compile / packageBin / mappings ++= Seq(
      (clientJS / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler" / "main" / "boinc-webmanager_client-opt-bundle.js.gz" -> s"public/boinc-webmanager_client-opt-bundle.js.gz",
      (clientJS / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler" / "main" / "boinc-webmanager_client-opt-bundle.js.br" -> s"public/boinc-webmanager_client-opt-bundle.js.br",
      (clientJS / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler" / "main" / "boinc-webmanager_client-opt-bundle.js.LICENSE.txt" -> s"public/boinc-webmanager_client-opt-bundle.js.LICENSE.txt",
      (clientJS / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler" / "main" / "boinc-webmanager_client-opt-bundle.js.map" -> s"public/boinc-webmanager_client-opt-bundle.js.map",
      (clientJS / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler" / "main" / "boinc-webmanager_client-opt-bundle.css.gz" -> s"public/boinc-webmanager_client-opt-bundle.css.gz",
      (clientJS / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler" / "main" / "boinc-webmanager_client-opt-bundle.css.br" -> s"public/boinc-webmanager_client-opt-bundle.css.br"
    ),

    // Dependencies
    libraryDependencies ++= Seq(
      // Logging:
      "ch.qos.logback" % "logback-classic" % "1.4.6",
      "ch.qos.logback" % "logback-core" % "1.4.6",
      "org.typelevel" %% "log4cats-slf4j" % "2.5.0",

      // http4s & web server stuff
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "com.auth0" % "java-jwt" % "4.3.0",
      "org.webjars" % "swagger-ui" % "4.18.1",
      "co.fs2" %% "fs2-io" % "3.8.0",

      // for server side rendering:
      "com.lihaoyi" %% "scalatags" % "0.12.0",

      // config file:
      "com.github.pureconfig" %% "pureconfig-cats" % "0.17.4",

      // for boinc client:
      "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
      "org.jsoup" % "jsoup" % "1.15.4",

      // DB
      "com.h2database" % "h2" % "2.1.214",
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-h2" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "com.lihaoyi" %% "upack" % uPickleVersion,
      "com.lihaoyi" %% "upickle" % uPickleVersion,

      // Otel & tracing stuff
      "org.typelevel" %% "otel4s-java" % "0.2.1",
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.26.0" % Runtime,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.26.0-alpha" % Runtime,
      "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.26.0" % Runtime,

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.16" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
    )
  )

lazy val clientJS = (project in file("js"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(ScalaJSBundlerPlugin)
  // .disablePlugins(ScalaJSWeb)
  .dependsOn(sharedJS, clientCssJS)
  .settings(
    name := "Boinc-Webmanager_client",
    Compile / mainClass := Some("at.happywetter.boinc.web.Main"),
    scalaJSUseMainModuleInitializer := true,

    // BuildInfo stuff:
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    // Custom webpack options:
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    webpack / version := "5.86.0",

    // Dependencies:
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.6.0",
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0",
      "com.lihaoyi" %%% "upack" % uPickleVersion,
      "com.lihaoyi" %%% "upickle" % uPickleVersion,
      "in.nvilla" %%% "monadic-html" % "0.5.0-RC1", // <-- TODO: Delete once app has been ported to calico
      "com.raquo" %%% "laminar" % "16.0.0",
      "com.raquo" %%% "airstream" % "16.0.0",
      "io.frontroute" %%% "frontroute" % "0.18.1"
    ),
    Compile / npmDependencies ++= Seq(
      "navigo" -> "7.0.0",
      "nprogress" -> "0.2.0",
      "chart.js" -> "2.9.4",
      "@fortawesome/fontawesome-free" -> "6.4.0", // <-- TODO: Replace with "feather-icons" -> "4.29.0"
      "feather-icons" -> "4.29.0"
    ),
    Compile / npmDevDependencies ++= Seq(
      "@tailwindcss/aspect-ratio" -> "0.4.2",
      "@tailwindcss/forms" -> "0.5.4",
      "@tailwindcss/typography" -> "0.5.9",
      "tailwindcss" -> "3.3.3",
      // PostCSS plugins:
      "autoprefixer" -> "10.4.14",
      "cssnano" -> "6.0.1",
      // Webpack plugins:
      "css-loader" -> "6.8.1",
      "compression-webpack-plugin" -> "10.0.0",
      "style-loader" -> "3.3.3",
      "mini-css-extract-plugin" -> "2.7.6",
      "postcss" -> "8.4.27",
      "postcss-import" -> "15.1.0",
      "postcss-loader" -> "7.3.3",
      "postcss-nested" -> "6.0.1"
    )
  )

lazy val clientCSS = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("css"))
  .jsConfigure { project =>
    project.enablePlugins(ScalaJSBundlerPlugin)
  }
lazy val clientCssJVM = clientCSS.jvm
lazy val clientCssJS = clientCSS.js

lazy val cssRenderer = (project in file("css-renderer"))
  .dependsOn(clientCssJVM)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalacss" %% "core" % "1.0.0"
    )
  )
