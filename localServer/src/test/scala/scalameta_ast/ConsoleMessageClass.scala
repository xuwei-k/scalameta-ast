package scalameta_ast

import com.microsoft.playwright.ConsoleMessage

case class ConsoleMessageClass(
  location: String,
  text: String,
  messageType: String
)

object ConsoleMessageClass {
  def from(message: ConsoleMessage): ConsoleMessageClass = apply(
    location = message.location(),
    text = message.text(),
    messageType = message.`type`()
  )
}
