package scalameta_ast

import scala.meta.Term

case class ParsedValue(value: () => Term, args: ScalafixRule)
