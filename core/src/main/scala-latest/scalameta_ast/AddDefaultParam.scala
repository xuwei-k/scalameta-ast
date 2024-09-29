package scalameta_ast

import scala.annotation.tailrec
import scala.meta.Position
import scala.meta.Term

object AddDefaultParam {

  def addDefaultParam(parsed: Term, str: String): String = {
    val values = parsed.collect {
      case Term.Apply.After_4_6_0(
            Term.Select(Term.Name("Term"), Term.Name("ParamClause" | "ArgClause")),
            Term.ArgClause(
              List(a),
              _
            )
          ) =>
        a.pos
    }

    @tailrec
    def loop(acc: List[String], code: String, consumed: Int, src: List[Position]): List[String] = {
      src match {
        case head :: tail =>
          val (x1, x2) = code.splitAt(head.end - consumed)
          loop(
            s", None" :: x1 :: acc,
            x2,
            x1.length + consumed,
            tail
          )
        case Nil =>
          (code :: acc).reverse
      }
    }
    loop(Nil, str, 0, values).mkString
  }

}
