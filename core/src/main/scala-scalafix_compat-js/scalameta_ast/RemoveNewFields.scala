package scalameta_ast

import scala.meta.Defn
import scala.meta.Template
import scala.meta.Term
import scala.meta.Tree
import scala.meta.contrib._
import scala.meta.tokens.Token

object RemoveNewFields {
  def remove(tree: Tree, parsed: Term, str: String): String = {
    if (
      tree.collectFirst {
        case _: Term.If => ()
        case _: Term.Match => ()
        case _: Defn.Type => ()
        case _: Template => ()
      }.nonEmpty
    ) {
      val positions = parsed.collect {
        case x @ Term.Apply(
              Term.Select(
                Term.Name("Term"),
                Term.Name("If")
              ),
              _ :: _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
        case x @ Term.Apply(
              Term.Select(
                Term.Name("Term"),
                Term.Name("Match")
              ),
              _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
        case x @ Term.Apply(
              Term.Select(
                Term.Name("Defn"),
                Term.Name("Type")
              ),
              _ :: _ :: _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
        case x @ Term.Apply(
              Term.Name("Template"),
              _ :: _ :: _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
      }.flatMap { case (tokens, toRemove) =>
        val startOpt =
          tokens.reverseIterator.filter(_.is[Token.Comma]).find(_.pos.start < toRemove.pos.start).map(_.start)
        val endOpt =
          tokens.reverseIterator.find(_.is[Token.RightParen]).map(_.pos.end - 1)
        startOpt.zip(endOpt)
      }

      str.zipWithIndex.flatMap { case (char, pos) =>
        Option.unless(positions.exists { case (start, end) => start <= pos && pos < end }) {
          char
        }
      }.mkString
    } else {
      str
    }
  }
}
