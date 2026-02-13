addSbtPlugin("com.github.xuwei-k" % "sbt-root-aggregate" % "0.1.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.21.0-SNAPSHOT")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
)
