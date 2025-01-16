addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.18.1")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-language:existentials",
  "-language:higherKinds",
  "-Yno-adapted-args",
)
