// Add Scala.JS and JSDependency Plugin
addSbtPlugin("org.scala-js"     % "sbt-scalajs"                     % "1.0.0")
addSbtPlugin("org.scala-js"     % "sbt-jsdependencies"              % "1.0.0")

// Use Scala Crossproject for Scala.js (https://github.com/scala-native/sbt-crossproject)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.0.0")

// Used for annotating the builds with the git version / tag
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"       % "1.0.0")

// Add some helpers for bundling / building the project
// Doesn't work either
//addSbtPlugin("com.typesafe.sbt" % "sbt-web"                 % "1.4.4")
//addSbtPlugin("com.typesafe.sbt" % "sbt-gzip"                % "1.0.2")
// Doesn't work with ECMAScript2015
//addSbtPlugin("com.vmunier"      % "sbt-web-scalajs"         % "1.0.11")
//addSbtPlugin("ch.epfl.scala"    % "sbt-scalajs-bundler"     % "0.17.0")
//addSbtPlugin("ch.epfl.scala"    % "sbt-web-scalajs-bundler" % "0.17.0")

// Replace sbt-assembly with native-packager, can build for more environments
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.6.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")