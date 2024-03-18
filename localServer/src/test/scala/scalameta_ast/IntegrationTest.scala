package scalameta_ast

import com.microsoft.playwright._
import com.microsoft.playwright.options.AriaRole
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import unfiltered.jetty.Server
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Using

class IntegrationTestChromium extends IntegrationTest(_.chromium(), scalameta_ast.BrowserType.Chromium)
class IntegrationTestWebkit extends IntegrationTest(_.webkit(), scalameta_ast.BrowserType.Webkit)
class IntegrationTestFirefox extends IntegrationTest(_.firefox(), scalameta_ast.BrowserType.Firefox)

abstract class IntegrationTest(
  toBrowserType: Playwright => com.microsoft.playwright.BrowserType,
  browserType: scalameta_ast.BrowserType
) extends AnyFreeSpec
    with BeforeAndAfterAll {

  private var server: Server = null
  private var playwright: Playwright = null

  private def port(): Int = Option(server).map(_.ports.head).getOrElse(sys.error("server not started!?"))

  override def beforeAll(): Unit = {
    super.beforeAll()
    server = LocalServer.server(log = false)
    server.start()
    playwright = Playwright.create()
  }

  override def afterAll(): Unit = {
    if (server != null) {
      server.stop()
    }
    if (playwright != null) {
      playwright.close()
    }
    super.afterAll()
  }

  private def withBrowser[A](f: Page => A): Unit = {
    Using.resource(toBrowserType(playwright).launch()) { browser =>
      val context = browser.newContext()
      browserType match {
        case scalameta_ast.BrowserType.Chromium =>
          context.grantPermissions(
            java.util.List.of(
              "clipboard-read",
              "clipboard-write",
            )
          )
        case _ =>
      }
      val page = context.newPage()
      page.navigate(s"http://127.0.0.1:${port()}/")
      page.setDefaultTimeout(5000)
      var err: String = null
      page.onPageError { e =>
        err = e
      }
      try {
        f(page)
      } finally {
        clearLocalStorage(page)
      }
      if (err != null) {
        throw new AssertionError(s"page error: $err")
      }
    }
  }

  private def formatInput(page: Page): Unit = {
    clickButtonById(page, "format_input")
  }

  private def clickButtonById(page: Page, id: String): Unit = {
    getById(page, AriaRole.BUTTON, id).click()
  }

  private def clearLocalStorage(page: Page): Unit = {
    clickButtonById(page, "clear_local_storage")
  }

  private def output(page: Page): Locator = {
    getById(page, AriaRole.CODE, "output_scala")
  }

  private def getById(page: Page, role: AriaRole, id: String): Locator = {
    page.getByRole(role).all().asScala.find(_.getAttribute("id") == id).getOrElse(sys.error(s"not found $id"))
  }

  private def getTextboxById(page: Page, id: String): Locator =
    getById(page, AriaRole.TEXTBOX, id)

  private def scalafmtConfig(page: Page): Locator = {
    getTextboxById(page, "scalafmt")
  }

  private def setScalafmtConfig(page: Page, values: Seq[String]): Unit = {
    scalafmtConfig(page).fill(values.mkString("\n"))
  }

  private def addScalafmtConfig(page: Page, values: Seq[String]): Unit = {
    setScalafmtConfig(page, scalafmtConfig(page).inputValue() +: values)
  }

  private def inputElem(page: Page): Locator =
    getTextboxById(page, "input_scala")

  private def setInput(page: Page, sourceCode: String): Unit = {
    val input = inputElem(page)
    input.fill(sourceCode)
    input.press("\n")
  }

  private def formatOutput(page: Page): Locator = {
    getById(page, AriaRole.CHECKBOX, "format")
  }

  private def wildcardImport(page: Page): Locator = {
    getById(page, AriaRole.CHECKBOX, "wildcard_import")
  }

  private def removeNewFields(page: Page): Locator = {
    getById(page, AriaRole.CHECKBOX, "remove_new_fields")
  }

  private def initialExtractor(page: Page): Locator = {
    getById(page, AriaRole.CHECKBOX, "initial_extractor")
  }

  private def changeOutputType(page: Page, outputType: String): Unit = {
    page
      .getByRole(AriaRole.RADIO)
      .all()
      .asScala
      .find(_.getAttribute("value") == outputType)
      .getOrElse(sys.error(s"not found ${outputType}"))
      .check()
  }

  private def fromResource(path: String): String = {
    Source.fromURL(getClass.getResource("/" + path)).getLines().mkString("\n")
  }

  private def packageName(page: Page): Locator = {
    getTextboxById(page, "package")
  }

  private def ruleName(page: Page): Locator = {
    getTextboxById(page, "rule_name")
  }

  private def singleOrError[A](list: List[A]): A = {
    list match {
      case List(value) =>
        value
      case Nil =>
        sys.error("not found")
      case values =>
        sys.error(s"found multi values ${values}")
    }
  }

  private def selectedDialect(page: Page): String = {
    singleOrError(
      page.getByLabel("dialect").all().asScala.map(_.inputValue()).toList
    )
  }

  private def infoElem(page: Page): Locator = {
    singleOrError(
      page.locator("pre").all().asScala.filter(_.getAttribute("id") == "info").toList
    )
  }

  private def changeScalametaVersion(page: Page, v: ScalametaV): Unit = {
    page.selectOption(
      "select#scalameta",
      v match {
        case ScalametaV.V_0_10 =>
          "scalafix"
        case ScalametaV.V_0_11 =>
          "latest"
      }
    )
  }

  "change input" in withBrowser { page =>
    setInput(page, "class A")
    assert(infoElem(page).getAttribute("class") == "alert alert-success")
    val info = infoElem(page).textContent()
    Seq("ast: ", "fmt: ", " ms").forall(info contains _)
    val expect = Seq(
      """Defn.Class.After_4_6_0(""",
      """  Nil,""",
      """  Type.Name("A"),""",
      """  Type.ParamClause(Nil),""",
      """  Ctor.Primary""",
      """    .After_4_6_0(Nil, Name.Anonymous(), Nil),""",
      """  Template.After_4_4_0(""",
      """    Nil,""",
      """    Nil,""",
      """    Self(Name.Anonymous(), None),""",
      """    Nil,""",
      """    Nil""",
      """  )""",
      """)""",
      "",
    ).mkString("\n")
    assert(output(page).textContent() == expect)
  }

  "output type" - {
    "raw" in withBrowser { page =>
      changeOutputType(page, "raw")
      assert(output(page).textContent() == fromResource("raw.txt"))
      assert(!packageName(page).isEnabled())
      assert(!ruleName(page).isEnabled())
      assert(!wildcardImport(page).isEnabled())
    }
    "SyntacticRule" in withBrowser { page =>
      changeOutputType(page, "syntactic")
      assert(output(page).textContent() == fromResource("syntactic.txt"))
      assert(wildcardImport(page).isEnabled())
    }
    "SemanticRule" in withBrowser { page =>
      changeOutputType(page, "semantic")
      assert(output(page).textContent() == fromResource("semantic.txt"))
      assert(wildcardImport(page).isEnabled())
    }
    "Tokens" in withBrowser { page =>
      changeOutputType(page, "tokens")
      assert(output(page).textContent() == fromResource("tokens.txt"))
      assert(!wildcardImport(page).isEnabled())
      assert(!removeNewFields(page).isEnabled())
      assert(!initialExtractor(page).isEnabled())
      assert(!packageName(page).isEnabled())
      assert(!ruleName(page).isEnabled())
      setInput(
        page,
        "\"\\n\""
      )
      assert(
        output(page).textContent() == Seq(
          "Seq(",
          "  Token.BOF,",
          "  Token.Constant.String(\"\"\"",
          "\"\"\"),",
          "  Token.LF,",
          "  Token.EOF",
          ")",
          "",
        ).mkString("\n")
      )
    }
    "Comment" in withBrowser { page =>
      changeOutputType(page, "comment")
      assert(!wildcardImport(page).isEnabled())
      assert(!removeNewFields(page).isEnabled())
      assert(!initialExtractor(page).isEnabled())
      assert(!packageName(page).isEnabled())
      assert(!ruleName(page).isEnabled())
      setInput(
        page,
        Seq(
          """/**""",
          """ * @param a1 a2""",
          """ * @tparam a3 a4""",
          """ * @throws a5 a6""",
          """ * @see example.com""",
          """ * @note a7""",
          """ *""",
          """ * {{{""",
          """ *    def x = List("y", 2)""",
          """ * }}}""",
          """ */""",
        ).mkString("\n")
      )
      assert(output(page).textContent() == fromResource("comment.txt"))
    }
  }

  "format input" in withBrowser { page =>
    val notFormatted = Seq(
      """def a = """,
      """     b""",
    ).mkString("\n")
    setInput(page, notFormatted)
    val input1 = inputElem(page).inputValue()
    formatInput(page)
    val input2 = inputElem(page).inputValue()
    assert(input1 != input2)
    val formatted = Seq(
      """def a =""",
      """  b""",
      "",
    ).mkString("\n")
    assert(inputElem(page).inputValue() == formatted)
  }

  "change rule details" in withBrowser { page =>
    def render(): Unit = inputElem(page).press("\n")

    assert(!output(page).textContent().contains("import scala.meta._"))
    assert(!output(page).textContent().contains("package aaa"))
    assert(!output(page).textContent().contains("class OtherRuleName"))

    changeOutputType(page, "syntactic")
    output(page).textContent()
    packageName(page).fill("aaa")

    render()

    assert(output(page).textContent().contains("package aaa"))
    ruleName(page).fill("OtherRuleName")

    render()

    assert(output(page).textContent().contains("class OtherRuleName"))
    wildcardImport(page).check()

    render()

    assert(output(page).textContent().contains("import scala.meta._"))
  }

  "dialect" in withBrowser { page =>
    setInput(page, "enum A")
    changeOutputType(page, "raw")
    assert(output(page).textContent() contains """Term.Select(Term.Name("enum"), Term.Name("A"))""")
    page.selectOption("select#dialect", "Scala3")
    assert(selectedDialect(page) == "Scala3")
    assert(output(page).textContent() contains "Defn.Enum")
  }

  "patch" in withBrowser { page =>
    changeOutputType(page, "syntactic")
    assert(output(page).textContent().contains("LintSeverity.Warning"))
    page.selectOption("select#patch", "replace")
    assert(output(page).textContent().contains("Patch.replace"))
    page.selectOption("select#patch", "empty")
    assert(output(page).textContent().contains("Patch.empty"))
  }

  "scalameta version" in withBrowser { page =>
    changeOutputType(page, "raw")

    changeScalametaVersion(page, ScalametaV.V_0_11)
    assert(output(page).textContent().contains("Defn.Def.After_4_7_3("))

    changeScalametaVersion(page, ScalametaV.V_0_10)
    assert(!output(page).textContent().contains("Defn.Def.After_4_7_3("))
    assert(output(page).textContent().contains("Defn.Def("))
  }

  "scalafmt config" - {
    "input" in withBrowser { page =>
      setInput(page, "import foo._")
      formatInput(page)
      val input1 = inputElem(page).inputValue()
      assert(input1.contains("import foo._"))
      assert(!input1.contains("import foo.*"))
      setScalafmtConfig(
        page,
        Seq(
          """rewrite.scala3.convertToNewSyntax = true""",
          """runner.dialect = scala3"""
        )
      )
      formatInput(page)
      val input2 = inputElem(page).inputValue()
      assert(input1 != input2)
      assert(input2.contains("import foo.*"))
      assert(!input2.contains("import foo._"))
    }

    "output" in withBrowser { page =>
      def render(): Unit = inputElem(page).press("\n")
      changeOutputType(page, "syntactic")
      wildcardImport(page).check()
      render()
      val output1 = output(page).textContent()

      addScalafmtConfig(
        page,
        Seq(
          """rewrite.scala3.convertToNewSyntax = true""",
          """runner.dialect = scala3"""
        )
      )
      render()

      val output2 = output(page).textContent()
      val diff = output1.linesIterator
        .zip(output2.linesIterator)
        .filter { case (x1, x2) =>
          x1 != x2
        }
        .toList
      assert(
        diff == List(
          ("import scala.meta._", "import scala.meta.*"),
          ("import scala.meta.transversers._", "import scala.meta.transversers.*"),
        )
      )
    }
  }

  "Initial extractor" in withBrowser { page =>
    changeScalametaVersion(page, ScalametaV.V_0_10)
    assert(removeNewFields(page).isEnabled())
    assert(initialExtractor(page).isEnabled())

    changeOutputType(page, "raw")
    setScalafmtConfig(
      page,
      Seq(
        "maxColumn = 100"
      )
    )
    setInput(page, "if (a) 2 else 3")

    def outSingleLine(): String =
      output(page).textContent().linesIterator.next()

    assert(outSingleLine() == """Term.If(Term.Name("a"), Lit.Int(2), Lit.Int(3), Nil)""")

    removeNewFields(page).check()
    assert(outSingleLine() == """Term.If(Term.Name("a"), Lit.Int(2), Lit.Int(3))""")

    initialExtractor(page).check()
    assert(outSingleLine() == """Term.If.Initial(Term.Name("a"), Lit.Int(2), Lit.Int(3))""")

    changeScalametaVersion(page, ScalametaV.V_0_11)
    assert(!removeNewFields(page).isEnabled())
    assert(!initialExtractor(page).isEnabled())
  }

  "localStorage" - {
    "save values" in withBrowser { page =>
      def check(
        scalafmt: String,
        formatOut: Boolean,
        wildcard: Boolean,
        _removeNewFields: Boolean,
        _initialExtractor: Boolean,
        outputType: String,
        dialect: String,
        pkg: String,
        rule: String,
        input: String
      ) = {
        assert(scalafmtConfig(page).inputValue() == scalafmt)
        assert(formatOutput(page).isChecked == formatOut)
        assert(wildcardImport(page).isChecked == wildcard)
        assert(removeNewFields(page).isChecked == _removeNewFields)
        assert(initialExtractor(page).isChecked == _initialExtractor)
        assert(
          page.getByRole(AriaRole.RADIO).all().asScala.filter(_.isChecked).map(_.getAttribute("value")) == Seq(
            outputType
          )
        )
        assert(selectedDialect(page) == dialect)
        assert(packageName(page).inputValue() == pkg)
        assert(ruleName(page).inputValue() == rule)
        assert(inputElem(page).inputValue() == input)
      }

      changeOutputType(page, "syntactic")
      changeScalametaVersion(page, ScalametaV.V_0_10)

      check(
        scalafmt = Seq(
          """maxColumn = 50""",
          """runner.dialect = "Scala3"""",
          """align.preset = "none"""",
          """continuationIndent.defnSite = 2""",
          """continuationIndent.extendSite = 2""",
        ).mkString("\n"),
        formatOut = true,
        wildcard = false,
        _removeNewFields = false,
        _initialExtractor = false,
        outputType = "syntactic",
        dialect = "Auto",
        pkg = "fix",
        rule = "",
        input = "def a = b",
      )

      setScalafmtConfig(page, Seq("runner.dialect = Scala213"))
      formatOutput(page).uncheck()
      wildcardImport(page).check()
      initialExtractor(page).check()
      removeNewFields(page).check()
      changeOutputType(page, "semantic")
      page.selectOption("select#dialect", "Scala211")
      packageName(page).fill("ppppppppp")
      ruleName(page).fill("FFFFFFFFFF")
      setInput(page, "aaa")

      page.reload()

      check(
        scalafmt = "runner.dialect = Scala213",
        formatOut = false,
        wildcard = true,
        _removeNewFields = true,
        _initialExtractor = true,
        outputType = "semantic",
        dialect = "Scala211",
        pkg = "ppppppppp",
        rule = "FFFFFFFFFF",
        input = "aaa\n",
      )
    }

    "limit" in withBrowser { page =>
      val x = "x" * 1023
      setInput(page, x)
      page.reload()
      assert(inputElem(page).inputValue() == (x + "\n"))
      setInput(page, "n" * 1024)
      page.reload()
      assert(inputElem(page).inputValue() == (x + "\n"))
    }
  }

  "invalid input" in withBrowser { page =>
    setInput(page, "def")
    assert(infoElem(page).getAttribute("class") == "alert alert-danger")
    assert(infoElem(page).textContent() contains "error: identifier expected but end of file found")
  }

  "copy button" in withBrowser { page =>
    browserType match {
      case scalameta_ast.BrowserType.Chromium =>
        assert(page.evaluate("navigator.clipboard.readText()") == "")
        clickButtonById(page, "copy")
        assert(
          page.evaluate("navigator.clipboard.readText()") == Seq(
            """Defn.Def.After_4_7_3(""",
            """  Nil,""",
            """  Term.Name("a"),""",
            """  Nil,""",
            """  None,""",
            """  Term.Name("b")""",
            """)""",
            "",
          ).mkString("\n")
        )
      case _ =>
        pending
    }
  }
}
