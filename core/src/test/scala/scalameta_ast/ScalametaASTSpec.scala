package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec
import scala.meta.Defn
import scala.meta.Tree
import scala.meta.Pkg
import scala.meta.dialects
import scala.meta.inputs.Input

class ScalametaASTSpec extends AnyFreeSpec {
  "ScalametaAST" - {
    (0 to 1).foreach { i =>
      s"top level scalameta classes ${i}" in {
        val src = scala.io.Source.fromURL(this.getClass.getResource(s"/trees${i}.scala")).getLines().mkString("\n")
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
        val expect = (new ScalametaAST).topLevelScalametaDefinitions.map(_.getSimpleName).toSet
        assert(values == expect)
      }
    }
  }
}
