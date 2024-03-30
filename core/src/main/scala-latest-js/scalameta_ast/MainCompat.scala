package scalameta_ast

import metaconfig.Conf
import org.ekrich.config.Config
import org.ekrich.config.ConfigFactory
import org.ekrich.config.ConfigRenderOptions
import org.scalafmt.config.ScalafmtConfig
import scala.annotation.nowarn
import scala.meta.common.Convert
import scala.meta.contrib.XtensionTreeOps
import scala.meta.parsers.Parse
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.util.control.NonFatal

trait MainCompat {

  @JSExport
  def rawWithPos(
    src: String,
    dialect: String,
    scalafmtConfig: String,
    line: Int,
    column: Int,
  ) = {
    import scala.meta._
    val convert = implicitly[Convert[String, Input]]
    val main = new ScalametaAST
    val dialects = {
      main.stringToDialects.getOrElse(
        dialect, {
          Console.err.println(s"invalid dialct ${dialect}")
          main.dialectsDefault
        }
      )
    }

    val input = convert.apply(src)
    val tree: Tree = main.loopParse(
      input,
      for {
        x1 <- main.parsers
        x2 <- dialects
      } yield (x1, x2)
    )
    val res: String = runFormat(
      source = tree.structure,
      scalafmtConfig = hoconToMetaConfig(scalafmtConfig)
    ).result
    val parsed: Term = implicitly[Parse[Term]].apply(Input.String(res), scala.meta.dialects.Scala3).get
    val tokens = parsed.tokens
    val cursorPos = {
      if (src.isEmpty) {
        Position.Range(input, 0, 0)
      } else if (line >= src.linesIterator.size) {
        Position.Range(input, src.length, src.length)
      } else {
        Position.Range(input, line - 1, column, line, column)
      }
    }
    println(Seq("line" -> line, "column" -> column, "pos" -> cursorPos))
    tree.collect {
      case x if cursorPos.start <= x.pos.start =>
        x
    }.groupBy(_.pos.start).minByOption(_._1).map(_._2.minBy(_.pos.end).structure).map { current =>
      println("current = " + current)
      val currentSize = current.length
      val count = tree.structure.sliding(currentSize).count(_ == current)
      println(Seq("count" -> count, "size" -> currentSize))
    }
  }

  def runFormat(source: String, scalafmtConfig: Conf): Output[String] = {
    ScalametaAST.stopwatch {
      runFormat(
        source = source,
        conf = metaConfigToScalafmtConfig(scalafmtConfig)
      )
    }
  }

  private def runFormat(source: String, conf: ScalafmtConfig): String = {
    org.scalafmt.Scalafmt.format(source, conf).get
  }

  private def metaConfigToScalafmtConfig(conf: Conf): ScalafmtConfig = {
    ScalafmtConfig.decoder.read(None, conf).get
  }

  @JSExport
  @nowarn("msg=never used")
  def format(source: String, scalafmtConfJsonStr: String): js.Object =
    try {
      val res = runFormat(
        source = source,
        scalafmtConfig = hoconToMetaConfig(scalafmtConfJsonStr)
      )
      new js.Object {
        val result: String = res.result
        val time: Double = res.time.toDouble
        val error = null
      }
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        new js.Object {
          val result: String = source
          val error = e.toString()
        }
    }

  private[this] def hoconToMetaConfig(config: String): Conf =
    convertSConfigToMetaConfig(ConfigFactory.parseString(config))

  private[this] def convertSConfigToMetaConfig(config: Config): Conf =
    convertToMetaConfig(JSON.parse(config.root.render(ConfigRenderOptions.concise)))

  private[this] def convertToMetaConfig(input: Any): Conf = input match {
    case x: String =>
      Conf.Str(x)
    case x: Double =>
      Conf.Num(x)
    case true =>
      Conf.Bool(true)
    case false =>
      Conf.Bool(false)
    case x: js.Array[?] =>
      Conf.Lst(x.map(a => convertToMetaConfig(a)).toList)
    case x: js.Object =>
      Conf.Obj(
        x.asInstanceOf[js.Dictionary[?]].map { case (k, v) => k -> convertToMetaConfig(v) }.toList
      )
    case _ =>
      Conf.Null()
  }

}
