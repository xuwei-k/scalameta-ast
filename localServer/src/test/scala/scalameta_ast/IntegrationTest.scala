package scalameta_ast

import com.microsoft.playwright._
import com.microsoft.playwright.options.AriaRole
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import unfiltered.jetty.Server
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Using

class IntegrationTest extends AnyFreeSpec with BeforeAndAfterAll {

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

  private def withBrowser[A](f: (Browser, Page) => A): Unit = {
    Seq(
      playwright.chromium(),
      playwright.webkit(),
      playwright.firefox(),
    ).foreach { browserType =>
      Using.resource(browserType.launch()) { browser =>
        val context = browser.newContext()
        val page = context.newPage()
        page.navigate(s"http://127.0.0.1:${port()}/")
        page.setDefaultTimeout(5000)
        try {
          f(browser, page)
        } finally {
          clearLocalStorage(page)
        }
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
    page.getByRole(role).all().asScala.find(_.getAttribute("id") == id).getOrElse(sys.error("not found"))
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

  private def wildcardImport(page: Page): Locator = {
    getById(page, AriaRole.CHECKBOX, "wildcard_import")
  }

  private def changeOutputType(page: Page, outputType: String): Unit = {
    page
      .getByRole(AriaRole.RADIO)
      .all()
      .asScala
      .find(_.getAttribute("id") == outputType)
      .getOrElse(sys.error(s"not found ${outputType}"))
      .check()
  }

  "change input" in withBrowser { (browser, page) =>
    setInput(page, "class A")
    val expect = Seq(
      """Defn.Class(""",
      """  Nil,""",
      """  Type.Name("A"),""",
      """  Nil,""",
      """  Ctor.Primary(Nil, Name(""), Nil),""",
      """  Template(""",
      """    Nil,""",
      """    Nil,""",
      """    Self(Name(""), None),""",
      """    Nil,""",
      """    Nil""",
      """  )""",
      """)""",
      "",
    ).mkString("\n")
    assert(output(page).textContent() == expect)
  }

  private def fromResource(path: String): String = {
    Source.fromURL(getClass.getResource("/" + path)).getLines().mkString("\n")
  }

  "output type" - {
    "raw" in withBrowser { (browser, page) =>
      changeOutputType(page, "raw")
      assert(output(page).textContent() == fromResource("raw.txt"))
    }
    "SyntacticRule" in withBrowser { (browser, page) =>
      changeOutputType(page, "syntactic")
      assert(output(page).textContent() == fromResource("syntactic.txt"))
    }
    "SemanticRule" in withBrowser { (browser, page) =>
      changeOutputType(page, "semantic")
      assert(output(page).textContent() == fromResource("semantic.txt"))
    }
    "Tokens" in withBrowser { (browser, page) =>
      changeOutputType(page, "tokens")
      assert(output(page).textContent() == fromResource("tokens.txt"))
    }
  }

  "format input" in withBrowser { (browser, page) =>
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

  "change rule details" in withBrowser { (browser, page) =>
    def render(): Unit = inputElem(page).press("\n")

    assert(!output(page).textContent().contains("import scala.meta._"))
    assert(!output(page).textContent().contains("package aaa"))
    assert(!output(page).textContent().contains("class OtherRuleName"))

    changeOutputType(page, "syntactic")
    output(page).textContent()
    getTextboxById(page, "package").fill("aaa")

    render()

    assert(output(page).textContent().contains("package aaa"))
    getTextboxById(page, "rule_name").fill("OtherRuleName")

    render()

    assert(output(page).textContent().contains("class OtherRuleName"))
    wildcardImport(page).check()

    render()

    assert(output(page).textContent().contains("import scala.meta._"))
  }

  "dialect" in withBrowser { (browser, page) =>
    setInput(page, "enum A")
    changeOutputType(page, "raw")
    assert(output(page).textContent() contains """Term.Select(Term.Name("enum"), Term.Name("A"))""")
    page.selectOption("select#dialect", "Scala3")
    assert(output(page).textContent() contains "Defn.Enum")
  }

  "patch" in withBrowser { (browser, page) =>
    changeOutputType(page, "syntactic")
    assert(output(page).textContent().contains("LintSeverity.Warning"))
    page.selectOption("select#patch", "replace")
    assert(output(page).textContent().contains("Patch.replace"))
    page.selectOption("select#patch", "empty")
    assert(output(page).textContent().contains("Patch.empty"))
  }

  "scalameta version" in withBrowser { (browser, page) =>
    changeOutputType(page, "raw")

    page.selectOption("select#scalameta", "latest")
    assert(output(page).textContent().contains("Defn.Def.After_4_7_3("))

    page.selectOption("select#scalameta", "scalafix")
    assert(!output(page).textContent().contains("Defn.Def.After_4_7_3("))
    assert(output(page).textContent().contains("Defn.Def("))
  }

  "scalafmt config" - {
    "input" in withBrowser { (browser, page) =>
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

    "output" in withBrowser { (browser, page) =>
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
          ("import scala.meta._", "import scala.meta.*")
        )
      )
    }
  }
}
