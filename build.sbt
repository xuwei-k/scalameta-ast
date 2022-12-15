val unusedWarnings = Seq(
  "-Ywarn-unused",
)

def scalametaVersion = "4.6.0" // scala-steward:off

lazy val `scalameta-ast` = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    name := "scalameta-ast",
    scalaVersion := "2.13.10",
    licenses := Seq("MIT License" -> url("https://opensource.org/licenses/mit-license")),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-Xlint",
      "-language:existentials",
      "-language:higherKinds",
    ),
    scalacOptions ++= unusedWarnings,
    watchSources += (LocalRootProject / baseDirectory).value / "template.html",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "parsers" % scalametaVersion,
    ),
    Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings)
  )
  .jvmSettings(
    libraryDependencies += "org.scalameta" %%% "scalafmt-core" % "3.6.1",
  )
  .jsSettings(
    scalaJSLinkerConfig ~= { _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2018)) },
    libraryDependencies += "com.github.xuwei-k" %%% "scalafmt-core" % "3.6.1-fork-1",
    scalacOptions += {
      val a = (LocalRootProject / baseDirectory).value.toURI.toString
      val g = "https://raw.githubusercontent.com/xuwei-k/scalameta-ast/" + sys.process
        .Process("git rev-parse HEAD")
        .lineStream_!
        .head
      s"-P:scalajs:mapSourceURI:$a->$g/"
    }
  )

val genHtmlLocal = TaskKey[Unit]("genHtmlLocal")

genHtmlLocal := {
  val js = "./js/target/scala-2.13/scalameta-ast-fastopt.js"
  val html = gen(js = js, hash = "main")
  IO.write(file("index.html"), html)
}

TaskKey[Unit]("genAndCheckHtml") := {
  genHtmlLocal.value
  val diff = sys.process.Process("git diff").!!
  if (diff.nonEmpty) {
    sys.error("Working directory is dirty!\n" + diff)
  }
}

TaskKey[Unit]("genHtmlPublish") := {
  val js = "./scalameta-ast.js"
  val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
  val html = gen(js = js, hash = hash)
  IO.write(file("index.html"), html)
}

def gen(js: String, hash: String): String = {
  IO.read(file("template.html"))
    .replace("SCALA_META_AST_JAVASCRIPT_URL", js)
    .replace("SCALA_META_AST_GIT_HASH", hash)
    .replace("SCALA_META_VERSION", scalametaVersion)
}

publish := {}
publishLocal := {}
