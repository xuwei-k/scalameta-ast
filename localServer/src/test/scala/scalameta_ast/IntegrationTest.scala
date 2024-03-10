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

  private def withBrowser[A](f: (Browser, Page) => A): A = {
    Using.resource(playwright.chromium().launch()) { browser =>
      val context = browser.newContext()
      val page = context.newPage()
      page.navigate(s"http://127.0.0.1:${port()}/")
      clearLocalStorage(page)
      try {
        f(browser, page)
      } finally {
        clearLocalStorage(page)
      }
    }
  }

  private def clearLocalStorage(page: Page): Unit = {
    page.getByRole(AriaRole.BUTTON).all().asScala.find(_.getAttribute("id") == "clear_local_storage").foreach(_.click())
  }

  private def output(page: Page): Locator = {
    page
      .getByRole(AriaRole.CODE)
      .all()
      .asScala
      .find(_.getAttribute("id") == "output_scala")
      .getOrElse(sys.error("not found"))
  }

  private def setInput(page: Page, sourceCode: String): Unit = {
    val input = page
      .getByRole(AriaRole.TEXTBOX)
      .all()
      .asScala
      .find(_.getAttribute("id") == "input_scala")
      .getOrElse(sys.error("not found"))
    input.fill(sourceCode)
    input.press("\n")
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
}
