package scalameta_ast

import metaconfig.Conf
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

  private val defaultScalafmtConfig = ScalafmtConfig.default.copy(
    maxColumn = 50,
  )

  private def stopwatch[T](block: => T): (T, Long) = {
    val begin = new Date()
    val result = block
    val end = new Date()
    val diffMs = end.getTime - begin.getTime
    (result, diffMs)
  }

  def runFormat(source: String, scalafmtConfigJsonStr: String): String =
    runFormat(
      source = source,
      conf = scalafmtConfigJsonStringToScalafmtConfig(scalafmtConfigJsonStr)
    )

  private def runFormat(source: String, conf: ScalafmtConfig): String = {
    try {
      org.scalafmt.Scalafmt.format(source, conf).get
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        source
    }
  }

  private def scalafmtConfigJsonStringToScalafmtConfig(jsonStr: String): ScalafmtConfig = {
    if (jsonStr.trim.isEmpty) {
      defaultScalafmtConfig
    } else {
      try {
        val scalafmtConfig = jsonStringToMetaConfig(jsonStr)
        ScalafmtConfig.decoder.read(None, scalafmtConfig).get
      } catch {
        case NonFatal(e) =>
          e.printStackTrace()
          defaultScalafmtConfig
      }
    }
  }

  def convert(src: String, format: Boolean, scalafmtConfJsonStr: String): Output = {
    val scalafmtConfig = scalafmtConfigJsonStringToScalafmtConfig(scalafmtConfJsonStr)
    val input = convert.apply(src)
    val (ast, astBuildMs) = stopwatch {
      loop(input, parsers).structure
    }
    val (res, formatMs) = stopwatch {
      if (format) {
        runFormat(source = ast, scalafmtConfig)
      } else {
        ast
      }
    }
    Output(res, astBuildMs, formatMs)
  }

  def jsonStringToMetaConfig(jsonString: String): Conf =
    argonaut.JsonParser.parse(jsonString).fold(e => sys.error(e), argonautToMetaConfig)

  def argonautToMetaConfig(value: argonaut.Json): Conf =
    value.fold(
      jsonNull = Conf.Null(),
      jsonBool = x => Conf.Bool(x),
      jsonNumber = x => Conf.Num(x.toBigDecimal),
      jsonString = x => Conf.Str(x),
      jsonArray = x => Conf.Lst(x.map(argonautToMetaConfig)),
      jsonObject = x =>
        Conf.Obj(
          x.toList.map { case (k, v) => k -> argonautToMetaConfig(v) }
        )
    )
}

case class Output(ast: String, astBuildMs: Long, formatMs: Long)
