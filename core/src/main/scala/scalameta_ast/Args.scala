package scalameta_ast

import metaconfig.Conf

sealed abstract class Args extends Product with Serializable {
  def src: String
  def format: Boolean
  def scalafmtConfig: Conf
  def dialect: Option[String]
}

sealed abstract class NotToken extends Args {
  def removeNewFields: Boolean

  def initialExtractor: Boolean
}
sealed abstract class ScalafixRule extends NotToken {
  def packageName: Option[String]
  def wildcardImport: Boolean
  def ruleNameOption: Option[String]
  def patch: Option[String]
}
object Args {
  case class Token(
    src: String,
    format: Boolean,
    scalafmtConfig: Conf,
    dialect: Option[String],
  ) extends Args

  case class Raw(
    src: String,
    format: Boolean,
    scalafmtConfig: Conf,
    dialect: Option[String],
    removeNewFields: Boolean,
    initialExtractor: Boolean,
  ) extends NotToken

  case class Syntactic(
    src: String,
    format: Boolean,
    scalafmtConfig: Conf,
    dialect: Option[String],
    removeNewFields: Boolean,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleNameOption: Option[String],
    patch: Option[String],
    initialExtractor: Boolean,
  ) extends ScalafixRule

  case class Semantic(
    src: String,
    format: Boolean,
    scalafmtConfig: Conf,
    dialect: Option[String],
    removeNewFields: Boolean,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleNameOption: Option[String],
    patch: Option[String],
    initialExtractor: Boolean,
  ) extends ScalafixRule
}
