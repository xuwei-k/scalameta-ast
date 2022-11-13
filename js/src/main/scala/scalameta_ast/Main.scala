package scalameta_ast

import metaconfig.Conf
import org.scalafmt.config.ScalafmtConfig
import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel("ScalametaAstMain")
object Main {
  @JSExport
  @nowarn
  def convert(source: String, format: Boolean, scalafmtConfJsonStr: String): js.Object = {
    val output =
      new ScalametaAST().convert(
        src = source,
        format = format,
        scalafmtConfig = convertJsonStringToMeteConfig(scalafmtConfJsonStr)
      )
    new js.Object {
      var ast = output.ast
      var astBuildMs = output.astBuildMs.toDouble
      var formatMs = output.formatMs.toDouble
    }
  }

  @JSExport
  def format(source: String, scalafmtConfJsonStr: String): String =
    new ScalametaAST().runFormat(
      source = source,
      scalafmtConfig = convertJsonStringToMeteConfig(scalafmtConfJsonStr)
    )

  @JSExport
  val defaultScalafmtConfig: String = {
    js.JSON.stringify(
      value = js.JSON
        .parse(
          ScalafmtConfig.encoder
            .write(
              ScalafmtConfig.default.copy(
                maxColumn = 50,
                runner =
                  ScalafmtConfig.default.runner.withDialect(sourcecode.Text(org.scalafmt.config.NamedDialect.scala3))
              )
            )
            .toString
        )
        .asInstanceOf[js.Any],
      replacer = (_, x) => if (x == null) () else x,
      space = 2
    )
  }

  private[this] def convertToMetaConfig(input: Any): Conf = input match {
    case x: String =>
      Conf.Str(x)
    case x: Double =>
      Conf.Num(x)
    case true =>
      Conf.Bool(true)
    case false =>
      Conf.Bool(false)
    case x: js.Array[_] =>
      Conf.Lst(x.map(a => convertToMetaConfig(a)).toList)
    case x: js.Object =>
      Conf.Obj(
        x.asInstanceOf[js.Dictionary[_]].map { case (k, v) => k -> convertToMetaConfig(v) }.toList
      )
    case _ =>
      Conf.Null()
  }

  private def convertJsonStringToMeteConfig(input: String): Conf =
    convertToMetaConfig(js.JSON.parse(input))

}
