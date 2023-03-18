val unusedWarnings = Seq(
  "-Ywarn-unused",
)

def scalametaVersion = "4.6.0" // scala-steward:off

def Scala213 = "2.13.10"

val metaScalafixCompat = MetaCross("-scalafix-compat", "-scalafix_compat")
val metaLatest = MetaCross("-latest", "-latest")

lazy val `scalameta-ast` = projectMatrix
  .in(file("core"))
  .settings(
    name := "scalameta-ast",
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
    Test / resourceGenerators += task {
      val treeURL =
        s"https://raw.githubusercontent.com/scalameta/scalameta/v${scalametaVersion}/scalameta/trees/shared/src/main/scala/scala/meta/Trees.scala"
      val src = scala.io.Source.fromURL(treeURL, "UTF-8").getLines().toList
      val f = (Test / resourceManaged).value / "trees.scala"
      IO.writeLines(f, src)
      f :: Nil
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest-freespec" % "3.2.15" % Test, // TODO scala-js test
      "org.ekrich" %%% "sconfig" % "1.5.0",
      "org.scalameta" %%% "parsers" % scalametaVersion,
    ),
    Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings)
  )
  .jvmPlatform(
    scalaVersions = Scala213 :: Nil,
    settings = Def.settings(
      libraryDependencies += "org.scalameta" %%% "scalafmt-core" % "3.7.2",
    )
  )
  .jsPlatform(
    scalaVersions = Scala213 :: Nil,
    axisValues = Seq(metaScalafixCompat),
    settings = Def.settings(
      jsProjectSettings,
      libraryDependencies += "org.scalameta" %%% "parsers" % scalametaVersion,
    )
  )
  .jsPlatform(
    scalaVersions = Scala213 :: Nil,
    axisValues = Seq(metaLatest),
    settings = Def.settings(
      jsProjectSettings,
      libraryDependencies += "org.scalameta" %%% "parsers" % "4.7.6",
    )
  )

lazy val jsProjectSettings: Def.SettingsDefinition = Def.settings(
  scalaJSLinkerConfig ~= {
    _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2018))
  },
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  Compile / unmanagedSourceDirectories += {
    `scalameta-ast`.base / "src" / "main" / "js"
  },
  libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.1.9",
  libraryDependencies += "com.github.xuwei-k" %%% "scalafmt-core" % "3.6.1-fork-1",
  scalacOptions += {
    val a = (LocalRootProject / baseDirectory).value.toURI.toString
    val g = "https://raw.githubusercontent.com/xuwei-k/scalameta-ast/" + sys.process
      .Process("git rev-parse HEAD")
      .lineStream_!
      .head
    s"-P:scalajs:mapSourceURI:$a->$g/"
  },
)

val genHtmlLocal = TaskKey[Unit]("genHtmlLocal")

genHtmlLocal := {
  val js = "./core/target/js-2.13/scalameta-ast-fastopt.js"
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
