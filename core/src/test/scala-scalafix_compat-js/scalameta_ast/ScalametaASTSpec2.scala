package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec
import scala.meta.inputs.Input
import scala.meta.Defn
import scala.meta.Pkg
import scala.meta.Tree
import scala.meta.dialects

class ScalametaASTSpec2 extends AnyFreeSpec {
  private case class TestArg(initial: Boolean, remove: Boolean)

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

    "Term.If" in {
      Seq[(String, List[TestArg])](
        """Term.If.Initial(Term.Name("a"), Term.Name("b"), Term.If.Initial(Term.Name("x"), Term.Name("y"), Term.Select(Term.Name("z"), Term.Name("f"))))"""
          -> List(TestArg(initial = true, remove = true)),
        """Term.If(Term.Name("a"), Term.Name("b"), Term.If(Term.Name("x"), Term.Name("y"), Term.Select(Term.Name("z"), Term.Name("f"))))""" -> List(
          TestArg(initial = false, remove = true)
        ),
        """Term.If(Term.Name("a"), Term.Name("b"), Term.If(Term.Name("x"), Term.Name("y"), Term.Select(Term.Name("z"), Term.Name("f")), Nil), Nil)""" -> List(
          TestArg(initial = false, remove = false),
          TestArg(initial = true, remove = false)
        )
      ).foreach { case (expect, args) =>
        args.foreach { arg =>
          val result = main.convert(
            src = """if a then b else (if(x) y else z.f)""",
            outputType = "",
            packageName = None,
            wildcardImport = false,
            ruleNameOption = None,
            dialect = None,
            patch = None,
            removeNewFields = arg.remove,
            initialExtractor = arg.initial,
            explanation = true,
            pathFilter = false,
          )
          assert(result.result == expect, arg)
        }
      }
    }

    "Term.Match" in {
      Seq[(String, List[TestArg])](
        """Term.Match.Initial(Term.Name("a"), List(Case(Pat.Var(Term.Name("b")), None, Term.Block(Nil))))""" -> List(
          TestArg(initial = true, remove = true)
        ),
        """Term.Match(Term.Name("a"), List(Case(Pat.Var(Term.Name("b")), None, Term.Block(Nil))))""" -> List(
          TestArg(initial = false, remove = true)
        ),
        """Term.Match(Term.Name("a"), List(Case(Pat.Var(Term.Name("b")), None, Term.Block(Nil))), List(Mod.Inline()))""" -> List(
          TestArg(initial = false, remove = false),
          TestArg(initial = true, remove = false)
        )
      ).foreach { case (expect, args) =>
        args.foreach { arg =>
          val result = main.convert(
            src = """inline a match { case b => }""",
            outputType = "",
            packageName = None,
            wildcardImport = false,
            ruleNameOption = None,
            dialect = Some("Scala3"),
            patch = None,
            removeNewFields = arg.remove,
            initialExtractor = arg.initial,
            explanation = true,
            pathFilter = false,
          )
          assert(result.result == expect, arg)
        }
      }
    }

    "Defn.Type" in {
      Seq[(String, List[TestArg])](
        """Defn.Type.Initial(Nil, Type.Name("A"), Nil, Type.Name("B"))""" -> List(
          TestArg(initial = true, remove = true)
        ),
        """Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("B"))""" -> List(
          TestArg(initial = false, remove = true)
        ),
        """Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("B"), Type.Bounds(None, None))""" -> List(
          TestArg(initial = true, remove = false),
          TestArg(initial = false, remove = false)
        )
      ).foreach { case (expect, args) =>
        args.foreach { arg =>
          val result = main.convert(
            src = """type A = B""",
            outputType = "",
            packageName = None,
            wildcardImport = false,
            ruleNameOption = None,
            dialect = Some("Scala3"),
            patch = None,
            removeNewFields = arg.remove,
            initialExtractor = arg.initial,
            explanation = true,
            pathFilter = false,
          )
          assert(result.result == expect, arg)
        }
      }
    }

    "Class, Template" in {
      Seq[(String, List[TestArg])](
        """Defn.Class.Initial(Nil, Type.Name("A"), Nil, Ctor.Primary.Initial(Nil, Name(""), Nil), Template.Initial(Nil, Nil, Self(Name(""), None), Nil))""" -> List(
          TestArg(initial = true, remove = true)
        ),
        """Defn.Class(Nil, Type.Name("A"), Nil, Ctor.Primary(Nil, Name(""), Nil), Template(Nil, Nil, Self(Name(""), None), Nil))""" -> List(
          TestArg(initial = false, remove = true)
        ),
        """Defn.Class(Nil, Type.Name("A"), Nil, Ctor.Primary(Nil, Name(""), Nil), Template(Nil, Nil, Self(Name(""), None), Nil, List(Type.Name("B"))))""" -> List(
          TestArg(initial = true, remove = false),
          TestArg(initial = false, remove = false)
        )
      ).foreach { case (expect, args) =>
        args.foreach { arg =>
          val result = main.convert(
            src = """class A derives B""",
            outputType = "",
            packageName = None,
            wildcardImport = false,
            ruleNameOption = None,
            dialect = Some("Scala3"),
            patch = None,
            removeNewFields = arg.remove,
            initialExtractor = arg.initial,
            explanation = true,
            pathFilter = false,
          )
          assert(result.result == expect, arg)
        }
      }
    }

    "top level scalameta classes" in {
      val src = TestCompat.scalametaTreeFile(1)
      val parsed =
        implicitly[scala.meta.parsers.Parse[scala.meta.Source]].apply(Input.String(src), dialects.Scala213Source3).get

      def topLevel(t: Tree): Boolean =
        t.parent.exists {
          case _: Pkg =>
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
