addSbtPlugin("com.github.xuwei-k" % "sbt-root-aggregate" % "0.1.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-language:existentials",
)
