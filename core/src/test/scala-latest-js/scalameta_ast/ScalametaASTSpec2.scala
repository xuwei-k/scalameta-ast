package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec
import scala.meta.inputs.Input
import scala.meta.Defn
import scala.meta.Pkg
import scala.meta.Tree
import scala.meta.dialects

class ScalametaASTSpec2 extends AnyFreeSpec {
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
      val expect =
        s"""package package_name
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
           |      case t @ Defn.Val(
           |  Nil,
           |  List(
           |    Pat.Var(Term.Name("x"))
           |  ),
           |  None,
           |  Term.Tuple(List(
           |    Lit.Unit(),
           |    Lit.Null()
           |  ))
           |) =>
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
           |      case t @ Term.Tuple(List(
           |  Term.Name("Case"),
           |  Term.Name("Ctor"),
           |  Term.Name("Decl"),
           |  Term.Name("Defn"),
           |  Term.Name("Defn"),
           |  Term.Name("Export"),
           |  Term.Name("Import"),
           |  Term.Name("Importee"),
           |  Term.Name("Init"),
           |  Term.Name("Member"),
           |  Term.Name("Mod"),
           |  Term.Name("Pkg"),
           |  Term.Name("Ref"),
           |  Term.Name("Self"),
           |  Term.Name("Stat"),
           |  Term.Name("Template"),
           |  Term.Name("Type")
           |)) =>
           |        Patch.empty
           |    }.asPatch
           |  }
           |}
           |""".stripMargin
      assert(result.result == expect)
    }

    "Term.ParamClause and ArgClause" in {
      val expect =
        """Defn.Def.After_4_7_3(
          |  Nil,
          |  Term.Name("a"),
          |  List(
          |    Member.ParamClauseGroup(
          |      Type.ParamClause(Nil),
          |      List(
          |        Term.ParamClause(List(
          |          Term.Param(
          |            Nil,
          |            Term.Name("b"),
          |            Some(
          |              Type.Name("C")
          |            ),
          |            None
          |          )
          |        ), None)
          |      )
          |    )
          |  ),
          |  None,
          |  Term.Apply.After_4_6_0(
          |    Term.Name("d"),
          |    Term.ArgClause(List(
          |      Term.Name("e")
          |    ), None)
          |  )
          |)""".stripMargin
      val result = main.convert(
        src = "def a(b: C) = d(e)",
        outputType = "",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = false,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      assert(result.result == expect)
    }

    "Term.ParamClause and ArgClause 2" in {
      val expect =
        """Term.Apply.After_4_6_0(
          |  Term.Name("a"),
          |  Term.ArgClause(List(
          |    Term.Function.After_4_6_0(
          |      Term.ParamClause(List(
          |        Term.Param(
          |          Nil,
          |          Term.Name("b"),
          |          None,
          |          None
          |        )
          |      ), None),
          |      Term.Name("c")
          |    )
          |  ), None)
          |)""".stripMargin
      val result = main.convert(
        src = "a(b => c)",
        outputType = "",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = false,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      assert(result.result == expect)
    }

    "Term.If" in {
      val expect =
        """Term.If.After_4_4_0(
          |  Term.Name("a"),
          |  Term.Name("b"),
          |  Term.If.After_4_4_0(
          |    Term.Name("x"),
          |    Term.Name("y"),
          |    Term.Select(
          |      Term.Name("z"),
          |      Term.Name("f")
          |    ),
          |    Nil
          |  ),
          |  Nil
          |)""".stripMargin
      val result = main.convert(
        src = """if a then b else (if(x) y else z.f)""",
        outputType = "",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
        removeNewFields = false,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      assert(result.result == expect)
    }

    "Term.Match" in {
      val expect =
        """Term.Match.After_4_9_9(
          |  Term.Name("a"),
          |  Term.CasesBlock(List(
          |    Case(
          |      Pat.Var(Term.Name("b")),
          |      None,
          |      Term.Block(Nil)
          |    )
          |  )),
          |  List(
          |    Mod.Inline()
          |  )
          |)""".stripMargin
      val result = main.convert(
        src = """inline a match { case b => }""",
        outputType = "",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = Some("Scala3"),
        patch = None,
        removeNewFields = false,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )
      assert(result.result == expect)
    }

    "Defn.Type" in {
      val result = main.convert(
        src = """type A = B""",
        outputType = "",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = Some("Scala3"),
        patch = None,
        removeNewFields = false,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )

      val expect =
        """Defn.Type.After_4_6_0(
          |  Nil,
          |  Type.Name("A"),
          |  Type.ParamClause(Nil),
          |  Type.Name("B"),
          |  Type.Bounds.After_4_12_3(
          |    None,
          |    None,
          |    Nil,
          |    Nil
          |  )
          |)""".stripMargin
      assert(result.result == expect)
    }

    "Template" in {
      val result = main.convert(
        src = """class A derives B""",
        outputType = "",
        packageName = None,
        wildcardImport = false,
        ruleNameOption = None,
        dialect = Some("Scala3"),
        patch = None,
        removeNewFields = false,
        initialExtractor = false,
        explanation = true,
        pathFilter = false,
      )

      val expect =
        """Defn.Class.After_4_6_0(
          |  Nil,
          |  Type.Name("A"),
          |  Type.ParamClause(Nil),
          |  Ctor.Primary.After_4_6_0(
          |    Nil,
          |    Name.Anonymous(),
          |    Nil
          |  ),
          |  Template.After_4_9_9(
          |    None,
          |    Nil,
          |    Template.Body(
          |      None,
          |      Nil
          |    ),
          |    List(
          |      Type.Name("B")
          |    )
          |  )
          |)""".stripMargin
      assert(result.result == expect)
    }

    "top level scalameta classes" in {
      val src = TestCompat.scalametaTreeFile(0)
      val parsed =
        implicitly[scala.meta.parsers.Parse[scala.meta.Source]].apply(Input.String(src), dialects.Scala213Source3).get

      def topLevel(t: Tree): Boolean =
        t.parent.exists {
          case _: Pkg.Body =>
            true
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
      }.toSet -- Set("All", "Quasi")
      val expect = main.topLevelScalametaDefinitions.map(_.getSimpleName).toSet
      assert(values == expect)
    }
  }
}
