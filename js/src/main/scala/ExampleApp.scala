import org.scalajs.dom.html
import org.scalajs.dom.raw.{WebGLBuffer, WebGLProgram, WebGLRenderingContext}

import scala.scalajs.js
import scala.scalajs.js.JSApp

import org.scalajs.dom._
import scala.scalajs.js.typedarray._
import org.scalajs._
import dom._

object ExampleApp extends JSApp {

  import raw.WebGLRenderingContext._

  def vmain(glCode: String) = {
    s"void main(void) { ${glCode} }"
  }

  def uniformFragmentShader(gl: WebGLRenderingContext)(r: Float, g: Float, b: Float, a: Float) = {
    val fragmentShader = gl.createShader(FRAGMENT_SHADER)
    val fragText = "precision highp float;" + "uniform vec4 color;" + vmain(s"gl_FragColor = vec4($r, $g, $b, $a);")
    gl.shaderSource(fragmentShader, fragText)
    gl.compileShader(fragmentShader)
    fragmentShader
  }

  def varyingFragmentShader(gl: WebGLRenderingContext) = {
    val fragmentShader = gl.createShader(FRAGMENT_SHADER)
    val fragText = "varying lowp vec4 vColor;" + vmain(s"gl_FragColor = vColor;")
    gl.shaderSource(fragmentShader, fragText)
    gl.compileShader(fragmentShader)
    fragmentShader
  }

  def uniformVertexShader(gl: WebGLRenderingContext) = {
    val vertexShader = gl.createShader(VERTEX_SHADER)
    val vertexText = "attribute vec3 position;" + vmain("gl_Position = vec4(aVertexPosition, 1);")
    gl.shaderSource(vertexShader, vertexText)
    gl.compileShader(vertexShader)
    vertexShader
  }

  def varyingVertexShader(gl: WebGLRenderingContext) = {
    val vertexShader = gl.createShader(VERTEX_SHADER)
    val vertexText = "attribute vec3 aVertexPosition;" +
      "attribute vec4 aVertexColor;" +
      "varying lowp vec4 vColor;" +
      vmain("gl_Position = vec4(aVertexPosition, 1);" +
        "vColor = aVertexColor;")

    gl.shaderSource(vertexShader, vertexText)
    gl.compileShader(vertexShader)
    vertexShader
  }

  def initShaders(gl: WebGLRenderingContext): WebGLProgram = {

    val program = gl.createProgram()
    gl.attachShader(program, varyingVertexShader(gl))
    gl.attachShader(program, varyingFragmentShader(gl))
    gl.linkProgram(program)
    program
  }

  def main(): Unit = {
    val canvas: html.Canvas = document.createElement("canvas").asInstanceOf[html.Canvas]
    document.body.appendChild(canvas)
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    val gl: raw.WebGLRenderingContext = canvas.getContext("webgl").asInstanceOf[raw.WebGLRenderingContext]
    gl.clearColor(0.0, 0.0, 0.0, 1.0)
    gl.clearDepth(1.0)
    gl.enable(DEPTH_TEST)
    gl.depthFunc(LEQUAL)


    var rotation = 0
    js.timers.setInterval(15){
      rotation = if(rotation == Int.MaxValue) 0 else rotation + 1
      renderScene(gl)(rotation)
    }
  }

  def rotMod(initial: Float, rotation: Int): Float = {

    initial * Math.sin(Math.toRadians(rotation)).toFloat
  }
  def renderScene(gl: WebGLRenderingContext)(rotation: Int) = {
    gl.clear(COLOR_BUFFER_BIT)
    gl.clear(DEPTH_BUFFER_BIT)

    val colorBuffer = initColorBuffer(gl)

    val squareVerticesBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, squareVerticesBuffer)
    val vertices: Float32Array = new Float32Array(js.Array(rotMod(-0.3f,rotation), -0.3f, 0.0f,
                                                           rotMod(0.3f,rotation), -0.3f, 0.0f,
                                                           rotMod(-0.3f,rotation),  0.3f, 0.0f,
                                                           rotMod(0.3f,rotation),  0.3f, 0.0f))
    gl.bufferData(ARRAY_BUFFER, vertices, STATIC_DRAW)

    val program = initShaders(gl)
    gl.useProgram(program)

    val positionIndex = gl.getAttribLocation(program, "aVertexPosition")
    gl.enableVertexAttribArray(positionIndex)
    gl.vertexAttribPointer(positionIndex, 3, FLOAT, false, 0, 0)

    gl.bindBuffer(ARRAY_BUFFER, colorBuffer)

    val colorIndex = gl.getAttribLocation(program, "aVertexColor")
    gl.enableVertexAttribArray(colorIndex)
    gl.vertexAttribPointer(colorIndex, 4, FLOAT, false, 0, 0)

    gl.drawArrays(TRIANGLE_STRIP, 0, vertices.length / 3)
  }

  def initColorBuffer(gl: WebGLRenderingContext): WebGLBuffer = {
    val verticesColorBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, verticesColorBuffer)
    val verticeColors = new Float32Array(js.Array(
      1.0f, 1.0f, 1.0f, 1.0f, // white
      1.0f, 0.0f, 0.0f, 1.0f, // red
      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 0.0f, 1.0f, 1.0f // blue
    ))
    gl.bufferData(ARRAY_BUFFER, verticeColors, STATIC_DRAW)
    verticesColorBuffer
  }

}