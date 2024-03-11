package scalameta_ast

sealed abstract class ScalametaV extends Product with Serializable

object ScalametaV {
  case object V_0_10 extends ScalametaV
  case object V_0_11 extends ScalametaV
}
