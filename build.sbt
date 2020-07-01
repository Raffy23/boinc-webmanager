import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoPackage

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

val http4sVersion  = "1.0.0-M3"
val uPickleVersion = "0.9.9"

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
    name := "Boinc-Webmanager_server",
    mainClass := Some("at.happywetter.boinc.WebServer"),

    buildInfoKeys    := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    libraryDependencies ++= Seq(
      "ch.qos.logback"         %  "logback-classic"     % "1.2.3",
      "ch.qos.logback"         %  "logback-core"        % "1.2.3",

      "org.http4s"             %% "http4s-dsl"          % http4sVersion,
      "org.http4s"             %% "http4s-blaze-server" % http4sVersion,

      "com.github.pureconfig"  %% "pureconfig"          % "0.12.2",

      "org.scala-lang.modules" %% "scala-xml"           % "1.2.0",
      "com.lihaoyi"            %% "scalatags"           % "0.8.4",
      "org.scalaj"             %% "scalaj-http"         % "2.4.2",

      "org.webjars"            %  "swagger-ui"          % "3.25.0",

      "org.jsoup"              %  "jsoup"               % "1.12.1",
      "com.auth0"              %  "java-jwt"            % "3.9.0",

      "com.h2database"         %  "h2"                  % "1.4.200",
      "io.getquill"            %% "quill-jdbc-monix"    % "3.5.2",

      "com.lihaoyi"            %% "upack"               % uPickleVersion,
      "com.lihaoyi"            %% "upickle"             % uPickleVersion,
    )
  )

lazy val clientJS = (project in file ("js"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JSDependenciesPlugin)
  .dependsOn(sharedJS, clientCssJS)
  .settings(
    name := "Boinc-Webmanager_client",
    mainClass := Some("at.happywetter.boinc.web.Main"),

    buildInfoKeys    := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom"       % "1.0.0",
      "org.scala-js" %%% "scalajs-java-time" % "1.0.0",

      "com.lihaoyi"  %%% "upack"             % uPickleVersion,
      "com.lihaoyi"  %%% "upickle"           % uPickleVersion,

      // Note: local published artifact build against ScalaJS 1.0.0
      "in.nvilla"    %%% "monadic-html"      % "0.4.1-SNAPSHOT",
    ),

    resolvers += "WebJars-BinTray" at "https://dl.bintray.com/webjars/maven",
    jsDependencies ++= Seq(
      "org.webjars.bower" % "navigo"    % "7.0.0" / "navigo.js"    commonJSName "Navigo" minified "navigo.min.js",
      "org.webjars.bower" % "nprogress" % "0.2.0" / "nprogress.js" commonJSName "NProgress",
      "org.webjars.bower" % "chart.js"  % "2.8.0" / "Chart.js"     commonJSName "ChartJS" minified "Chart.min.js",

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