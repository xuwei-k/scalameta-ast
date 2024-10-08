package scalameta_ast

import java.util.Date
import scala.annotation.tailrec
import scala.meta._
import scala.meta.common.Convert
import scala.meta.parsers.Parse
import scala.meta.parsers.Parsed
import scala.meta.tokenizers.Tokenize

object ScalametaAST {
  def stopwatch[T](block: => T): Output[T] = {
    val begin = new Date()
    val result = block
    val end = new Date()
    val diffMs = end.getTime - begin.getTime
    Output(result, diffMs)
  }
}

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

  private def isValidTermName(str: String): Boolean = {
    implicitly[Parse[Term]].apply(Input.String(str), dialects.Scala3).toOption.exists(_.is[Term.Name])
  }

  private def addExtractor(parsed: Term, str: String, extractor: String => String): String = {
    val values = parsed.collect {
      case t @ Term.Select(Term.Name(x1), Term.Name(x2)) =>
        AfterExtractor.values.collect { case AfterExtractor.E2(`x1`, `x2`, e) =>
          t.pos -> e
        }
      case t @ Term.Name(x1) =>
        if (Set("Template", "Pkg").apply(x1) && str.splitAt(t.pos.end)._2.startsWith(".Body")) {
          Nil
        } else {
          AfterExtractor.values.collect { case AfterExtractor.E1(`x1`, e) =>
            t.pos -> e
          }
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
    pathFilter: Boolean,
  ): Output[String] = {
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
            pathFilter = pathFilter,
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
            pathFilter = pathFilter,
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
  ): Output[String] = {
    lazy val input = convert.apply(args.src)
    val result = ScalametaAST.stopwatch {
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
          TokensToString.tokensToString(loop(input, dialects)) -> None

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
          val str = ScalametaBug2921.convert(tree)
          lazy val parsed = implicitly[Parse[Term]].apply(Input.String(str), scala.meta.dialects.Scala3).get
          val parsedOpt = PartialFunction.condOpt(args) { case x: ScalafixRule =>
            ParsedValue(() => parsed, x)
          }

          if (AfterExtractor.enable) {
            val str2 = addExtractor(parsed = parsed, str = str, identity)
            val parsed2 = implicitly[Parse[Term]].apply(Input.String(str2), scala.meta.dialects.Scala3).get
            val parsedOpt2 = PartialFunction.condOpt(args) { case x: ScalafixRule =>
              ParsedValue(() => parsed2, x)
            }
            AddDefaultParam.addDefaultParam(parsed = parsed2, str = str2) -> parsedOpt2
          } else {
            if (a.removeNewFields) {
              val str2 = RemoveNewFields.remove(tree = tree, parsed = parsed, str = str)
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

    val ast = result.result._1
    val res = {
      result.result._2 match {
        case Some(a0) =>
          val ruleNameRaw = a0.args.ruleNameOption.getOrElse("Example").filterNot(char => char == '`' || char == '"')
          val ruleName = {
            if (isValidTermName(ruleNameRaw)) ruleNameRaw else s"`${ruleNameRaw}`"
          }

          val a = a0.args
          scalafixRule(
            x = ast,
            packageName = a.packageName,
            wildcardImport = a.wildcardImport,
            ruleName = ruleName,
            ruleNameRaw = ruleNameRaw,
            patch = a.patch,
            parsed = a0.value,
            explanation = a.explanation,
            pathFilter = a.pathFilter,
            documentClass = a.documentClass,
            ruleClass = a.ruleClass,
          )
        case _ =>
          ast
      }
    }
    Output(res, result.time)
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

  private def patchCode(patch: Option[String], explanation: Boolean, wildcardImport: Boolean): PatchValue = {
    def lint(serverity: String): PatchValue = PatchValue(
      imports = {
        if (wildcardImport) {
          List(
            "scalafix.lint.LintSeverity",
          )
        } else {
          List(
            "scalafix.lint.Diagnostic",
            "scalafix.lint.LintSeverity",
          )
        }
      },
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

  private def scalafixRule(
    x: String,
    packageName: Option[String],
    wildcardImport: Boolean,
    ruleName: String,
    ruleNameRaw: String,
    patch: Option[String],
    parsed: () => Term,
    explanation: Boolean,
    pathFilter: Boolean,
    documentClass: String,
    ruleClass: String,
  ): String = {
    val p = patchCode(patch = patch, explanation = explanation, wildcardImport = wildcardImport)
    val imports = List[List[String]](
      p.imports,
      if (wildcardImport) {
        Nil
      } else {
        List("scala.meta.transversers._")
      },
      if (pathFilter) {
        if (wildcardImport) {
          Nil
        } else {
          List("scala.meta.inputs.Input")
        }
      } else {
        Nil
      },
      if (wildcardImport) {
        List(
          "scalafix.v1._",
        )
      } else {
        List(
          "scalafix.Patch",
          s"scalafix.v1.${documentClass}",
          s"scalafix.v1.${ruleClass}",
          "scalafix.v1.XtensionSeqPatch",
        )
      }
    ).flatten.map("import " + _).sorted
    val body =
      s"""|    doc.tree.collect {
          |      case t @ ${x} =>
          |${p.value(8)}
          |    }.asPatch""".stripMargin
    val withPathFilter = if (pathFilter) {
      s"""|    doc.input match {
          |      case f: Input.VirtualFile if f.path.contains("src/main/scala") =>
          |        Patch.empty
          |      case _ =>
          |${body.linesIterator.map("    " + _).mkString("\n")}
          |    }""".stripMargin
    } else {
      body
    }
    s"""${header(x = x, packageName = packageName, wildcardImport = wildcardImport, parsed = parsed)}
       |${imports.mkString("\n")}
       |
       |class ${ruleName} extends ${ruleClass}("${ruleNameRaw}") {
       |  override def fix(implicit doc: ${documentClass}): Patch = {
       |$withPathFilter
       |  }
       |}
       |""".stripMargin
  }
}

case class Output[A](result: A, time: Long)
