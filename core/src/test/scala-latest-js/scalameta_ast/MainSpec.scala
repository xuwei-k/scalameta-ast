package scalameta_ast

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
      def check(line: Int, column: Int): List[(String, Int)] = {
        Main.rawWithPos(
          src = Seq(
            """class A {""",
            """  def a(x: Y): Z =""",
            """    b""",
            """}""",
          ).mkString("\n"),
          dialect = "Scala3",
          scalafmtConfig = "",
          line = line,
          column = column,
        )
      }

      (0 to 5).foreach { column =>
        check(0, column) match {
          case List((t, _)) =>
            assert(t.startsWith("Defn.Class(Nil"), column)
        }
      }
      (6 to 8).foreach { column =>
        assert(check(0, column) == List(("""Type.Name("A")""", 16)), column)
      }
    }
  }
}
