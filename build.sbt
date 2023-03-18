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
      libraryDependencies += "com.github.xuwei-k" %%% "scalafmt-core" % "3.6.1-fork-1", // scala-steward:off
    )
  )
  .jsPlatform(
    scalaVersions = Scala213 :: Nil,
    axisValues = Seq(metaLatest),
    settings = Def.settings(
      jsProjectSettings,
      libraryDependencies += "com.github.xuwei-k" %%% "scalafmt-core" % "3.7.2-fork-1",
    )
  )

lazy val jsProjectSettings: Def.SettingsDefinition = Def.settings(
  scalaJSLinkerConfig ~= {
    _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2018))
  },
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  Compile / unmanagedSourceDirectories += {
    (Compile / scalaSource).value / ".." / "js"
  },
  libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.1.9",
  Test / test := {},
  scalacOptions += {
    val a = (LocalRootProject / baseDirectory).value.toURI.toString
    val g = "https://raw.githubusercontent.com/xuwei-k/scalameta-ast/" + sys.process
      .Process("git rev-parse HEAD")
      .lineStream_!
      .head
    s"-P:scalajs:mapSourceURI:$a->$g/"
  },
)

libraryDependencies ++= Seq(
  "ws.unfiltered" %% "unfiltered-filter" % "0.12.0",
  "ws.unfiltered" %% "unfiltered-jetty" % "0.12.0",
)

val srcDir = file("sources")
def cp(c: MetaCross, d: String, k: TaskKey[Attributed[File]]): Def.Initialize[Task[Unit]] = Def.taskDyn {
  val Seq(p) = `scalameta-ast`.finder(c, VirtualAxis.js).get
  Def.task {
    val v = (p / Compile / k).value
    val f = v.data
    val Some(srcMap) = v.get(scalaJSSourceMap)
    IO.copyFile(f, srcDir / d / f.getName)
    IO.copyFile(srcMap, srcDir / d / srcMap.getName)
  }
}

TaskKey[Unit]("copyFilesFast") := {
  cp(metaScalafixCompat, "scalafix-compat", fastOptJS).value
  cp(metaLatest, "latest", fastOptJS).value
}

TaskKey[Unit]("copyFilesFull") := {
  cp(metaScalafixCompat, "scalafix-compat", fullOptJS).value
  cp(metaLatest, "latest", fullOptJS).value
  val mainJS = srcDir / "main.js"
  val out = IO.read(mainJS).replace("scalameta-ast-fastopt.js", "scalameta-ast-opt.js")
  IO.write(mainJS, out)
}

publish := {}
publishLocal := {}
