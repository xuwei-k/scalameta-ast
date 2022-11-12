package scalameta_ast

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel("ScalametaAstMain")
object Main {
  @JSExport
  @nowarn
  def convert(source: String, format: Boolean, scalafmtConfJsonStr: String): js.Object = {
    val output = new ScalametaAST().convert(src = source, format = format, scalafmtConfJsonStr = scalafmtConfJsonStr)
    new js.Object {
      var ast = output.ast
      var astBuildMs = output.astBuildMs.toDouble
      var formatMs = output.formatMs.toDouble
    }
  }

  @JSExport
  def format(source: String, scalafmtConfJsonStr: String): String =
    new ScalametaAST().runFormat(source = source, scalafmtConfigJsonStr = scalafmtConfJsonStr)

}
