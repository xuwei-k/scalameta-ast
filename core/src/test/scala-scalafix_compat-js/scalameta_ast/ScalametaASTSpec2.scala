package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec

class ScalametaASTSpec2 extends AnyFreeSpec {
  private case class TestArg(initial: Boolean, remove: Boolean)

  "ScalametaAST" - {
    val main = new ScalametaAST

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
          )
          assert(result.ast == expect, arg)
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
          )
          assert(result.ast == expect, arg)
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
          )
          assert(result.ast == expect, arg)
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
          )
          assert(result.ast == expect, arg)
        }
      }
    }

  }
}
