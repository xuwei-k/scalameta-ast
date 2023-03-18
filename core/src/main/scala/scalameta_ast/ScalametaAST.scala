package scalameta_ast

import metaconfig.Conf
import org.scalafmt.config.ScalafmtConfig
import java.util.Date
import scala.annotation.tailrec
import scala.meta._
import scala.meta.common.Convert
import scala.meta.parsers.Parse
import scala.meta.parsers.Parsed
import scala.util.control.NonFatal

class ScalametaAST {
  private val dialectsDefault = List(dialects.Scala213Source3, dialects.Scala3)
  private val stringToDialects: Map[String, List[Dialect]] = {
    import dialects._
    Map(
      "Auto" -> dialectsDefault,
      "Scala3" -> List(Scala3),
      "Scala213Source3" -> List(Scala213Source3),
      "Scala213" -> List(Scala213),
      "Scala212Source3" -> List(Scala212Source3),
      "Scala212" -> List(Scala212),
      "Scala211" -> List(Scala211),
      "Scala210" -> List(Scala210),
    ).view
      .mapValues(
        _.map(_.withAllowToplevelTerms(true).withAllowToplevelStatements(true))
      )
      .toMap
  }
  private val parsers: List[Parse[Tree]] = List(
    implicitly[Parse[Stat]],
    implicitly[Parse[Source]],
  ).map(_.asInstanceOf[Parse[Tree]])

  val topLevelScalametaDefinitions: Seq[Class[_]] = List(
    classOf[Lit],
    classOf[MultiSource],
    classOf[CaseTree],
    classOf[Import],
    classOf[Mod],
    classOf[Name],
    classOf[Case],
    classOf[Ctor],
    classOf[Importee],
    classOf[Init],
    classOf[Term],
    classOf[Decl],
    classOf[Importer],
    classOf[Defn],
    classOf[Tree],
    classOf[Enumerator],
    classOf[Export],
    classOf[Pat],
    classOf[Pkg],
    classOf[Template],
    classOf[Member],
    classOf[TypeCase],
    classOf[ImportExportStat],
    classOf[Source],
    classOf[Type],
    classOf[Ref],
    classOf[Self],
    classOf[Stat]
  ).distinct.sortBy(_.getName)

  @tailrec
  private def loop(input: Input, xs: List[(Parse[Tree], Dialect)]): Tree = {
    (xs: @unchecked) match {
      case (parse, dialect) :: t1 :: t2 =>
        parse.apply(input, dialect) match {
          case s: Parsed.Success[_] =>
            s.get
          case _: Parsed.Error =>
            loop(input, t1 :: t2)
        }
      case (parse, dialect) :: Nil =>
        parse.apply(input, dialect).get
    }
  }

  private val convert = implicitly[Convert[String, Input]]

  private def stopwatch[T](block: => T): (T, Long) = {
    val begin = new Date()
    val result = block
    val end = new Date()
    val diffMs = end.getTime - begin.getTime
    (result, diffMs)
  }

  def runFormat(source: String, scalafmtConfig: Conf): String =
    runFormat(
      source = source,
      conf = metaConfigToScalafmtConfig(scalafmtConfig)
    )

  private def runFormat(source: String, conf: ScalafmtConfig): String = {
    try {
      org.scalafmt.Scalafmt.format(source, conf).get
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        source
    }
  }

  private def metaConfigToScalafmtConfig(conf: Conf): ScalafmtConfig = {
    ScalafmtConfig.decoder.read(None, conf).get
  }

  def convert(
    src: String,
    format: Boolean,
    scalafmtConfig: Conf,
    outputType: String,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleNameOption: Option[String],
    dialect: Option[String],
  ): Output = {
    val input = convert.apply(src)
    val (ast, astBuildMs) = stopwatch {
      loop(
        input,
        for {
          x1 <- parsers
          x2 <- dialect.fold(dialectsDefault) { x =>
            stringToDialects.getOrElse(
              x, {
                Console.err.println(s"invalid dialct ${x}")
                dialectsDefault
              }
            )
          }
        } yield (x1, x2)
      ).structure
    }
    val ruleNameRaw = ruleNameOption.getOrElse("Example").filterNot(char => char == '`' || char == '"')
    val ruleName = {
      val isValid =
        implicitly[Parse[Term]].apply(Input.String(ruleNameRaw), dialects.Scala3).toOption.exists(_.is[Term.Name])
      if (isValid) ruleNameRaw else s"`${ruleNameRaw}`"
    }

    val (res, formatMs) = stopwatch {
      val ast0 = outputType match {
        case "semantic" =>
          semantic(
            x = ast,
            packageName = packageName,
            wildcardImport = wildcardImport,
            ruleName = ruleName,
            ruleNameRaw = ruleNameRaw,
          )
        case "syntactic" =>
          syntactic(
            x = ast,
            packageName = packageName,
            wildcardImport = wildcardImport,
            ruleName = ruleName,
            ruleNameRaw = ruleNameRaw,
          )
        case _ =>
          ast
      }
      if (format) {
        runFormat(source = ast0, scalafmtConfig)
      } else {
        ast0
      }
    }
    Output(res, astBuildMs, formatMs)
  }

  private def imports(src: String): Seq[String] = {
    // TODO more better way
    val termName = "Term.Name"
    val typeName = "Type.Name"
    val name = "Name"
    val termOrTypeNameCount = src.sliding(termName.length).count(x => x == termName || x == typeName)
    val nameCount = src.sliding(name.length).count(_ == name)
    val values = if (termOrTypeNameCount == nameCount) {
      topLevelScalametaDefinitions.filterNot(_ == classOf[scala.meta.Name])
    } else {
      topLevelScalametaDefinitions
    }
    values.map(_.getSimpleName).filter(src.contains).map(x => s"import scala.meta.${x}")
  }

  private def header(x: String, packageName: Option[String], wildcardImport: Boolean): String = {
    val pkg = packageName.fold("")(x => s"package ${x}\n\n")
    val i = if (wildcardImport) "import scala.meta._" else imports(x).mkString("\n")
    s"${pkg}${i}"
  }

  private def syntactic(
    x: String,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleName: String,
    ruleNameRaw: String,
  ): String = {
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport)}
       |import scalafix.Patch
       |import scalafix.lint.Diagnostic
       |import scalafix.lint.LintSeverity
       |import scalafix.v1.SyntacticDocument
       |import scalafix.v1.SyntacticRule
       |
       |class ${ruleName} extends SyntacticRule("${ruleNameRaw}") {
       |  override def fix(implicit doc: SyntacticDocument): Patch = {
       |    doc.tree.collect {
       |      case t @ ${x} =>
       |        Patch.lint(
       |          Diagnostic(
       |            id = "",
       |            message = "",
       |            position = t.pos,
       |            explanation = "",
       |            severity = LintSeverity.Warning
       |          )
       |        )
       |    }.asPatch
       |  }
       |}
       |""".stripMargin
  }

  private def semantic(
    x: String,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleName: String,
    ruleNameRaw: String,
  ): String = {
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport)}
       |import scalafix.Patch
       |import scalafix.lint.Diagnostic
       |import scalafix.lint.LintSeverity
       |import scalafix.v1.SemanticDocument
       |import scalafix.v1.SemanticRule
       |
       |class ${ruleName} extends SemanticRule("${ruleNameRaw}") {
       |  override def fix(implicit doc: SemanticDocument): Patch = {
       |    doc.tree.collect {
       |      case t @ ${x} =>
       |        Patch.lint(
       |          Diagnostic(
       |            id = "",
       |            message = "",
       |            position = t.pos,
       |            explanation = "",
       |            severity = LintSeverity.Warning
       |          )
       |        )
       |    }.asPatch
       |  }
       |}
       |""".stripMargin
  }
}

case class Output(ast: String, astBuildMs: Long, formatMs: Long)
