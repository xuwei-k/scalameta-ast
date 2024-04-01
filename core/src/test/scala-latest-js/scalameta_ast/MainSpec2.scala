package scalameta_ast

import org.scalatest.freespec.AnyFreeSpec

class MainSpec2 extends AnyFreeSpec {
  "rawWithPos" - {
    "test" in {
      def check(pos: Int): String = {
        val lines = Seq(
          """class A {""",
          """  def a(x: Y): Z =""",
          """    b""",
          """}""",
        )
        val src = lines.mkString("\n")
        assert(0 <= pos && pos < src.length)

        val (lineSrc, sum, lineNumber) = lines.zipWithIndex.map { case (line, index) =>
          (line, lines.take(index + 1).map(_.length).sum, index)
        }.dropWhile(_._2 < pos).headOption.getOrElse((lines.last, src.length, lines.size))
        val column = pos - sum + lineSrc.length
        Main
          .rawWithPos1(
            src = src,
            dialect = "Scala3",
            scalafmtConfig = "",
            line = lineNumber,
            column = column,
          )
          .fold(_ => sys.error("left"), identity)
          .current
          .trim
      }

      assert(check(6) == """Type.Name("A")""")
      assert(check(7) == """Type.Name("A")""")
      assert(check(15) == """Term.Name("a")""")
      assert(check(16) == """Term.Name("a")""")
      assert(check(17) == """Term.Name("x")""")
      (20 to 21).foreach { pos =>
        assert(check(pos) == """Type.Name("Y")""")
      }
      (24 to 25).foreach { pos =>
        assert(check(pos) == """Type.Name("Z")""")
      }
      (31 to 32).foreach { pos =>
        assert(check(pos) == """Term.Name("b")""")
      }
    }
  }
}
