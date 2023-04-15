package scalameta_ast

sealed abstract class TreeType extends Product with Serializable

object TreeType {
  case object Tree extends TreeType
  case object Token extends TreeType
}
