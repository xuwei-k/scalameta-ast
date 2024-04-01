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

case class WithPosResult(src: String, cursorValues: List[(String, Int)], tokenMap: List[(Token, Boolean)])

case class Highlighted(prefix: String, current: String, suffix: String)

trait MainCompat {

  @JSExport
  def rawWithPos(
    src: String,
    dialect: String,
    scalafmtConfig: String,
    line: Int,
    column: Int,
  ): String = {
    try {
      rawWithPos1(
        src = src,
        dialect = dialect,
        scalafmtConfig = scalafmtConfig,
        line = line,
        column = column
      ) match {
        case Right(x) =>
          List(x.prefix, "<span style='color: blue;'>", x.current, "</span>", x.suffix).mkString("")
        case Left(x) =>
          x
      }
    } catch {
      case e: Throwable =>
        println(e)
        ""
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

    if (false) {
      val aaaa = result.tokenMap.map { case (t, isSpace) =>
        if (isSpace) {
          t
        } else {
          List.fill(t.end - t.start)("x").mkString
        }
      }.mkString

      println(result.tokenMap.map(_._1).mkString)
      println(aaaa)
      println(aaaa.split(" ").filter(_.trim.nonEmpty).map(_.length).toList)
      println(result.tokenMap.map(_._1).mkString.linesIterator.map(_.trim.length + 1).toList)
    }

    result.cursorValues match {
      case List((s, pos)) =>
        val newStartPos = {
          @tailrec
          def loop(n: Int, list: List[(Token, Boolean)], acc: Int): Int = {
            list match {
              case x :: xs =>
                val tokenSize = x._1.end - x._1.start
                if (n <= 0) {
                  acc
                } else {
                  loop(if (x._2) n else n - tokenSize, xs, acc + tokenSize)
                }
              case _ =>
                sys.error(s"error ${n} ${acc}")
            }
          }
          loop(pos, result.tokenMap, 0)
        }
        println(
          Seq(
            ("current tree", s),
            ("pos", pos),
            ("new-pos", newStartPos),
            ("diff", newStartPos - pos),
            ("space until pos", result.tokenMap.takeWhile(_._1.start < pos).count(_._2)),
            ("spaces", result.tokenMap.count(_._2)),
            ("all space", result.tokenMap.count(_._1.is[Token.Whitespace]))
          ),
        )
        val currentSizeWithSpace = {
          @tailrec
          def loop(n: Int, list: List[(Token, Boolean)], acc: Int): Int = {
            list match {
              case x :: xs =>
                val tokenSize = x._1.end - x._1.start
                if (n <= 0) {
                  acc
                } else {
                  loop(if (x._2) n else n - tokenSize, xs, acc + tokenSize)
                }
              case _ =>
                sys.error(s"error ${n} ${acc}")
            }
          }
          loop(s.length, result.tokenMap.dropWhile(_._1.pos.end < newStartPos), 0)
        }
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
          println(s"not multi values ${values}")
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
    val tokenMap: List[(Token, Boolean)] = {
      (tokens.head -> false) +: tokens.lazyZip(tokens.drop(1)).map { (t1, t2) =>
        def isSpace(x: Token): Boolean = PartialFunction.cond(x) { case _: scala.meta.tokens.Token.Whitespace =>
          true
        }
        assert(t1.end == t2.start)
        if (isSpace(t1) && isSpace(t2)) {
          if (false) {
            println((t1.pos.startLine, t1.pos.startColumn, t1.productPrefix, t2.productPrefix))
          }
          t2 -> true
        } else if (!t1.is[scala.meta.tokens.Token.Comma] && isSpace(t2)) {
          t2 -> true
        } else {
          t2 -> false
        }
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

    if (t2.sizeIs > 1) {
      Console.err.println(s"found multi tree ${t2.map(_.productPrefix).mkString(", ")}")
    }

    val result: List[(String, Int)] = t2.flatMap { cursorTree =>
      val current = cursorTree.structure
      val currentSize = current.length
      tree.structure.sliding(currentSize).zipWithIndex.filter(_._1 == current).map(_._2).map { pos =>
        if (false) {
          println(Seq("pos" -> pos, "size" -> currentSize))
        }
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
