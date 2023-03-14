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
  private val parsers: List[(Parse[Tree], Dialect)] = for {
    x1 <- List(
      implicitly[Parse[Stat]],
      implicitly[Parse[Source]],
    ).map(_.asInstanceOf[Parse[Tree]])
    x2 <- List(
      dialects.Scala213Source3,
      dialects.Scala3,
    )
  } yield (x1, x2)

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
    packageName: Option[String]
  ): Output = {
    val input = convert.apply(src)
    val (ast, astBuildMs) = stopwatch {
      loop(input, parsers).structure
    }
    val (res, formatMs) = stopwatch {
      val ast0 = outputType match {
        case "semantic" =>
          semantic(ast, packageName)
        case "syntactic" =>
          syntactic(ast, packageName)
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

  private def syntactic(x: String, packageName: Option[String]): String = {
    val pkg = packageName.fold("")(x => s"package ${x}\n\n")

    s"""${pkg}${imports(x).mkString("\n")}
       |import scalafix.Patch
       |import scalafix.lint.Diagnostic
       |import scalafix.lint.LintSeverity
       |import scalafix.v1.SyntacticDocument
       |import scalafix.v1.SyntacticRule
       |
       |class Example extends SyntacticRule("Example") {
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

  private def semantic(x: String, packageName: Option[String]): String = {
    val pkg = packageName.fold("")(x => s"package ${x}\n\n")

    s"""${pkg}${imports(x).mkString("\n")}
       |import scalafix.Patch
       |import scalafix.lint.Diagnostic
       |import scalafix.lint.LintSeverity
       |import scalafix.v1.SemanticDocument
       |import scalafix.v1.SemanticRule
       |
       |class Example extends SemanticRule("Example") {
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
