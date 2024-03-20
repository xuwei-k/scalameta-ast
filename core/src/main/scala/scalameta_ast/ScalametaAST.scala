package scalameta_ast

import java.util.Date
import scala.annotation.tailrec
import scala.meta._
import scala.meta.common.Convert
import scala.meta.contrib.XtensionTreeOps
import scala.meta.tokens.Token
import scala.meta.parsers.Parse
import scala.meta.parsers.Parsed
import scala.meta.tokenizers.Tokenize

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

  val topLevelScalametaDefinitions: Seq[Class[?]] = List(
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
          case s: Parsed.Success[?] =>
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
      def q(a: String): String = s"(${scala.meta.Lit.String(a)})"

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

  private def addExtractor(parsed: Term, str: String, extractor: String => String): String = {
    val values = parsed.collect {
      case t @ Term.Select(Term.Name(x1), Term.Name(x2)) =>
        AfterExtractor.values.collect { case AfterExtractor.E2(`x1`, `x2`, e) =>
          t.pos -> e
        }
      case t @ Term.Name(x1) =>
        AfterExtractor.values.collect { case AfterExtractor.E1(`x1`, e) =>
          t.pos -> e
        }
    }.flatten.sortBy(_._1.start)

    @tailrec
    def loop(acc: List[String], code: String, consumed: Int, src: List[(Position, String)]): List[String] = {
      src match {
        case head :: tail =>
          val (x1, x2) = code.splitAt(head._1.end - consumed)
          loop(
            s".${extractor(head._2)}" :: x1 :: acc,
            x2,
            x1.length + consumed,
            tail
          )
        case Nil =>
          (code :: acc).reverse
      }
    }

    loop(Nil, str, 0, values).mkString
  }

  private def removeModsFields(tree: Tree, parsed: Term, str: String): String = {
    if (
      tree.collectFirst {
        case _: Term.If => ()
        case _: Term.Match => ()
        case _: Defn.Type => ()
        case _: Template => ()
      }.nonEmpty
    ) {
      val positions = parsed.collect {
        case x @ Term.Apply(
              Term.Select(
                Term.Name("Term"),
                Term.Name("If")
              ),
              _ :: _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
        case x @ Term.Apply(
              Term.Select(
                Term.Name("Term"),
                Term.Name("Match")
              ),
              _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
        case x @ Term.Apply(
              Term.Select(
                Term.Name("Defn"),
                Term.Name("Type")
              ),
              _ :: _ :: _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
        case x @ Term.Apply(
              Term.Name("Template"),
              _ :: _ :: _ :: _ :: last :: Nil
            ) =>
          (x.tokens, last)
      }.flatMap { case (tokens, toRemove) =>
        val startOpt =
          tokens.reverseIterator.filter(_.is[Token.Comma]).find(_.pos.start < toRemove.pos.start).map(_.start)
        val endOpt =
          tokens.reverseIterator.find(_.is[Token.RightParen]).map(_.pos.end - 1)
        startOpt.zip(endOpt)
      }

      str.zipWithIndex.flatMap { case (char, pos) =>
        Option.unless(positions.exists { case (start, end) => start <= pos && pos < end }) {
          char
        }
      }.mkString
    } else {
      str
    }
  }

  def convert(
    src: String,
    outputType: String,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleNameOption: Option[String],
    dialect: Option[String],
    patch: Option[String],
    removeNewFields: Boolean,
    initialExtractor: Boolean,
    explanation: Boolean,
  ): Output = {
    convert(
      outputType match {
        case "tokens" =>
          Args.Token(
            src = src,
            dialect = dialect,
          )
        case "syntactic" =>
          Args.Syntactic(
            src = src,
            dialect = dialect,
            removeNewFields = removeNewFields,
            packageName = packageName,
            wildcardImport = wildcardImport,
            ruleNameOption = ruleNameOption,
            patch = patch,
            initialExtractor = initialExtractor,
            explanation = explanation,
          )
        case "semantic" =>
          Args.Semantic(
            src = src,
            dialect = dialect,
            removeNewFields = removeNewFields,
            packageName = packageName,
            wildcardImport = wildcardImport,
            ruleNameOption = ruleNameOption,
            patch = patch,
            initialExtractor = initialExtractor,
            explanation = explanation,
          )
        case "comment" =>
          Args.Comment(
            src = src,
            dialect = dialect,
          )
        case _ =>
          Args.Raw(
            src = src,
            dialect = dialect,
            removeNewFields = removeNewFields,
            initialExtractor = initialExtractor,
          )
      }
    )
  }
  def convert(
    args: Args
  ): Output = {
    lazy val input = convert.apply(args.src)
    val ((ast, parsedOpt), astBuildMs) = stopwatch {
      val dialects = args.dialect.fold(dialectsDefault) { x =>
        stringToDialects.getOrElse(
          x, {
            Console.err.println(s"invalid dialct ${x}")
            dialectsDefault
          }
        )
      }

      args match {
        case _: Args.Token =>
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
          tokensToString(loop(input, dialects)) -> None

        case _: Args.Comment =>
          scala.meta.contrib.CommentOps
            .docTokens(
              new scala.meta.tokens.Token.Comment(
                input = Input.String(args.src),
                dialect = dialects.head,
                start = 0,
                end = args.src.length,
                value = args.src
              )
            )
            .map(_.map { x =>
              val q: String => String = s => scala.meta.Lit.String(s).toString
              s"""DocToken(kind = ${x.kind}, name = ${x.name.map(q)}, body = ${x.body.map(q)})"""
            })
            .toString -> None

        case a: NotToken =>
          val tree = loopParse(
            input,
            for {
              x1 <- parsers
              x2 <- dialects
            } yield (x1, x2)
          )
          val str = scalametaBugWorkaround.foldLeft(tree.structure) { case (s, (x1, x2)) =>
            s.replace(x1, x2)
          }
          lazy val parsed = implicitly[Parse[Term]].apply(Input.String(str), scala.meta.dialects.Scala3).get
          val parsedOpt = PartialFunction.condOpt(args) { case x: ScalafixRule =>
            ParsedValue(() => parsed, x)
          }

          if (AfterExtractor.enable) {
            addExtractor(parsed = parsed, str = str, identity) -> parsedOpt
          } else {
            if (a.removeNewFields) {
              val str2 = removeModsFields(tree = tree, parsed = parsed, str = str)
              if (a.initialExtractor) {
                val parsed2 = implicitly[Parse[Term]].apply(Input.String(str2), scala.meta.dialects.Scala3).get
                val parsedOpt2 = PartialFunction.condOpt(args) { case x: ScalafixRule =>
                  ParsedValue(() => parsed2, x)
                }
                addExtractor(parsed = parsed2, str = str2, Function.const("Initial")) -> parsedOpt2
              } else {
                str2 -> parsedOpt
              }
            } else {
              str -> parsedOpt
            }
          }
      }
    }

    val (res, formatMs) = stopwatch {
      parsedOpt match {
        case Some(a0) =>
          val ruleNameRaw = a0.args.ruleNameOption.getOrElse("Example").filterNot(char => char == '`' || char == '"')
          val ruleName = {
            if (isValidTermName(ruleNameRaw)) ruleNameRaw else s"`${ruleNameRaw}`"
          }

          a0.args match {
            case a: Args.Semantic =>
              semantic(
                x = ast,
                packageName = a.packageName,
                wildcardImport = a.wildcardImport,
                ruleName = ruleName,
                ruleNameRaw = ruleNameRaw,
                patch = a.patch,
                parsed = a0.value,
                explanation = a.explanation,
              )
            case a: Args.Syntactic =>
              syntactic(
                x = ast,
                packageName = a.packageName,
                wildcardImport = a.wildcardImport,
                ruleName = ruleName,
                ruleNameRaw = ruleNameRaw,
                patch = a.patch,
                parsed = a0.value,
                explanation = a.explanation,
              )
          }
        case _ =>
          ast
      }
    }
    Output(res, astBuildMs, formatMs)
  }

  private def imports(src: String, parsed: Term): Seq[String] = {
    val names = parsed.collect { case Term.Name(x) => x }.toSet
    // TODO more better way
    val name = "Name"
    val termOrTypeNameCount = parsed.collect {
      case Term.Select(Term.Name("Term"), Term.Name("Name")) =>
        ()
      case Term.Select(Term.Name("Type"), Term.Name("Name")) =>
        ()
    }.size
    val nameCount = src.sliding(name.length).count(_ == name)
    val values = if (termOrTypeNameCount == nameCount) {
      topLevelScalametaDefinitions.filterNot(_ == classOf[scala.meta.Name])
    } else {
      topLevelScalametaDefinitions
    }
    values.map(_.getSimpleName).filter(names).map(x => s"import scala.meta.${x}")
  }

  private def header(x: String, packageName: Option[String], wildcardImport: Boolean, parsed: () => Term): String = {
    val pkg = packageName.fold("") { x =>
      s"package ${x.split('.').map(s => if (isValidTermName(s)) s else s"`${s}`").mkString(".")}\n\n"
    }
    val i = if (wildcardImport) "import scala.meta._" else imports(x, parsed.apply()).mkString("\n")
    s"${pkg}${i}"
  }

  private def patchCode(patch: Option[String], explanation: Boolean): PatchValue = {
    def lint(serverity: String): PatchValue = PatchValue(
      imports = List(
        "scalafix.lint.Diagnostic",
        "scalafix.lint.LintSeverity",
      ),
      value = indent => {
        List(
          """Patch.lint(""",
          """  Diagnostic(""",
          """    id = "",""",
          """    message = "",""",
          """    position = t.pos,""",
          if (explanation) """    explanation = "",""" else "",
          s"""    severity = LintSeverity.${serverity}""",
          """  )""",
          """)"""
        ).filter(_.nonEmpty).map(x => s"${" " * indent}$x").mkString("\n")
      }
    )

    patch.collect {
      case "error" =>
        lint("Error")
      case "info" =>
        lint("Info")
      case "left" =>
        PatchValue(imports = Nil, indent => s"""${" " * indent}Patch.addLeft(t, "")""")
      case "right" =>
        PatchValue(imports = Nil, indent => s"""${" " * indent}Patch.addRight(t, "")""")
      case "replace" =>
        PatchValue(imports = Nil, indent => s"""${" " * indent}Patch.replaceTree(t, "")""")
      case "empty" =>
        PatchValue(imports = Nil, indent => s"""${" " * indent}Patch.empty""")
      case "remove" =>
        PatchValue(imports = Nil, indent => s"""${" " * indent}Patch.removeTokens(t.tokens)""")
      case "around" =>
        PatchValue(imports = Nil, indent => s"""${" " * indent}Patch.addAround(t, "", "")""")
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
    parsed: () => Term,
    explanation: Boolean,
  ): String = {
    val p = patchCode(patch, explanation)
    val imports = List[List[String]](
      p.imports,
      if (wildcardImport) {
        Nil
      } else {
        List("scala.meta.transversers._")
      },
      List(
        "scalafix.Patch",
        "scalafix.v1.SyntacticDocument",
        "scalafix.v1.SyntacticRule",
        "scalafix.v1.XtensionSeqPatch",
      )
    ).flatten.map("import " + _).sorted
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport, parsed = parsed)}
       |${imports.mkString("\n")}
       |
       |class ${ruleName} extends SyntacticRule("${ruleNameRaw}") {
       |  override def fix(implicit doc: SyntacticDocument): Patch = {
       |    doc.tree.collect {
       |      case t @ ${x} =>
       |${p.value(8)}
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
    parsed: () => Term,
    explanation: Boolean,
  ): String = {
    val p = patchCode(patch, explanation)
    val imports = List[List[String]](
      p.imports,
      if (wildcardImport) {
        Nil
      } else {
        List("scala.meta.transversers._")
      },
      List(
        "scalafix.Patch",
        "scalafix.v1.SemanticDocument",
        "scalafix.v1.SemanticRule",
        "scalafix.v1.XtensionSeqPatch",
      )
    ).flatten.map("import " + _).sorted
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport, parsed = parsed)}
       |${imports.mkString("\n")}
       |
       |class ${ruleName} extends SemanticRule("${ruleNameRaw}") {
       |  override def fix(implicit doc: SemanticDocument): Patch = {
       |    doc.tree.collect {
       |      case t @ ${x} =>
       |${p.value(8)}
       |    }.asPatch
       |  }
       |}
       |""".stripMargin
  }
}

case class Output(ast: String, astBuildMs: Long, formatMs: Long)
