package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec

class ScalametaASTSpec2 extends AnyFreeSpec {
  "ScalametaAST" - {
    val main = new ScalametaAST

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

        val expect = {
          if (remove) {
            """|Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("B"))
               |""".stripMargin
          } else {
            """|Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("B"), Type.Bounds(None, None))
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

        val expect = {
          if (remove) {
            """|Defn.Class(
               |  Nil,
               |  Type.Name("A"),
               |  Nil,
               |  Ctor.Primary(Nil, Name(""), Nil),
               |  Template(Nil, Nil, Self(Name(""), None), Nil)
               |)
               |""".stripMargin
          } else {
            """|Defn.Class(
               |  Nil,
               |  Type.Name("A"),
               |  Nil,
               |  Ctor.Primary(Nil, Name(""), Nil),
               |  Template(Nil, Nil, Self(Name(""), None), Nil, List(Type.Name("B")))
               |)
               |""".stripMargin
          }
        }
        assert(result.ast == expect)
      }
    }

  }
}
