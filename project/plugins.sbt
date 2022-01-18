// Add Scala.JS and JSDependency Plugin
addSbtPlugin("org.scala-js"     % "sbt-scalajs"                     % "1.8.0")
addSbtPlugin("org.scala-js"     % "sbt-jsdependencies"              % "1.0.2")

// Use Scala Crossproject for Scala.js (https://github.com/scala-native/sbt-crossproject)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.1.0")

// Used for annotating the builds with the git version / tag
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"       % "1.0.2")

// Replace sbt-assembly with native-packager, can build for more environments
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")

//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")
addDependencyTreePlugin

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.1")