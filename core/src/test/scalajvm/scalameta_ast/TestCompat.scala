package scalameta_ast

object TestCompat {
  def scalametaTreeFile(i: Int): String =
    scala.io.Source.fromURL(this.getClass.getResource(s"/trees${i}.scala")).getLines().mkString("\n")
}
