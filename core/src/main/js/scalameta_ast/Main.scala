package scalameta_ast

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel(ExportName.value)
object Main extends MainCompat {
  @JSExport
  def initialize(): js.Object = {
    convert(
      source = "",
      outputType = "",
      packageName = "",
      wildcardImport = false,
      ruleName = "",
      dialect = "",
      patch = "",
      removeNewFields = false,
      initialExtractor = false,
      explanation = true,
      pathFilter = false,
    )
  }

  @JSExport
  @nowarn("msg=never used")
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
    explanation: Boolean,
    pathFilter: Boolean,
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
          explanation = explanation,
          pathFilter = pathFilter,
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
