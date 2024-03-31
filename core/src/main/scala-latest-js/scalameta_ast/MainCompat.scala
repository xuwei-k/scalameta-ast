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
  ): List[(String, Int)] = {
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
    val cursorPos = {
      if (src.isEmpty) {
        Position.Range(input, 0, 0)
      } else if (line >= src.linesIterator.size) {
        Position.Range(input, src.length, src.length)
      } else {
        Position.Range(input, line, column, line, column)
      }
    }.start
    if (false) {
      println(Seq("line" -> line, "column" -> column, "pos" -> cursorPos))
    }

    val t1: List[Tree] = tree.collect {
      case x if (x.pos.start <= cursorPos && cursorPos <= x.pos.end) && ((x.pos.end - x.pos.start) >= 1) =>
        x
    }

    implicit class ListOps[A](xs: List[A]) {
      def minValues[B: Ordering](f: A => B): List[A] = {
        xs.groupBy(f).minBy(_._1)._2
      }
    }

    val t2 = if (t1.size > 1) {
      val ss: List[Tree] = t1.minValues(t => t.pos.end - t.pos.start)
      if (ss.isEmpty) {
        t1
      } else {
        if (ss.size > 1) {
          ss.minValues(_.structure.length) match {
            case Nil => ss
            case aa => aa
          }
        } else {
          ss
        }
      }
    } else {
      t1
    }

    t2.flatMap { cursorTree =>
      val current = cursorTree.structure
      if (false) {
        println("current = " + current)
      }
      val currentSize = current.length
      tree.structure.sliding(currentSize).zipWithIndex.find(_._1 == current).map(_._2).map { pos =>
        if (false) {
          println(Seq("pos" -> pos, "size" -> currentSize))
        }
        (current, pos)
      }
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
