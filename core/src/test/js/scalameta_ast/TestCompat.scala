package scalameta_ast

import scalajs.js.Dynamic

object TestCompat {
  private val fs = Dynamic.global.require("fs")
  def scalametaTreeFile(i: Int): String = {
    fs.readFileSync(
      s"target/out/jvm/scala-2.13.18/scalameta-ast/resource_managed/test/trees${i}.scala"
    ).toString
  }
}
