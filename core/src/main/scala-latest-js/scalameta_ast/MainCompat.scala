package scalameta_ast

import metaconfig.Conf
import org.ekrich.config.Config
import org.ekrich.config.ConfigFactory
import org.ekrich.config.ConfigRenderOptions
import org.scalafmt.config.ScalafmtConfig
import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.util.control.NonFatal

trait MainCompat {
  def runFormat(source: String, scalafmtConfig: Conf): Output[String] = {
    ScalametaAST.stopwatch {
      runFormat(
        source = source,
        conf = metaConfigToScalafmtConfig(scalafmtConfig)
      )
    }
  }

  private def runFormat(source: String, conf: ScalafmtConfig): String = {
    org.scalafmt.Scalafmt.format(source, conf).get
  }

  private def metaConfigToScalafmtConfig(conf: Conf): ScalafmtConfig = {
    ScalafmtConfig.decoder.read(None, conf).get
  }

  @nowarn("msg=never used")
  def formatImpl(source: String, scalafmtConfJsonStr: String): js.Object =
    try {
      val res = runFormat(
        source = source,
        scalafmtConfig = hoconToMetaConfig(scalafmtConfJsonStr)
      )
      new js.Object {
        val result: String = res.result
        val time: Double = res.time.toDouble
        val error = null
      }
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        new js.Object {
          val result: String = source
          val error = e.toString()
        }
    }

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
    case x: js.Array[?] =>
      Conf.Lst(x.map(a => convertToMetaConfig(a)).toList)
    case x: js.Object =>
      Conf.Obj(
        x.asInstanceOf[js.Dictionary[?]].map { case (k, v) => k -> convertToMetaConfig(v) }.toList
      )
    case _ =>
      Conf.Null()
  }

}
