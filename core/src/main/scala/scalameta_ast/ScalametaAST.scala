package scalameta_ast

import metaconfig.Conf
import org.scalafmt.config.ScalafmtConfig
import java.util.Date
import scala.annotation.tailrec
import scala.meta._
import scala.meta.common.Convert
import scala.meta.parsers.Parse
import scala.meta.parsers.Parsed
import scala.meta.tokenizers.Tokenize
import scala.meta.tokens.Token
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
  private def loopParse(input: Input, xs: List[(Parse[Tree], Dialect)]): Tree = {
    (xs: @unchecked) match {
      case (parse, dialect) :: t1 :: t2 =>
        parse.apply(input, dialect) match {
          case s: Parsed.Success[_] =>
            s.get
          case _: Parsed.Error =>
            loopParse(input, t1 :: t2)
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

  // TODO remove when scalafix depends on new scalameta version
  // https://github.com/scalameta/scalameta/pull/2921
  private[this] val scalametaBugWorkaround: Seq[(String, String)] = Seq(
    "Lit.Unit(())" -> "Lit.Unit()",
    "Lit.Null(null)" -> "Lit.Null()"
  )

  private def isValidTermName(str: String): Boolean = {
    implicitly[Parse[Term]].apply(Input.String(str), dialects.Scala3).toOption.exists(_.is[Term.Name])
  }

  private def tokensToString(tokens: Tokens): String = {
    tokens.tokens.map { x =>
      val n = x.getClass.getSimpleName
      def q(a: String): String = s"(\"${a}\")"

      PartialFunction
        .condOpt(x) {
          case y: Token.Ident =>
            s"Ident${q(y.value)}"
          case y: Token.Comment =>
            s"Comment${q(y.value)}"
          case y: Token.Interpolation.Id =>
            s"Interpolation.Id${q(y.value)}"
          case y: Token.Interpolation.Part =>
            s"Interpolation.Part${q(y.value)}"
          case _: Token.Interpolation.Start =>
            s"Interpolation.$n"
          case _: Token.Interpolation.SpliceStart =>
            s"Interpolation.$n"
          case _: Token.Interpolation.SpliceEnd =>
            s"Interpolation.$n"
          case _: Token.Interpolation.End =>
            s"Interpolation.$n"
          case y: Token.Constant[?] =>
            y match {
              case z: Token.Constant.Int =>
                s"Constant.Int(BigInt${q(z.value.toString)})"
              case z: Token.Constant.Long =>
                s"Constant.Long(BigInt${q(z.value.toString)})"
              case z: Token.Constant.Float =>
                s"Constant.Float(BigDecimal${q(z.value.toString)})"
              case z: Token.Constant.Double =>
                s"Constant.Double(BigDecimal${q(z.value.toString)})"
              case z: Token.Constant.Char =>
                s"Constant.Char('${z.value}')"
              case z: Token.Constant.Symbol =>
                s"Constant.Symbol(scala.Symbol${q(z.value.name)})"
              case z: Token.Constant.String =>
                s"Constant.String${q(z.value)}"
            }
          case _: Token.Xml.Start =>
            s"Xml.${n}"
          case y: Token.Xml.Part =>
            s"Xml.Part${q(y.value)}"
          case _: Token.Xml.SpliceStart =>
            s"Xml.${n}"
          case _: Token.Xml.SpliceEnd =>
            s"Xml.${n}"
          case _: Token.Xml.End =>
            s"Xml.${n}"
          case _: Token.Indentation.Indent =>
            s"Indentation.${n}"
          case _: Token.Indentation.Outdent =>
            s"Indentation.${n}"
        }
        .getOrElse(n)
    }.map("Token." + _).mkString("Seq(", ", ", ")")
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
    patch: Option[String],
  ): Output = {
    val input = convert.apply(src)
    val (ast, astBuildMs) = stopwatch {
      val dialects = dialect.fold(dialectsDefault) { x =>
        stringToDialects.getOrElse(
          x, {
            Console.err.println(s"invalid dialct ${x}")
            dialectsDefault
          }
        )
      }

      outputType match {
        case "tokens" =>
          @tailrec
          def loop(input: Input, xs: List[Dialect]): Tokens = {
            (xs: @unchecked) match {
              case d :: t1 :: t2 =>
                implicitly[Tokenize].apply(input, d).toEither match {
                  case Right(tokens) =>
                    tokens
                  case Left(_) =>
                    loop(input, t1 :: t2)
                }
              case d :: Nil =>
                implicitly[Tokenize].apply(input, d).get
            }
          }
          tokensToString(loop(input, dialects))

        case _ =>
          val tree = loopParse(
            input,
            for {
              x1 <- parsers
              x2 <- dialects
            } yield (x1, x2)
          )
          scalametaBugWorkaround.foldLeft(tree.structure) { case (s, (x1, x2)) =>
            s.replace(x1, x2)
          }
      }
    }

    val ruleNameRaw = ruleNameOption.getOrElse("Example").filterNot(char => char == '`' || char == '"')
    val ruleName = {
      if (isValidTermName(ruleNameRaw)) ruleNameRaw else s"`${ruleNameRaw}`"
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
            patch = patch,
          )
        case "syntactic" =>
          syntactic(
            x = ast,
            packageName = packageName,
            wildcardImport = wildcardImport,
            ruleName = ruleName,
            ruleNameRaw = ruleNameRaw,
            patch = patch,
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
    val pkg = packageName.fold("") { x =>
      s"package ${x.split('.').map(s => if (isValidTermName(s)) s else s"`${s}`").mkString(".")}\n\n"
    }
    val i = if (wildcardImport) "import scala.meta._" else imports(x).mkString("\n")
    s"${pkg}${i}"
  }

  private def patchCode(patch: Option[String]): PatchValue = {
    def lint(serverity: String): PatchValue = PatchValue(
      imports = List(
        "scalafix.lint.Diagnostic",
        "scalafix.lint.LintSeverity",
      ),
      value = s"""Patch.lint(
           |  Diagnostic(
           |    id = "",
           |    message = "",
           |    position = t.pos,
           |    explanation = "",
           |    severity = LintSeverity.${serverity}
           |  )
           |)""".stripMargin
    )

    patch.collect {
      case "error" =>
        lint("Error")
      case "info" =>
        lint("Info")
      case "left" =>
        PatchValue(imports = Nil, """Patch.addLeft(t, "")""")
      case "right" =>
        PatchValue(imports = Nil, """Patch.addRight(t, "")""")
      case "replace" =>
        PatchValue(imports = Nil, """Patch.replaceTree(t, "")""")
      case "empty" =>
        PatchValue(imports = Nil, "Patch.empty")
    }.getOrElse {
      lint("Warning")
    }
  }

  private def syntactic(
    x: String,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleName: String,
    ruleNameRaw: String,
    patch: Option[String],
  ): String = {
    val p = patchCode(patch)
    val imports = List[List[String]](
      List("scalafix.Patch"),
      p.imports,
      List(
        "scalafix.v1.SyntacticDocument",
        "scalafix.v1.SyntacticRule",
      )
    ).flatten.map("import " + _)
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport)}
       |${imports.mkString("\n")}
       |
       |class ${ruleName} extends SyntacticRule("${ruleNameRaw}") {
       |  override def fix(implicit doc: SyntacticDocument): Patch = {
       |    doc.tree.collect {
       |      case t @ ${x} =>
       |        ${p.value}
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
    patch: Option[String],
  ): String = {
    val p = patchCode(patch)
    val imports = List[List[String]](
      List("scalafix.Patch"),
      p.imports,
      List(
        "scalafix.v1.SemanticDocument",
        "scalafix.v1.SemanticRule",
      )
    ).flatten.map("import " + _)
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport)}
       |${imports.mkString("\n")}
       |
       |class ${ruleName} extends SemanticRule("${ruleNameRaw}") {
       |  override def fix(implicit doc: SemanticDocument): Patch = {
       |    doc.tree.collect {
       |      case t @ ${x} =>
       |        ${p.value}
       |    }.asPatch
       |  }
       |}
       |""".stripMargin
  }
}

case class Output(ast: String, astBuildMs: Long, formatMs: Long)
