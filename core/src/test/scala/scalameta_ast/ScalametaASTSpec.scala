package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec

class ScalametaASTSpec extends AnyFreeSpec {
  "ScalametaAST" - {
    val main = new ScalametaAST

    "invalid tree" in {
      val result = main.convert(
        src = """(""",
        outputType = "tokens",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = true,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      val expect = """Seq(Token.BOF, Token.LeftParen, Token.EOF)"""
      assert(result.result == expect)
    }

    "convert token" in {
      val result = main.convert(
        src = """def x(y: Z) = ('y, 'a', "b", 1.5, 4.4f, 2L, 3, s"x1${x2}", <g>{p}</g>) // c """,
        outputType = "tokens",
        packageName = Option("package_name"),
        wildcardImport = false,
        ruleNameOption = None,
        dialect = Some("Scala213"),
        patch = None,
        removeNewFields = true,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      val expect =
        """Seq(Token.BOF, Token.KwDef, Token.Space, Token.Ident("x"), Token.LeftParen, Token.Ident("y"), Token.Colon, Token.Space, Token.Ident("Z"), Token.RightParen, Token.Space, Token.Equals, Token.Space, Token.LeftParen, Token.Constant.Symbol(scala.Symbol("y")), Token.Comma, Token.Space, Token.Constant.Char('a'), Token.Comma, Token.Space, Token.Constant.String("b"), Token.Comma, Token.Space, Token.Constant.Double(BigDecimal("1.5")), Token.Comma, Token.Space, Token.Constant.Float(BigDecimal("4.4")), Token.Comma, Token.Space, Token.Constant.Long(BigInt("2")), Token.Comma, Token.Space, Token.Constant.Int(BigInt("3")), Token.Comma, Token.Space, Token.Interpolation.Id("s"), Token.Interpolation.Start, Token.Interpolation.Part("x1"), Token.Interpolation.SpliceStart, Token.LeftBrace, Token.Ident("x2"), Token.RightBrace, Token.Interpolation.SpliceEnd, Token.Interpolation.Part(""), Token.Interpolation.End, Token.Comma, Token.Space, Token.Xml.Start, Token.Xml.Part("<g>"), Token.Xml.SpliceStart, Token.LeftBrace, Token.Ident("p"), Token.RightBrace, Token.Xml.SpliceEnd, Token.Xml.Part("</g>"), Token.Xml.End, Token.RightParen, Token.Space, Token.Comment(" c "), Token.EOF)"""
      assert(result.result == expect)
    }

  }
}
