package scalameta_ast

import metaconfig.ConfDecoder
import metaconfig.generic.Surface

final case class FmtConfig(runner: FmtConfig.Runner)

object FmtConfig {
  val default: FmtConfig = FmtConfig(Runner.default)
  implicit val surface: Surface[FmtConfig] =
    metaconfig.generic.deriveSurface[FmtConfig]
  implicit val decoder: ConfDecoder[FmtConfig] =
    metaconfig.generic.deriveDecoder(default)

  final case class Runner(dialectOverride: Map[String, Boolean])

  object Runner {
    val default: Runner = Runner(Map.empty)
    implicit val surface: Surface[Runner] =
      metaconfig.generic.deriveSurface[Runner]
    implicit val decoder: ConfDecoder[Runner] =
      metaconfig.generic.deriveDecoder(default)
  }
}
