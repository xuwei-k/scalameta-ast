package scalameta_ast

import scala.meta.Tree

object ScalametaBug2921 {
  // https://github.com/scalameta/scalameta/pull/2921
  private[this] val scalametaBugWorkaround: Seq[(String, String)] = Seq(
    "Lit.Unit(())" -> "Lit.Unit()",
    "Lit.Null(null)" -> "Lit.Null()"
  )

  def convert(t: Tree): String = {
    scalametaBugWorkaround.foldLeft(t.structure) { case (s, (x1, x2)) =>
      s.replace(x1, x2)
    }
  }
}
