package scalameta_ast

import scala.annotation.tailrec
import scala.meta.Term

/**
 * [[https://github.com/scalameta/scalameta/commit/f27fd027df1de8285b1cc83027adf0b255cdb7f6]]
 */
object AddDefaultParam {

  def addDefaultParam(parsed: Term, str: String): String = {
    val positions = parsed.collect {
      case Term.Apply.After_4_6_0(
            Term.Select(Term.Name("Term"), Term.Name("ParamClause" | "ArgClause")),
            Term.ArgClause(
              List(a),
              _
            )
          ) =>
        a.pos.end
    }.sorted

    @tailrec
    def loop(acc: List[String], code: String, consumed: Int, positions: List[Int]): List[String] = {
      positions match {
        case head :: tail =>
          val (x1, x2) = code.splitAt(head - consumed)
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
    loop(Nil, str, 0, positions).mkString
  }

}
