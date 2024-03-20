package scalameta_ast

sealed abstract class Args extends Product with Serializable {
  def src: String
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
    dialect: Option[String],
  ) extends Args

  case class Comment(
    src: String,
    dialect: Option[String],
  ) extends Args

  case class Raw(
    src: String,
    dialect: Option[String],
    removeNewFields: Boolean,
    initialExtractor: Boolean,
  ) extends NotToken

  case class Syntactic(
    src: String,
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
    dialect: Option[String],
    removeNewFields: Boolean,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleNameOption: Option[String],
    patch: Option[String],
    initialExtractor: Boolean,
  ) extends ScalafixRule
}
