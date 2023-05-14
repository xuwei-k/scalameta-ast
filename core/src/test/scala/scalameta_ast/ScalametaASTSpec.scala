package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec
import scala.meta.Defn
import scala.meta.Tree
import scala.meta.Pkg
import scala.meta.dialects
import scala.meta.inputs.Input

class ScalametaASTSpec extends AnyFreeSpec {
  "ScalametaAST" - {
    val main = new ScalametaAST
    "convert" in {
      val result = main.convert(
        src = "val x = ((), null)",
        format = true,
        scalafmtConfig = metaconfig.Conf.Obj.empty,
        outputType = "syntactic",
        packageName = Option("package_name"),
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = true,
      )
      val expect = s"""package package_name
         |
         |import scala.meta.Defn
         |import scala.meta.Lit
         |import scala.meta.Pat
         |import scala.meta.Term
         |import scalafix.Patch
         |import scalafix.lint.Diagnostic
         |import scalafix.lint.LintSeverity
         |import scalafix.v1.SyntacticDocument
         |import scalafix.v1.SyntacticRule
         |
         |class Example extends SyntacticRule("Example") {
         |  override def fix(implicit doc: SyntacticDocument): Patch = {
         |    doc.tree.collect {
         |      case t @ Defn.Val(
         |            Nil,
         |            List(Pat.Var(Term.Name("x"))),
         |            None,
         |            Term.Tuple(List(Lit.Unit(), Lit.Null()))
         |          ) =>
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
      assert(result.ast == expect)
    }

    "remove Term.If mods" in {
      Seq[(Boolean, String)](
        true ->
          """|Term.If(
           |  Term.Name("a"),
           |  Term.Name("b"),
           |  Term.If(
           |    Term.Name("x"),
           |    Term.Name("y"),
           |    Term.Select(Term.Name("z"), Term.Name("f"))
           |  )
           |)
           |""".stripMargin,
        false ->
          """|Term.If(
             |  Term.Name("a"),
             |  Term.Name("b"),
             |  Term.If(
             |    Term.Name("x"),
             |    Term.Name("y"),
             |    Term.Select(Term.Name("z"), Term.Name("f")),
             |    Nil
             |  ),
             |  Nil
             |)
             |""".stripMargin
      ).foreach { case (remove, expect) =>
        val result = main.convert(
          src = """if a then b else (if(x) y else z.f)""",
          format = true,
          scalafmtConfig = metaconfig.Conf.Obj.empty,
          outputType = "",
          packageName = None,
          wildcardImport = false,
          ruleNameOption = None,
          dialect = None,
          patch = None,
          removeNewFields = remove,
        )
        assert(result.ast == expect)
      }
    }

    "remove Term.Match mods" in {
      Seq[(Boolean, String)](
        true ->
          """|Term.Match(
             |  Term.Name("a"),
             |  List(Case(Pat.Var(Term.Name("b")), None, Term.Block(Nil)))
             |)
             |""".stripMargin,
        false ->
          """|Term.Match(
             |  Term.Name("a"),
             |  List(Case(Pat.Var(Term.Name("b")), None, Term.Block(Nil))),
             |  List(Mod.Inline())
             |)
             |""".stripMargin
      ).foreach { case (remove, expect) =>
        val result = main.convert(
          src = """inline a match { case b => }""",
          format = true,
          scalafmtConfig = metaconfig.Conf.Obj.empty,
          outputType = "",
          packageName = None,
          wildcardImport = false,
          ruleNameOption = None,
          dialect = Some("Scala3"),
          patch = None,
          removeNewFields = remove,
        )
        assert(result.ast == expect)
      }
    }

    "remove Defn.Type bounds" in {
      Seq[Boolean](
        true,
        false
      ).foreach { remove =>
        val result = main.convert(
          src = """type A = B""",
          format = true,
          scalafmtConfig = metaconfig.Conf.Obj.empty,
          outputType = "",
          packageName = None,
          wildcardImport = false,
          ruleNameOption = None,
          dialect = Some("Scala3"),
          patch = None,
          removeNewFields = remove,
        )

        val expect = ScalametaASTTestBuildInfo.scalametaVersion match {
          case "4.6.0" =>
            if (remove) {
              """Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("B"))
                |""".stripMargin
            } else {
              """Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("B"), Type.Bounds(None, None))
                |""".stripMargin
            }
          case "4.7.7" =>
            if (remove) {
              """Defn.Type(Nil, Type.Name("A"), Type.ParamClause(Nil), Type.Name("B"))
                |""".stripMargin
            } else {
              """Defn.Type(
                |  Nil,
                |  Type.Name("A"),
                |  Type.ParamClause(Nil),
                |  Type.Name("B"),
                |  Type.Bounds(None, None)
                |)
                |""".stripMargin
            }
        }
        assert(result.ast == expect)
      }
    }

    "remove Template derives" in {
      Seq[Boolean](
        true,
        false
      ).foreach { remove =>
        val result = main.convert(
          src = """class A derives B""",
          format = true,
          scalafmtConfig = metaconfig.Conf.Obj.empty,
          outputType = "",
          packageName = None,
          wildcardImport = false,
          ruleNameOption = None,
          dialect = Some("Scala3"),
          patch = None,
          removeNewFields = remove,
        )

        val expect = ScalametaASTTestBuildInfo.scalametaVersion match {
          case "4.6.0" =>
            if (remove) {
              """Defn.Class(
                |  Nil,
                |  Type.Name("A"),
                |  Nil,
                |  Ctor.Primary(Nil, Name(""), Nil),
                |  Template(Nil, Nil, Self(Name(""), None), Nil)
                |)
                |""".stripMargin
            } else {
              """Defn.Class(
                |  Nil,
                |  Type.Name("A"),
                |  Nil,
                |  Ctor.Primary(Nil, Name(""), Nil),
                |  Template(Nil, Nil, Self(Name(""), None), Nil, List(Type.Name("B")))
                |)
                |""".stripMargin
            }
          case "4.7.7" =>
            if (remove) {
              """Defn.Class(
                |  Nil,
                |  Type.Name("A"),
                |  Type.ParamClause(Nil),
                |  Ctor.Primary(Nil, Name(""), Nil),
                |  Template(Nil, Nil, Self(Name(""), None), Nil)
                |)
                |""".stripMargin
            } else {
              """Defn.Class(
                |  Nil,
                |  Type.Name("A"),
                |  Type.ParamClause(Nil),
                |  Ctor.Primary(Nil, Name(""), Nil),
                |  Template(Nil, Nil, Self(Name(""), None), Nil, List(Type.Name("B")))
                |)
                |""".stripMargin
            }
        }
        assert(result.ast == expect)
      }
    }

    "invalid tree" in {
      val result = main.convert(
        src = """(""",
        format = true,
        scalafmtConfig = metaconfig.Conf.Obj.empty,
        outputType = "tokens",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = true,
      )
      val expect =
        """Seq(Token.BOF, Token.LeftParen, Token.EOF)
          |""".stripMargin
      assert(result.ast == expect)
    }

    "convert token" in {
      val result = main.convert(
        src = """def x(y: Z) = ('y, 'a', "b", 1.5, 4.4f, 2L, 3, s"x1${x2}", <g>{p}</g>) // c """,
        format = true,
        scalafmtConfig = metaconfig.Conf.Obj.empty,
        outputType = "tokens",
        packageName = Option("package_name"),
        wildcardImport = false,
        ruleNameOption = None,
        dialect = Some("Scala213"),
        patch = None,
        removeNewFields = true,
      )
      val expect = """Seq(
        |  Token.BOF,
        |  Token.KwDef,
        |  Token.Space,
        |  Token.Ident("x"),
        |  Token.LeftParen,
        |  Token.Ident("y"),
        |  Token.Colon,
        |  Token.Space,
        |  Token.Ident("Z"),
        |  Token.RightParen,
        |  Token.Space,
        |  Token.Equals,
        |  Token.Space,
        |  Token.LeftParen,
        |  Token.Constant.Symbol(scala.Symbol("y")),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Constant.Char('a'),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Constant.String("b"),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Constant.Double(BigDecimal("1.5")),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Constant.Float(BigDecimal("4.4")),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Constant.Long(BigInt("2")),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Constant.Int(BigInt("3")),
        |  Token.Comma,
        |  Token.Space,
        |  Token.Interpolation.Id("s"),
        |  Token.Interpolation.Start,
        |  Token.Interpolation.Part("x1"),
        |  Token.Interpolation.SpliceStart,
        |  Token.LeftBrace,
        |  Token.Ident("x2"),
        |  Token.RightBrace,
        |  Token.Interpolation.SpliceEnd,
        |  Token.Interpolation.Part(""),
        |  Token.Interpolation.End,
        |  Token.Comma,
        |  Token.Space,
        |  Token.Xml.Start,
        |  Token.Xml.Part("<g>"),
        |  Token.Xml.SpliceStart,
        |  Token.LeftBrace,
        |  Token.Ident("p"),
        |  Token.RightBrace,
        |  Token.Xml.SpliceEnd,
        |  Token.Xml.Part("</g>"),
        |  Token.Xml.End,
        |  Token.RightParen,
        |  Token.Space,
        |  Token.Comment(" c "),
        |  Token.EOF
        |)
        |""".stripMargin
      assert(result.ast == expect)
    }

    (0 to 1).foreach { i =>
      s"top level scalameta classes ${i}" in {
        val src = TestCompat.scalametaTreeFile(i)
        val parsed =
          implicitly[scala.meta.parsers.Parse[scala.meta.Source]].apply(Input.String(src), dialects.Scala213Source3).get

        def topLevel(t: Tree): Boolean =
          t.parent.exists {
            case p: Pkg =>
              p.name.value == "meta"
            case _ =>
              false
          }

        val values = parsed.collect {
          case c: Defn.Class if topLevel(c) =>
            c.name.value
          case c: Defn.Trait if topLevel(c) =>
            c.name.value
          case c: Defn.Object if topLevel(c) =>
            c.name.value
        }.toSet
        val expect = main.topLevelScalametaDefinitions.map(_.getSimpleName).toSet
        assert(values == expect)
      }
    }
  }
}
