import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.OutputPatterns

val unusedWarnings = Seq(
  "-Ywarn-unused",
)

def Scala213 = "2.13.16"

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
  ),
  scalacOptions ++= unusedWarnings,
  Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings),
)

commonSettings

lazy val commonLatest = Def.settings(
  Compile / sources ++= {
    (((Compile / sourceDirectory).value / "scala-latest") ** "*.scala").get
  }
)

lazy val `scalameta-ast` = projectMatrix
  .in(file("core"))
  .settings(
    name := "scalameta-ast",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest-freespec" % "3.2.19" % Test,
    ),
  )
  .jvmPlatform(
    scalaVersions = Scala213 :: Nil,
    settings = Def.settings(
      metaVersion := (LocalProject("scalameta-ast-latestJS") / metaVersion).value,
      testBuildInfo,
      commonLatest,
      libraryDependencies += "org.scalameta" %%% "scalameta" % "4.13.5",
      libraryDependencies += "org.scalameta" %%% "scalafmt-core" % "3.9.6",
      libraryDependencies += "com.google.inject" % "guice" % "7.0.0" % Test,
      Test / resourceGenerators += Def.task {
        val v1 = (LocalProject("scalameta-ast-latestJS") / metaTreesSource).value
        val v2 = (LocalProject("scalameta-ast-scalafix-compatJS") / metaTreesSource).value
        Seq(v1, v2).zipWithIndex.map { case (src, index) =>
          val f = (Test / resourceManaged).value / s"trees${index}.scala"
          IO.write(f, src)
          f
        }
      },
    )
  )
  .jsPlatform(
    scalaVersions = Scala213 :: Nil,
    axisValues = Seq(metaScalafixCompat),
    settings = Def.settings(
      jsProjectSettings,
      libraryDependencies += {
        ("org.scalameta" %%% "scalameta" % "4.6.0").withSources() // scala-steward:off
      }
    )
  )
  .jsPlatform(
    scalaVersions = Scala213 :: Nil,
    axisValues = Seq(metaLatest),
    settings = Def.settings(
      jsProjectSettings,
      commonLatest,
      libraryDependencies += "org.ekrich" %%% "sconfig" % "1.9.0",
      libraryDependencies += ("com.github.xuwei-k" %%% "scalafmt-core" % "3.9.6-fork-1").withSources(),
    )
  )

lazy val metaVersion = taskKey[String]("")
lazy val metaTreesSource = taskKey[String]("")

lazy val testBuildInfo = {
  Test / sourceGenerators += Def.task {
    val x = "ScalametaASTTestBuildInfo"
    val f = (Test / sourceManaged).value / s"${x}.scala"
    IO.write(
      f,
      s"""|package scalameta_ast
          |
          |object ${x} {
          |  def scalametaVersion: String = "${metaVersion.value}"
          |}
          |""".stripMargin
    )
    f :: Nil
  }
}

lazy val jsProjectSettings: Def.SettingsDefinition = Def.settings(
  scalaJSLinkerConfig ~= {
    _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2018))
  },
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  genBuildInfo := {
    val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
    s"""export default {
       |  "gitHash" : "$hash",
       |  "scalametaVersion" : "${metaVersion.value}"
       |}
       |""".stripMargin
  },
  Seq(Compile, Test).map { x =>
    x / unmanagedSourceDirectories += {
      (x / scalaSource).value / ".." / "js"
    }
  },
  libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.3.0",
  metaTreesSource := {
    val v = metaVersion.value
    val s = scalaBinaryVersion.value
    val p = s"https/repo1.maven.org/maven2/org/scalameta/trees_sjs1_${s}/${v}/trees_sjs1_${s}-${v}-sources.jar"
    val sourceJar = csrCacheDirectory.value / p
    IO.withTemporaryDirectory { dir =>
      val file :: Nil = IO.unzip(sourceJar, dir, _ == "scala/meta/Trees.scala").toList
      IO.read(file)
    }
  },
  testBuildInfo,
  metaVersion := {
    val s = scalaBinaryVersion.value
    val x1 = s"https/repo1.maven.org/maven2/org/scalameta/parsers_sjs1_${s}/"
    val x2 = s"/parsers_sjs1_${s}-"
    val Seq(jarPath) = (Compile / externalDependencyClasspath).value
      .map(_.data.getAbsolutePath)
      .filter(path => path.contains(x1) && path.contains(x2))
    jarPath.split(x1).last.split(x2).head
  },
  Test / scalaJSLinkerConfig ~= {
    _.withModuleKind(ModuleKind.ESModule).withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs"))
  },
  scalacOptions += {
    val a = (LocalRootProject / baseDirectory).value.toURI.toString
    val g = "https://raw.githubusercontent.com/xuwei-k/scalameta-ast/" + sys.process
      .Process("git rev-parse HEAD")
      .lineStream_!
      .head
    s"-P:scalajs:mapSourceURI:$a->$g/"
  },
)

val genBuildInfo = taskKey[String]("")

lazy val localServer = project.settings(
  commonSettings,
  run / fork := true,
  run / baseDirectory := (LocalRootProject / baseDirectory).value,
  Test / testOptions += Tests.Argument("-oDF"),
  Test / test := (Test / test).dependsOn(LocalRootProject / copyFilesFull).value,
  Test / testOptions ++= {
    if (scala.util.Properties.isMac) {
      Nil
    } else {
      Seq(
        Tests.Exclude(
          Seq(
            "scalameta_ast.IntegrationTestWebkit",
          )
        )
      )
    }
  },
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % "2.0.17" % Runtime,
    "ws.unfiltered" %% "unfiltered-filter" % "0.12.1",
    "ws.unfiltered" %% "unfiltered-jetty" % "0.12.1",
    "org.scalatest" %%% "scalatest-freespec" % "3.2.19" % Test,
    "com.microsoft.playwright" % "playwright" % "1.52.0" % Test,
  )
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
    IO.write(d.value / "build_info.js", (p / genBuildInfo).value)
    IO.copyFile(f, d.value / m.jsFileName)
    IO.copyFile(srcMap, d.value / srcMap.getName)
  }
}

val copyFilesFull = taskKey[Unit]("")

TaskKey[Unit]("copyFilesFast") := {
  cp(metaScalafixCompat, scalafixCompatOutJSDir, fastLinkJS, fastLinkJSOutput).value
  cp(metaLatest, latestOutJSDir, fastLinkJS, fastLinkJSOutput).value
}

copyFilesFull := {
  cp(metaScalafixCompat, scalafixCompatOutJSDir, fullLinkJS, fullLinkJSOutput).value
  cp(metaLatest, latestOutJSDir, fullLinkJS, fullLinkJSOutput).value
}

publish := {}
publishLocal := {}
