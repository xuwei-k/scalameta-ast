addSbtPlugin("com.github.xuwei-k" % "sbt-root-aggregate" % "0.1.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.2")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-language:existentials",
  "-language:higherKinds",
  "-Yno-adapted-args",
)
