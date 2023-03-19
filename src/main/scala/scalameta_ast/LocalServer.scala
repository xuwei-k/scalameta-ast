package scalameta_ast

import unfiltered.request.Path
import unfiltered.response.HtmlContent
import unfiltered.response.JsContent
import unfiltered.response.NotFound
import unfiltered.response.Ok
import unfiltered.response.ResponseString
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.annotation.nowarn

object LocalServer {
  @nowarn("msg=a type was inferred to be")
  def main(args: Array[String]): Unit = {
    unfiltered.jetty.Server.anylocal
      .plan(
        unfiltered.filter.Planify { case Path(p) =>
          println(s"${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())} ${p}")
          val f = new File("sources", if (p == "/") "index.html" else s".$p")
          if (f.isFile) {
            val path = f.toPath
            val res = ResponseString(Files.readString(path, StandardCharsets.UTF_8))
            if (p.endsWith(".html")) {
              HtmlContent ~> res
            } else if (p.endsWith(".js")) {
              JsContent ~> res
            } else {
              res
            }
          } else if (p == "/favicon.ico") {
            Ok
          } else {
            NotFound
          }
        }
      )
      .run { svr =>
        unfiltered.util.Browser.open(svr.portBindings.head.url)
      }
  }
}
