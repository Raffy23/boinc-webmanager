// Use Scala Crossproject for Scala.js (https://github.com/scala-native/sbt-crossproject)
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.7.0")

addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % "0.6.18")
addSbtPlugin("org.scala-native" % "sbt-crossproject"         % "0.2.0")  // (1)
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.0")  // (2)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

