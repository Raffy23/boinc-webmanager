// Use Scala Crossproject for Scala.js (https://github.com/scala-native/sbt-crossproject)
addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % "0.6.28")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "0.6.1")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "0.6.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")
//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.4.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.1")