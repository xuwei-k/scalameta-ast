package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec

class ScalametaASTSpec extends AnyFreeSpec {
  "ScalametaAST" - {
    val main = new ScalametaAST
    "convert" in {
      val result = main.convert(
        src = "val x = ((), null)",
        outputType = "syntactic",
        packageName = Option("package_name"),
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = true,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      val expect = s"""package package_name
         |
         |import scala.meta.Defn
         |import scala.meta.Lit
         |import scala.meta.Pat
         |import scala.meta.Term
         |import scala.meta.transversers._
         |import scalafix.Patch
         |import scalafix.lint.Diagnostic
         |import scalafix.lint.LintSeverity
         |import scalafix.v1.SyntacticDocument
         |import scalafix.v1.SyntacticRule
         |import scalafix.v1.XtensionSeqPatch
         |
         |class Example extends SyntacticRule("Example") {
         |  override def fix(implicit doc: SyntacticDocument): Patch = {
         |    doc.tree.collect {
         |      case t @ Defn.Val(Nil, List(Pat.Var(Term.Name("x"))), None, Term.Tuple(List(Lit.Unit(), Lit.Null()))) =>
         |        Patch.lint(
         |          Diagnostic(
         |            id = "",
         |            message = "",
         |            position = t.pos,
         |            explanation = "",
         |            severity = LintSeverity.Warning
         |          )
         |        )
         |    }.asPatch
         |  }
         |}
         |""".stripMargin
      assert(result.result == expect)
    }

    "import" in {
      val result = main.convert(
        src = (
          "Case",
          "Ctor",
          "Decl",
          "Defn",
          "Defn",
          "Export",
          "Import",
          "Importee",
          "Init",
          "Member",
          "Mod",
          "Pkg",
          "Ref",
          "Self",
          "Stat",
          "Template",
          "Type"
        ).toString,
        outputType = "syntactic",
        packageName = Option("package_name"),
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = Some("empty"),
        removeNewFields = true,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      val expect =
        s"""package package_name
           |
           |import scala.meta.Term
           |import scala.meta.transversers._
           |import scalafix.Patch
           |import scalafix.v1.SyntacticDocument
           |import scalafix.v1.SyntacticRule
           |import scalafix.v1.XtensionSeqPatch
           |
           |class Example extends SyntacticRule("Example") {
           |  override def fix(implicit doc: SyntacticDocument): Patch = {
           |    doc.tree.collect {
           |      case t @ Term.Tuple(List(Term.Name("Case"), Term.Name("Ctor"), Term.Name("Decl"), Term.Name("Defn"), Term.Name("Defn"), Term.Name("Export"), Term.Name("Import"), Term.Name("Importee"), Term.Name("Init"), Term.Name("Member"), Term.Name("Mod"), Term.Name("Pkg"), Term.Name("Ref"), Term.Name("Self"), Term.Name("Stat"), Term.Name("Template"), Term.Name("Type"))) =>
           |        Patch.empty
           |    }.asPatch
           |  }
           |}
           |""".stripMargin
      assert(result.result == expect)
    }

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
