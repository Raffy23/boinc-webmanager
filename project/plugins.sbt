// Add Scala.JS and JSDependency Plugin
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
//addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.2.0")
//addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

// Use Scala Crossproject for Scala.js (https://github.com/scala-native/sbt-crossproject)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")

// Used for annotating the builds with the git version / tag
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")

// Replace sbt-assembly with native-packager, can build for more environments
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")
addDependencyTreePlugin

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.6")
