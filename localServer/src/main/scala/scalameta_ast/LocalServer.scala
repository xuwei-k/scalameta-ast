package scalameta_ast

import unfiltered.jetty.Server
import unfiltered.request.Path
import unfiltered.response.HtmlContent
import unfiltered.response.JsContent
import unfiltered.response.JsonContent
import unfiltered.response.NotFound
import unfiltered.response.Ok
import unfiltered.response.ResponseString
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.annotation.nowarn

@nowarn("msg=a type was inferred to be")
object LocalServer {

  def server(log: Boolean): Server = {
    unfiltered.jetty.Server.anylocal.plan(
      unfiltered.filter.Planify { case Path(p) =>
        if (log) {
          println(s"${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())} ${p}")
        }
        val f = new File("sources", if (p == "/") "index.html" else s".$p")
        if (f.isFile) {
          val path = f.toPath
          val res = ResponseString(Files.readString(path, StandardCharsets.UTF_8))
          p.split('.')
            .lastOption
            .collect {
              case "html" =>
                HtmlContent
              case "js" =>
                JsContent
              case "json" =>
                JsonContent
            }
            .map(_ ~> res)
            .getOrElse(res)
        } else if (p == "/favicon.ico") {
          Ok
        } else {
          NotFound
        }
      }
    )
  }

  def main(args: Array[String]): Unit = {
    server(log = true).run { svr =>
      unfiltered.util.Browser.open(svr.portBindings.head.url)
    }
  }
}
