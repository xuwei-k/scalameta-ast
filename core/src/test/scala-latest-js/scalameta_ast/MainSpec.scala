package scalameta_ast

import org.scalactic.source.Position
import org.scalatest.freespec.AnyFreeSpec

class MainSpec extends AnyFreeSpec {
  "rawWithPos" - {
    "empty" in {
      val actual = Main.rawWithPos(
        src = "",
        dialect = "Scala3",
        scalafmtConfig = "",
        line = 0,
        column = 0,
      )
      assert(actual == Nil)
    }

    "test" in {
      def check(pos: Int): List[(String, Int)] = {
        val lines = Seq(
          """class A {""",
          """  def a(x: Y): Z =""",
          """    b""",
          """}""",
        )
        val src = lines.mkString("\n")
        assert(0 <= pos && pos < src.length)
        if (pos == 0) {
          Main.rawWithPos(
            src = src,
            dialect = "Scala3",
            scalafmtConfig = "",
            line = 0,
            column = 0,
          )
        } else {
          val (lineSrc, sum, lineNumber) = lines.zipWithIndex.map { case (line, index) =>
            (line, lines.take(index + 1).map(_.length).sum, index)
          }.dropWhile(_._2 < pos).headOption.getOrElse((lines.last, src.length, lines.size))
          val column = pos - sum + lineSrc.length
          println(
            (
              ("pos", pos),
              ("lineNumber", lineNumber),
              ("sum", sum),
              ("column", column),
            )
          )
          Main.rawWithPos(
            src = src,
            dialect = "Scala3",
            scalafmtConfig = "",
            line = lineNumber,
            column = column,
          )
        }
      }

      def checkClass(pos: Int)(implicit p: Position) = {
        check(pos) match {
          case List((t, _)) =>
            assert(t.startsWith("Defn.Class(Nil"), pos)
        }
      }

      (0 to 5).foreach { pos =>
        checkClass(pos)
      }
      (6 to 7).foreach { pos =>
        assert(check(pos) == List(("""Type.Name("A")""", 16)), pos)
      }
      (8 to 14).foreach { pos =>
        checkClass(pos)
      }
      (15 to 16).foreach { pos =>
        assert(check(pos) == List(("""Term.Name("a")""", 165)), pos)
      }
      (17 to 18).foreach { pos =>
        assert(check(pos) == List(("""Term.Name("x")""", 276)), pos)
      }
      checkClass(19) // TODO detect `Def` not `Class`
      (20 to 21).foreach { pos =>
        assert(check(pos) == List(("""Type.Name("Y")""", 297)), pos)
      }
      (22 to 23).foreach { pos =>
        checkClass(pos) // TODO detect `Def` not `Class`
      }
      (24 to 25).foreach { pos =>
        assert(check(pos) == List(("""Type.Name("Z")""", 337)), pos)
      }
      (26 to 30).foreach { pos =>
        checkClass(pos) // TODO detect `Def` not `Class`
      }
      (31 to 32).foreach { pos =>
        assert(check(pos) == List(("""Term.Name("b")""", 354)), pos)
      }
      (33 to 35).foreach { pos =>
        checkClass(pos)
      }
    }
  }
}
