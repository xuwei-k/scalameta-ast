package scalameta_ast

import scala.annotation.unused
import scala.meta.Term
import scala.meta.Tree

object RemoveNewFields {
  def remove(@unused tree: Tree, @unused parsed: Term, str: String): String = str
}
