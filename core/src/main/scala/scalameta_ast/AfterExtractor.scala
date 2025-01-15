package scalameta_ast

import scala.meta.Term

sealed abstract class AfterExtractor {
  def tree: Term
  def extractor: String
}

object AfterExtractor extends AfterExtractorEnable {
  final case class E2(t1: String, t2: String, extractor: String) extends AfterExtractor {
    override val tree: Term = Term.Select(Term.Name(t1), Term.Name(t2))
  }

  final case class E1(t1: String, extractor: String) extends AfterExtractor {
    override val tree: Term = Term.Name(t1)
  }

  val values: List[AfterExtractor] = List(
    E2("Ctor", "Primary", "After_4_6_0"),
    E2("Ctor", "Secondary", "After_4_9_9"),
    E2("Decl", "Def", "After_4_7_3"),
    E2("Decl", "Given", "After_4_6_0"),
    E2("Decl", "Type", "After_4_6_0"),
    E2("Defn", "Class", "After_4_6_0"),
    E2("Defn", "Def", "After_4_7_3"),
    E2("Defn", "Enum", "After_4_6_0"),
    E2("Defn", "EnumCase", "After_4_6_0"),
    E2("Defn", "ExtensionGroup", "After_4_6_0"),
    E2("Defn", "Given", "After_4_6_0"),
    E2("Defn", "GivenAlias", "After_4_6_0"),
    E2("Defn", "Macro", "After_4_7_3"),
    E2("Defn", "Trait", "After_4_6_0"),
    E2("Defn", "Type", "After_4_6_0"),
    E2("Defn", "Var", "After_4_7_2"),
    E1("Init", "After_4_6_0"),
    E2("Pat", "Extract", "After_4_6_0"),
    E2("Pat", "ExtractInfix", "After_4_6_0"),
    E1("Pkg", "After_4_9_9"),
    E1("Template", "After_4_9_9"),
    E2("Term", "Apply", "After_4_6_0"),
    E2("Term", "ApplyInfix", "After_4_6_0"),
    E2("Term", "ApplyType", "After_4_6_0"),
    E2("Term", "ApplyUsing", "After_4_6_0"),
    E2("Term", "ContextFunction", "After_4_6_0"),
    E2("Term", "For", "After_4_9_9"),
    E2("Term", "ForYield", "After_4_9_9"),
    E2("Term", "Function", "After_4_6_0"),
    E2("Term", "If", "After_4_4_0"),
    E2("Term", "Match", "After_4_9_9"),
    E2("Term", "PolyFunction", "After_4_6_0"),
    E2("Term", "Try", "After_4_9_9"),
    E2("Type", "Apply", "After_4_6_0"),
    E2("Type", "Bounds", "After_4_12_3"),
    E2("Type", "ContextFunction", "After_4_6_0"),
    E2("Type", "Existential", "After_4_9_9"),
    E2("Type", "Function", "After_4_6_0"),
    E2("Type", "Lambda", "After_4_6_0"),
    E2("Type", "Match", "After_4_9_9"),
    E2("Type", "Method", "After_4_6_0"),
    E2("Type", "Param", "After_4_6_0"),
    E2("Type", "PolyFunction", "After_4_6_0"),
    E2("Type", "Refine", "After_4_9_9"),
    E2("Type", "TypedParam", "After_4_7_8"),
  )
}
