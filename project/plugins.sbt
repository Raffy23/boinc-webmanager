// Use Scala Crossproject for Scala.js (https://github.com/scala-native/sbt-crossproject)
addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % "0.6.19")
addSbtPlugin("org.scala-native" % "sbt-crossproject"         % "0.2.2")  // (1)
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.2")  // (2)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

