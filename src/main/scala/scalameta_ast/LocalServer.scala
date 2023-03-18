package scalameta_ast

import unfiltered.request.Path
import unfiltered.response.HtmlContent
import unfiltered.response.JsContent
import unfiltered.response.ResponseString
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

object LocalServer {
  def main(args: Array[String]): Unit = {
    unfiltered.jetty.Server.anylocal
      .plan(
        unfiltered.filter.Planify { case Path(p) =>
          val path = new File("sources", if (p == "/") "index.html" else s".$p").toPath
          val res = ResponseString(Files.readString(path, StandardCharsets.UTF_8))
          if (p.endsWith(".html")) {
            HtmlContent ~> res
          } else if (p.endsWith(".js")) {
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
