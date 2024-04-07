package scalameta_ast

import metaconfig.Conf
import org.ekrich.config.Config
import org.ekrich.config.ConfigFactory
import org.ekrich.config.ConfigRenderOptions
import org.scalafmt.config.ScalafmtConfig
import scala.annotation.nowarn
import scala.annotation.tailrec
import scala.meta.common.Convert
import scala.meta.parsers.Parse
import scala.meta.tokens.Token
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.util.control.NonFatal

case class WithPosResult(src: String, cursorValues: List[(String, Int)], tokenMap: List[WrappedToken])

case class Highlighted(prefix: String, current: String, suffix: String)

case class WrappedToken(token: Token, addedSpaceByScalafmt: Boolean) {
  lazy val tokenSize: Int = token.end - token.start
}

trait MainCompat {

  private def escape(html: String): String = {
    html
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#039;")
  }

  @JSExport
  def rawWithPos(
    src: String,
    dialect: String,
    scalafmtConfig: String,
    line: Int,
    column: Int,
  ): js.Object = {
    try {
      val output =
        rawWithPos1(
          src = src,
          dialect = dialect,
          scalafmtConfig = scalafmtConfig,
          line = line,
          column = column
        ) match {
          case Right(x) =>
            List(
              "<span>",
              escape(x.prefix),
              "</span><span style='color: blue;'>",
              escape(x.current),
              "</span><span>",
              escape(x.suffix),
              "</span>"
            ).mkString("")
          case Left(x) =>
            x
        }
      new js.Object {
        val ast: String = output
        val astBuildMs: Double = 0.0 // TODO
      }
    } catch {
      case e: Throwable =>
        new js.Object {
          val error = e
          val errorString: String = e.toString
        }
    }
  }

  def rawWithPos1(
    src: String,
    dialect: String,
    scalafmtConfig: String,
    line: Int,
    column: Int,
  ): Either[String, Highlighted] = {
    val result = rawWithPos0(
      src = src,
      dialect = dialect,
      scalafmtConfig = scalafmtConfig,
      line = line,
      column = column
    )

    result.cursorValues match {
      case List((s, pos)) =>
        @tailrec
        def loop(n: Int, list: List[WrappedToken], acc: Int): Int = {
          list match {
            case x :: xs =>
              if (n <= 0) {
                acc
              } else {
                loop(if (x.addedSpaceByScalafmt) n else n - x.tokenSize, xs, acc + x.tokenSize)
              }
            case _ =>
              sys.error(s"error ${n} ${acc}")
          }
        }

        val newStartPos = loop(pos, result.tokenMap, 0)
        val currentSizeWithSpace = loop(s.length, result.tokenMap.dropWhile(_.token.end < newStartPos), 0)

        Right(
          Highlighted(
            result.src.take(newStartPos),
            result.src.drop(newStartPos).take(currentSizeWithSpace),
            result.src.drop(newStartPos + currentSizeWithSpace),
          )
        )
      case values =>
        if (values.isEmpty) {
          println(s"not found")
        } else {
          println(s"multi values ${values}")
        }
        Left(result.src)
    }
  }

  def rawWithPos0(
    src: String,
    dialect: String,
    scalafmtConfig: String,
    line: Int,
    column: Int,
  ): WithPosResult = {
    import scala.meta._
    val convert = implicitly[Convert[String, Input]]
    val main = new ScalametaAST
    val dialects = List(scala.meta.dialects.Scala3)

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
    val tokens =
      implicitly[Parse[Term]].apply(Input.String(res), scala.meta.dialects.Scala3).get.tokens
    val tokenMap: List[WrappedToken] = {
      val head = WrappedToken(
        token = tokens.head,
        addedSpaceByScalafmt = false,
      )

      head +: tokens.lazyZip(tokens.drop(1)).map { (t1, t2) =>
        WrappedToken(
          token = t2,
          addedSpaceByScalafmt = {
            !t1.is[scala.meta.tokens.Token.Comma] && t2.is[scala.meta.tokens.Token.Whitespace]
          }
        )
      }
    }.toList
    assert(tokenMap.size == tokens.size)
    val cursorPos = {
      if (src.isEmpty) {
        Position.Range(input, 0, 0)
      } else if (line >= src.linesIterator.size) {
        Position.Range(input, src.length, src.length)
      } else {
        Position.Range(input, line, column, line, column)
      }
    }.start

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

    val result: List[(String, Int)] = t2.flatMap { cursorTree =>
      val current = cursorTree.structure
      val currentSize = current.length
      tree.structure.sliding(currentSize).zipWithIndex.filter(_._1 == current).map(_._2).map { pos =>
        (current, pos)
      }
    }
    WithPosResult(res, result, tokenMap)
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
