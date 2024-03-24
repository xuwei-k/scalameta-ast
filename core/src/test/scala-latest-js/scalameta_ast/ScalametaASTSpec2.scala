package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec

class ScalametaASTSpec2 extends AnyFreeSpec {
  "ScalametaAST" - {
    val main = new ScalametaAST

    "Term.If" in {
      val expect =
        """Term.If.After_4_4_0(Term.Name("a"), Term.Name("b"), Term.If.After_4_4_0(Term.Name("x"), Term.Name("y"), Term.Select(Term.Name("z"), Term.Name("f")), Nil), Nil)"""
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
      assert(result.ast == expect)
    }

    "Term.Match" in {
      val expect =
        """Term.Match.After_4_4_5(Term.Name("a"), List(Case(Pat.Var(Term.Name("b")), None, Term.Block(Nil))), List(Mod.Inline()))"""
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
      assert(result.ast == expect)
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
        """Defn.Type.After_4_6_0(Nil, Type.Name("A"), Type.ParamClause(Nil), Type.Name("B"), Type.Bounds(None, None))"""
      assert(result.ast == expect)
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
        """Defn.Class.After_4_6_0(Nil, Type.Name("A"), Type.ParamClause(Nil), Ctor.Primary.After_4_6_0(Nil, Name.Anonymous(), Nil), Template.After_4_4_0(Nil, Nil, Self(Name.Anonymous(), None), Nil, List(Type.Name("B"))))"""
      assert(result.ast == expect)
    }
  }
}
