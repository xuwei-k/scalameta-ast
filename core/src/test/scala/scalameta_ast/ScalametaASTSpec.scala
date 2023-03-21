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
        src = "val x = y",
        format = true,
        scalafmtConfig = metaconfig.Conf.Obj.empty,
        outputType = "syntactic",
        packageName = Option("package_name"),
        wildcardImport = false,
        ruleNameOption = None,
        dialect = None,
        patch = None,
      )
      val expect = s"""package package_name
         |
         |import scala.meta.Defn
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
         |            Term.Name("y")
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
