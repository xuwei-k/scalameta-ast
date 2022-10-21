package scalameta_ast

import org.scalafmt.config.ScalafmtConfig
import scala.annotation.tailrec
import scala.meta._
import scala.meta.common.Convert
import scala.meta.parsers.Parse
import scala.meta.parsers.Parsed

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

  def convert(src: String): String = {
    val input = convert.apply(src)
    val prefix = "class A {\n"
    val suffix = "}\n"
    val ast = prefix + loop(input, parsers).structure + suffix
    val res = org.scalafmt.Scalafmt.format(ast, scalafmtConfig).get
    res.drop(prefix.length).dropRight(suffix.length)
  }

}
