package scalameta_ast

import org.scalafmt.config.ScalafmtConfig
import java.util.Date
import scala.annotation.tailrec
import scala.meta._
import scala.meta.common.Convert
import scala.meta.parsers.Parse
import scala.meta.parsers.Parsed
import scala.util.control.NonFatal

class ScalametaAST {
  private val parsers: List[(Parse[Tree], Dialect)] = for {
    x1 <- List(
      implicitly[Parse[Stat]],
      implicitly[Parse[Source]],
    ).map(_.asInstanceOf[Parse[Tree]])
    x2 <- List(
      dialects.Scala213Source3,
      dialects.Scala3,
    )
  } yield (x1, x2)

  @tailrec
  private def loop(input: Input, xs: List[(Parse[Tree], Dialect)]): Tree = {
    (xs: @unchecked) match {
      case (parse, dialect) :: t1 :: t2 =>
        parse.apply(input, dialect) match {
          case s: Parsed.Success[_] =>
            s.get
          case _: Parsed.Error =>
            loop(input, t1 :: t2)
        }
      case (parse, dialect) :: Nil =>
        parse.apply(input, dialect).get
    }
  }

  private val convert = implicitly[Convert[String, Input]]

  private val scalafmtConfig = ScalafmtConfig.default.copy(
    maxColumn = 50,
  )

  private def stopwatch[T](block: => T): (T, Long) = {
    val begin = new Date()
    val result = block
    val end = new Date()
    val diffMs = end.getTime - begin.getTime
    (result, diffMs)
  }

  def runFormat(source: String): String = {
    try {
      org.scalafmt.Scalafmt.format(source, scalafmtConfig).get
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        source
    }
  }

  def convert(src: String, format: Boolean): Output = {
    val input = convert.apply(src)
    val (ast, astBuildMs) = stopwatch {
      loop(input, parsers).structure
    }
    val (res, formatMs) = stopwatch {
      if (format) {
        runFormat(source = ast)
      } else {
        ast
      }
    }
    Output(res, astBuildMs, formatMs)
  }

}

case class Output(ast: String, astBuildMs: Long, formatMs: Long)
