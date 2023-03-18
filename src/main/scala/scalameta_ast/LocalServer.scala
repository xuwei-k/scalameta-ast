package scalameta_ast

import unfiltered.request._
import unfiltered.response._

object LocalServer {
  def main(args: Array[String]): Unit = {

    unfiltered.jetty.Server.anylocal
      .plan(
        unfiltered.filter.Planify { case Path(p) =>
          val path = if (p == "/") "index.html" else s".$p"
          val res = ResponseString(scala.io.Source.fromFile(path).getLines.mkString("\n"))
          if (path.endsWith(".html")) {
            HtmlContent ~> res
          } else if (path.endsWith(".js")) {
            JsContent ~> res
          } else {
            res
          }
        }
      )
      .run { svr =>
        unfiltered.util.Browser.open(svr.portBindings.head.url)
      }
  }
}
