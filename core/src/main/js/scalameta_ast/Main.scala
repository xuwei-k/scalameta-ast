package scalameta_ast

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

object Main extends MainCompat {
  def main(args: Array[String]): Unit = println("hello " + args.mkString(" "))

  @JSExportTopLevel("format")
  def format(source: String, scalafmtConfJsonStr: String): js.Object =
    formatImpl(source = source, scalafmtConfJsonStr = scalafmtConfJsonStr)

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

  @JSExportTopLevel("convert")
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
        val ast = output.result
        val astBuildMs = output.time.toDouble
      }
    } catch {
      case e: Throwable =>
        new js.Object {
          val error = e
          val errorString: String = e.toString
        }
    }
  }
}
