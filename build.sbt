import org.scalajs.linker.interface.Report

val unusedWarnings = Seq(
  "-Ywarn-unused",
)

def Scala213 = "2.13.10"

val metaScalafixCompat = MetaCross("-scalafix-compat", "-scalafix_compat")
val metaLatest = MetaCross("-latest", "-latest")

lazy val commonSettings = Def.settings(
  scalaVersion := Scala213,
  licenses := Seq("MIT License" -> url("https://opensource.org/licenses/mit-license")),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-language:existentials",
    "-language:higherKinds",
  ),
  scalacOptions ++= unusedWarnings,
  Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings),
)

commonSettings

run / fork := true

libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.7" % Runtime

lazy val `scalameta-ast` = projectMatrix
  .in(file("core"))
  .settings(
    name := "scalameta-ast",
    commonSettings,
    Test / resourceGenerators += Def.task {
      val v1 = (LocalProject("scalameta-ast-latestJS") / metaVersion).value
      val v2 = (LocalProject("scalameta-ast-scalafix-compatJS") / metaVersion).value
      Seq(v1, v2).zipWithIndex.map { case (v, index) =>
        val treeURL =
          s"https://raw.githubusercontent.com/scalameta/scalameta/v${v}/scalameta/trees/shared/src/main/scala/scala/meta/Trees.scala"
        val src = scala.io.Source.fromURL(treeURL, "UTF-8").getLines().toList
        val f = (Test / resourceManaged).value / s"trees${index}.scala"
        IO.writeLines(f, src)
        f
      }
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest-freespec" % "3.2.15" % Test, // TODO scala-js test
      "org.ekrich" %%% "sconfig" % "1.5.0",
    ),
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

lazy val metaVersion = taskKey[String]("")

lazy val jsProjectSettings: Def.SettingsDefinition = Def.settings(
  scalaJSLinkerConfig ~= {
    _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2018))
  },
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  Compile / unmanagedSourceDirectories += {
    (Compile / scalaSource).value / ".." / "js"
  },
  libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.1.9",
  metaVersion := {
    val x1 = "https/repo1.maven.org/maven2/org/scalameta/parsers_sjs1_2.13/"
    val x2 = "/parsers_sjs1_2.13-"
    val Seq(jarPath) = (Compile / externalDependencyClasspath).value
      .map(_.data.getAbsolutePath)
      .filter(path => path.contains(x1) && path.contains(x2))
    jarPath.split(x1).last.split(x2).head
  },
  Compile / sourceGenerators += Def.task {
    val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
    val src =
      s"""package scalameta_ast
         |
         |import scala.scalajs.js.annotation.JSExport
         |import scala.scalajs.js.annotation.JSExportTopLevel
         |
         |@JSExportTopLevel("ScalametaASTBuildInfo")
         |object ScalametaASTBuildInfo {
         |  @JSExport
         |  def gitHash: String = "${hash}"
         |  @JSExport
         |  def scalametaVersion: String = "${metaVersion.value}"
         |}
         |""".stripMargin
    val f = (Compile / resourceManaged).value / "scalameta_ast" / "ScalametaASTBuildInfo.scala"
    IO.write(f, src)
    f :: Nil
  },
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

val srcDir = (LocalRootProject / baseDirectory).apply(_ / "sources")
val scalafixCompatOutJSDir = srcDir(_ / "scalafix-compat")
val latestOutJSDir = srcDir(_ / "latest")

cleanFiles += scalafixCompatOutJSDir.value
cleanFiles += latestOutJSDir.value

def cp(
  c: MetaCross,
  d: Def.Initialize[File],
  k: TaskKey[Attributed[Report]],
  originalOutputDir: TaskKey[File]
): Def.Initialize[Task[Unit]] = Def.taskDyn {
  val Seq(p) = `scalameta-ast`.finder(c, VirtualAxis.js).get
  Def.task {
    val v = (p / Compile / k).value
    val Seq(m) = v.data.publicModules.toSeq
    val src = (p / Compile / originalOutputDir).value
    val f = src / m.jsFileName
    val srcMap = src / m.sourceMapName.getOrElse(sys.error("source map not found"))
    IO.copyFile(f, d.value / m.jsFileName)
    IO.copyFile(srcMap, d.value / srcMap.getName)
  }
}

TaskKey[Unit]("copyFilesFast") := {
  cp(metaScalafixCompat, scalafixCompatOutJSDir, fastLinkJS, fastLinkJSOutput).value
  cp(metaLatest, latestOutJSDir, fastLinkJS, fastLinkJSOutput).value
}

TaskKey[Unit]("copyFilesFull") := {
  cp(metaScalafixCompat, scalafixCompatOutJSDir, fullLinkJS, fullLinkJSOutput).value
  cp(metaLatest, latestOutJSDir, fullLinkJS, fullLinkJSOutput).value
}

publish := {}
publishLocal := {}
