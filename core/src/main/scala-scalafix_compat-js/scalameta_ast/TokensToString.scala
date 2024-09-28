package scalameta_ast

import scala.meta.Tokens
import scala.meta.tokens.Token

object TokensToString {
  def tokensToString(tokens: Tokens): String = {
    tokens.tokens.map { x =>
      val n = x.getClass.getSimpleName
      def q(a: String): String = s"(${scala.meta.Lit.String(a)})"

      PartialFunction
        .condOpt(x) {
          case y: Token.Ident =>
            s"Ident${q(y.value)}"
          case y: Token.Comment =>
            s"Comment${q(y.value)}"
          case y: Token.Interpolation.Id =>
            s"Interpolation.Id${q(y.value)}"
          case y: Token.Interpolation.Part =>
            s"Interpolation.Part${q(y.value)}"
          case _: Token.Interpolation.Start =>
            s"Interpolation.$n"
          case _: Token.Interpolation.SpliceStart =>
            s"Interpolation.$n"
          case _: Token.Interpolation.SpliceEnd =>
            s"Interpolation.$n"
          case _: Token.Interpolation.End =>
            s"Interpolation.$n"
          case y: Token.Constant[?] =>
            y match {
              case z: Token.Constant.Int =>
                s"Constant.Int(BigInt${q(z.value.toString)})"
              case z: Token.Constant.Long =>
                s"Constant.Long(BigInt${q(z.value.toString)})"
              case z: Token.Constant.Float =>
                s"Constant.Float(BigDecimal${q(z.value.toString)})"
              case z: Token.Constant.Double =>
                s"Constant.Double(BigDecimal${q(z.value.toString)})"
              case z: Token.Constant.Char =>
                s"Constant.Char('${z.value}')"
              case z: Token.Constant.Symbol =>
                s"Constant.Symbol(scala.Symbol${q(z.value.name)})"
              case z: Token.Constant.String =>
                s"Constant.String${q(z.value)}"
            }
          case _: Token.Xml.Start =>
            s"Xml.${n}"
          case y: Token.Xml.Part =>
            s"Xml.Part${q(y.value)}"
          case _: Token.Xml.SpliceStart =>
            s"Xml.${n}"
          case _: Token.Xml.SpliceEnd =>
            s"Xml.${n}"
          case _: Token.Xml.End =>
            s"Xml.${n}"
          case _: Token.Indentation.Indent =>
            s"Indentation.${n}"
          case _: Token.Indentation.Outdent =>
            s"Indentation.${n}"
        }
        .getOrElse(n)
    }.map("Token." + _).mkString("Seq(", ", ", ")")
  }

}
