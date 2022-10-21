package scalameta_ast

import scala.scalajs.js.annotation._

@JSExportTopLevel("ScalametaAstMain")
object Main {
  @JSExport
  def convert(source: String): String =
    new ScalametaAST().convert(source)
}
