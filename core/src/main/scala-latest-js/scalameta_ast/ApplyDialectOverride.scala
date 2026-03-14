package scalameta_ast

import scala.meta.Dialect

object ApplyDialectOverride {
  def patch(d: Dialect, configString: String): Dialect = {
    val conf = MainCompat.hoconToMetaConfig(configString)
    val overrides = FmtConfig.decoder.read(conf).get.runner.dialectOverride
    overrides.foldLeft(d) { case (x, (k, v)) =>
      DialectOverride.value.get(k).fold(x)(_.apply(x, v))
    }
  }
}
