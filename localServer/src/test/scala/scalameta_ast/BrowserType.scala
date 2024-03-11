package scalameta_ast

sealed abstract class BrowserType extends Product with Serializable

object BrowserType {
  case object Chromium extends BrowserType
  case object Webkit extends BrowserType
  case object Firefox extends BrowserType
}
