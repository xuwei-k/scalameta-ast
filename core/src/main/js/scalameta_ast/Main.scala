package scalameta_ast

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel(ExportName.value)
object Main extends MainCompat {
  @JSExport
  @nowarn
  def convert(
    source: String,
    outputType: String,
    packageName: String,
    wildcardImport: Boolean,
    ruleName: String,
    dialect: String,
    patch: String,
    removeNewFields: Boolean,
    initialExtractor: Boolean,
  ): js.Object = {
    try {
      val output =
        new ScalametaAST().convert(
          src = source,
          outputType = outputType,
          packageName = Option(packageName).filter(_.trim.nonEmpty),
          wildcardImport = wildcardImport,
          ruleNameOption = Option(ruleName).filter(_.trim.nonEmpty),
          dialect = Option(dialect).filter(_.trim.nonEmpty),
          patch = Option(patch).filter(_.trim.nonEmpty),
          removeNewFields = removeNewFields,
          initialExtractor = initialExtractor,
        )
      new js.Object {
        var ast = output.ast
        var astBuildMs = output.astBuildMs.toDouble
      }
    } catch {
      case e: Throwable =>
        new js.Object {
          var error = e
          var errorString: String = e.toString
        }
    }
  }
}
