import java.io.{FileInputStream, FileOutputStream}
import java.util.zip.GZIPOutputStream
import scala.io.Source

enablePlugins(GitVersioning)

name := "Boinc-Webmanager"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
)

git.gitTagToVersionNumber := { tag: String =>
  if(tag matches "[0-9]+\\..*") Some(tag)
  else None
}

val http4sVersion  = "1.0.0-M30"
val uPickleVersion = "1.4.4"

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

lazy val gzip = taskKey[Unit]("GZip managed resources")

lazy val serverJVM = (project in file ("jvm"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .disablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJVM, cssRenderer)
  .settings(
    name := "Boinc-Webmanager_server",
    Compile / mainClass := Some("at.happywetter.boinc.WebServer"),

    buildInfoKeys    := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    (Compile / packageBin) := ((Compile / packageBin) dependsOn gzip).value,
    Compile / packageBin / mappings ++= Seq(
      ((Compile / resourceManaged).value / "web-root" / "boinc-webmanager_client-jsdeps.min.js")    -> "web-root/boinc-webmanager_client-jsdeps.min.js",
      ((Compile / resourceManaged).value / "web-root" / "boinc-webmanager_client-opt.js")           -> "web-root/boinc-webmanager_client-opt.js",
      ((Compile / resourceManaged).value / "web-root" / "boinc-webmanager_client-jsdeps.min.js.gz") -> "web-root/boinc-webmanager_client-jsdeps.min.js.gz",
      ((Compile / resourceManaged).value / "web-root" / "boinc-webmanager_client-opt.js.gz")        -> "web-root/boinc-webmanager_client-opt.js.gz",
      ((Compile / resourceManaged).value / "web-root" / "boinc-webmanager_client-opt.js.map")       -> "web-root/boinc-webmanager_client-opt.js.map",
      ((Compile / resourceManaged).value / "web-root" / "boinc-webmanager_client-opt.js.map.gz")    -> "web-root/boinc-webmanager_client-opt.js.map.gz",
    ),

    libraryDependencies ++= Seq(
      "ch.qos.logback"         %  "logback-classic"     % "1.2.10",
      "ch.qos.logback"         %  "logback-core"        % "1.2.10",
      "org.typelevel"          %% "log4cats-slf4j"      % "2.1.1",

      "org.http4s"             %% "http4s-dsl"          % http4sVersion,
      "org.http4s"             %% "http4s-blaze-server" % http4sVersion,

      "com.github.pureconfig"  %% "pureconfig"          % "0.17.1",

      "org.scala-lang.modules" %% "scala-xml"           % "2.0.1",
      "com.lihaoyi"            %% "scalatags"           % "0.11.0",
      "org.scalaj"             %% "scalaj-http"         % "2.4.2",

      "org.webjars"            %  "swagger-ui"          % "4.1.3",

      "org.jsoup"              %  "jsoup"               % "1.14.3",
      "com.auth0"              %  "java-jwt"            % "3.18.3",

      "com.h2database"         %  "h2"                  % "2.1.210",
      "io.getquill"            %% "quill-jdbc"          % "3.12.0",

      "com.lihaoyi"            %% "upack"               % uPickleVersion,
      "com.lihaoyi"            %% "upickle"             % uPickleVersion,



      // Resources for the client:
      "org.webjars"            %  "font-awesome"        % "5.15.4",
      "org.webjars.bower"      %  "nprogress"           % "0.2.0",
      "org.webjars.npm"        %  "flag-icon-css"       % "3.5.0",
      // "org.webjars.npm"        %  "purecss"             % "2.0.3"
    ),

    gzip := {
      val logger = sLog.value

      Seq(
        "boinc-webmanager_client-jsdeps.min.js",
        "boinc-webmanager_client-opt.js",
        "boinc-webmanager_client-opt.js.map"
      ).foreach { file =>
        logger.info(s"gzipping managed resource: ${(Compile / resourceManaged).value / "web-root" / file}")

        val in  = new FileInputStream((Compile / resourceManaged).value / "web-root" / file)
        val out = new FileOutputStream((Compile / resourceManaged).value / "web-root" / (file + ".gz"))
        val gzipOut = new GZIPOutputStream(out, 4096)

        val buffer = new Array[Byte](4096)
        Iterator
          .continually(in.read(buffer))
          .takeWhile(_ != -1)
          .foreach(read => gzipOut.write(buffer,0,read))

        gzipOut.finish()
        gzipOut.close()
        out.close()
        in.close()
      }
    }
  )

lazy val clientJS = (project in file ("js"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JSDependenciesPlugin)
  .dependsOn(sharedJS, clientCssJS)
  .settings(
    name := "Boinc-Webmanager_client",
    Compile / mainClass := Some("at.happywetter.boinc.web.Main"),

    buildInfoKeys    := Seq[BuildInfoKey](version, scalaVersion, sbtVersion, git.gitCurrentBranch),
    buildInfoPackage := "at.happywetter.boinc",
    buildInfoOptions += BuildInfoOption.BuildTime,

    // Publish fullOpt + dependencies directly to managed resource directory of the server
    Compile / fullOptJS / crossTarget                     := (serverJVM / Compile / resourceManaged).value / "web-root",
    Compile / packageMinifiedJSDependencies / crossTarget := (serverJVM / Compile / resourceManaged).value / "web-root",

    libraryDependencies ++= Seq(
      "org.scala-js"      %%% "scalajs-dom"       % "1.2.0",
      "io.github.cquiroz" %%% "scala-java-time"   % "2.3.0",

      "com.lihaoyi"  %%% "upack"             % uPickleVersion,
      "com.lihaoyi"  %%% "upickle"           % uPickleVersion,

      "in.nvilla"    %%% "monadic-html"      % "0.4.1",
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
      "com.github.japgolly.scalacss" %% "core" % "0.7.0",
    ),
  )