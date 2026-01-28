addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.21.0-SNAPSHOT")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-language:existentials",
)
