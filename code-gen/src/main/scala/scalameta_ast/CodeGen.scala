package scalameta_ast

import scala.meta.Dialect

class CodeGen {
  private def quote(s: String): String = "\"" + s + "\""

  def run(): String = {
    val typeString = "Map[String, (Dialect, Boolean) => Dialect]"
    val map = values.map { case (k, v) =>
      s"    ${quote(k)} -> (_ `$v` _)"
    }.mkString(s"${typeString}(\n", ",\n", "\n  )")

    s"""|package scalameta_ast
        |
        |import scala.meta.Dialect
        |
        |object DialectOverride {
        |  val value: ${typeString} = $map
        |}
        |""".stripMargin
  }

  private def withPrefix = "with"

  private val exclude: Set[String] = Set(
    "withAllowTypeParamUnderscore",
    "withAllowQuestionMarkPlaceholder",
    "withAllowPlusMinusUnderscoreAsPlaceholder",
    "withAllowAndTypes",
    "withAllowOrTypes",
    "withAllowViewBounds",
  )

  private val methodNames: Seq[String] = classOf[Dialect].getMethods
    .filter(method =>
      (
        method.getName.startsWith(withPrefix)
      ) && (
        method.getParameterTypes.toSeq == Seq(classOf[Boolean])
      ) && (
        method.getReturnType == classOf[Dialect]
      ) && (
        java.lang.reflect.Modifier.isPublic(method.getModifiers)
      ) && (
        !exclude(method.getName)
      )
    )
    .map(_.getName)
    .toSeq

  private val values: Seq[(String, String)] =
    methodNames.map { name =>
      val x = name.drop(withPrefix.length)
      s"${x.head.toLower}${x.tail}" -> name
    }
}
