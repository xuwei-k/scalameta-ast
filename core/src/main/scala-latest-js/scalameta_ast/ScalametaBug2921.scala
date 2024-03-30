package scalameta_ast

import scala.meta.Tree

object ScalametaBug2921 {
  def convert(t: Tree): String = t.structure
}
