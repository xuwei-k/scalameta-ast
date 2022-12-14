package scalameta_ast

import metaconfig.Conf
import org.ekrich.config.Config
import org.ekrich.config.ConfigFactory
import org.ekrich.config.ConfigRenderOptions
import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._

@JSExportTopLevel("ScalametaAstMain")
object Main {
  @JSExport
  @nowarn
  def convert(source: String, format: Boolean, scalafmtConfJsonStr: String, outputType: String): js.Object = {
    val output =
      new ScalametaAST().convert(
        src = source,
        format = format,
        scalafmtConfig = hoconToMetaConfig(scalafmtConfJsonStr),
        outputType = outputType
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
      scalafmtConfig = hoconToMetaConfig(scalafmtConfJsonStr)
    )

  private[this] def hoconToMetaConfig(config: String): Conf =
    convertSConfigToMetaConfig(ConfigFactory.parseString(config))

  private[this] def convertSConfigToMetaConfig(config: Config): Conf =
    convertToMetaConfig(JSON.parse(config.root.render(ConfigRenderOptions.concise)))

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

}
