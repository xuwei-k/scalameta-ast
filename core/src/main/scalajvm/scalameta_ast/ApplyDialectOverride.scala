package scalameta_ast

import scala.annotation.unused
import scala.meta.Dialect

object ApplyDialectOverride {
  def patch(d: Dialect, @unused config: String): Dialect = d
}
