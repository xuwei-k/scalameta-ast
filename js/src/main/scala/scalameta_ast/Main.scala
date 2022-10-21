package scalameta_ast

import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel("ScalametaAstMain")
object Main {
  @JSExport
  def convert(source: String): js.Object = {
    val output = new ScalametaAST().convert(source)
    new js.Object {
      var ast = output.ast
      var astBuildMs = output.astBuildMs.toDouble
      var formatMs = output.formatMs.toDouble
    }
  }
}
