package scalameta_ast

import scalajs.js.Dynamic

object TestCompat {
  private val fs = Dynamic.global.require("fs")
  def scalametaTreeFile(i: Int): String = {
    fs.readFileSync(s"core/target/jvm-2.13/resource_managed/test/trees${i}.scala").toString
  }
}
