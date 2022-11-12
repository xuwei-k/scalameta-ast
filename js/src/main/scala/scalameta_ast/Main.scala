package scalameta_ast

import fastparse.Parsed
import metaconfig.Conf
import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.akkajs.shocon

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

  private[this] def convertJsAnyToMetaConfig(input: Any): Conf = input match {
    case x: String =>
      Conf.Str(x)
    case x: Double =>
      Conf.Num(x)
    case true =>
      Conf.Bool(true)
    case false =>
      Conf.Bool(false)
    case x: js.Array[_] =>
      Conf.Lst(x.map(a => convertJsAnyToMetaConfig(a)).toList)
    case x: js.Object =>
      Conf.Obj(
        x.asInstanceOf[js.Dictionary[_]].map { case (k, v) => k -> convertJsAnyToMetaConfig(v) }.toList
      )
    case _ =>
      Conf.Null()
  }

  private[this] def convertToMetaConfig(input: shocon.Config.Value): Conf = input match {
    case x: shocon.Config.StringLiteral =>
      Conf.Str(x.value)
    case x: shocon.Config.NumberLiteral =>
      Conf.Num(BigDecimal(x.value))
    case x: shocon.Config.BooleanLiteral =>
      Conf.Bool(x.value)
    case x: shocon.Config.Array =>
      Conf.Lst(x.unwrapped.map(a => convertJsAnyToMetaConfig(a)).toList)
    case x: shocon.Config.Object =>
      Conf.Obj(
        x.unwrapped.iterator.map { case (k, v) => k -> convertJsAnyToMetaConfig(v) }.toList
      )
    case _ =>
      Conf.Null()
  }

  private def convertJsonStringToMeteConfig(input: String): Conf = {
    shocon.ConfigParser.parseString(input) match {
      case Parsed.Success(value, _) =>
        convertToMetaConfig(value)
      case x: Parsed.Failure =>
        System.err.println(x)
        convertJsAnyToMetaConfig(js.JSON.parse(input))
    }
  }

}
