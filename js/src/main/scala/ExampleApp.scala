import scala.scalajs.js.JSApp

import org.scalajs.dom
object ExampleApp extends JSApp {

  def main(): Unit = {
    dom.document.body.innerHTML = "<h4> Hello World! </h4>"
  }
}