package com.shafer.tutorial

import org.scalajs._
import org.scalajs.dom.{html, _}
import org.scalajs.dom.raw.{WebGLBuffer, WebGLProgram, WebGLRenderingContext}

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.typedarray._

object ExampleApp extends JSApp {

  import org.scalajs.dom.raw.WebGLRenderingContext._

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
      "uniform mat4 uMVMatrix;" +
      "uniform mat4 uPMatrix;" +
      "varying lowp vec4 vColor;" +
      vmain("gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1);" +
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
    val width = window.innerWidth
    val height = window.innerHeight
    val canvas: html.Canvas = document.createElement("canvas").asInstanceOf[html.Canvas]
    document.body.appendChild(canvas)
    canvas.width = width
    canvas.height = height
    val gl: raw.WebGLRenderingContext = canvas.getContext("webgl").asInstanceOf[raw.WebGLRenderingContext]
    gl.clearColor(0.0, 0.0, 0.0, 1.0)
    gl.clearDepth(1.0)
    gl.enable(DEPTH_TEST)
    gl.depthFunc(LEQUAL)

    var rotation = 0
    js.timers.setInterval(15){
      rotation = if(rotation == Int.MaxValue) 0 else rotation + 1
      renderScene(gl)(rotation)(width,height)
    }
  }

  def rotMod(initial: Double, rotation: Int): Double = {

    initial * Math.sin(Math.toRadians(rotation))
  }

  var mvMatrix: Matrix = Matrix.empty
  var perspectiveMatrix: Matrix = Matrix.empty

  def setMatrixUniforms(gl: WebGLRenderingContext, shaderProgram: WebGLProgram) {
    val pUniform = gl.getUniformLocation(shaderProgram, "uPMatrix")
    gl.uniformMatrix4fv(pUniform, false, new Float32Array(perspectiveMatrix.toJs.flatten.map(_.toFloat)))

    val mvUniform = gl.getUniformLocation(shaderProgram, "uMVMatrix")
    gl.uniformMatrix4fv(mvUniform, false, new Float32Array(mvMatrix.toJs.flatten.map(_.toFloat)))
  }

  def rotate(degrees: Double, scales: Vector[Double]) = {
    val radians = degrees * Math.PI / 180.0
    mvMatrix = mvMatrix.*(Matrix.rotate(radians,scales))

  }
  def renderScene(gl: WebGLRenderingContext)(rotation: Int)(width: Int, height: Int) = {
    gl.clear(COLOR_BUFFER_BIT)
    gl.clear(DEPTH_BUFFER_BIT)

    perspectiveMatrix = Matrix.makePerspective(45, width.toFloat/height.toFloat, 0.1f, 100.0f)
    mvMatrix = Matrix.identity(4)
    rotate(rotation,Vector(1, 0, 1))
    mvMatrix = mvMatrix.*(Matrix.translation(Vector(0,0,-6)))


    val colorBuffer = initColorBuffer(gl)

    val squareVerticesBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, squareVerticesBuffer)
    val vertices: Float32Array = new Float32Array(js.Array(
      // Front face
      -1.0, -1.0,  1.0,
      1.0, -1.0,  1.0,
      1.0,  1.0,  1.0,
      -1.0,  1.0,  1.0,

      // Back face
      -1.0, -1.0, -1.0,
      -1.0,  1.0, -1.0,
      1.0,  1.0, -1.0,
      1.0, -1.0, -1.0,

      // Top face
      -1.0,  1.0, -1.0,
      -1.0,  1.0,  1.0,
      1.0,  1.0,  1.0,
      1.0,  1.0, -1.0,

      // Bottom face
      -1.0, -1.0, -1.0,
      1.0, -1.0, -1.0,
      1.0, -1.0,  1.0,
      -1.0, -1.0,  1.0,

      // Right face
      1.0, -1.0, -1.0,
      1.0,  1.0, -1.0,
      1.0,  1.0,  1.0,
      1.0, -1.0,  1.0,

      // Left face
      -1.0, -1.0, -1.0,
      -1.0, -1.0,  1.0,
      -1.0,  1.0,  1.0,
      -1.0,  1.0, -1.0
    ))

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

    val cubeBuffer = initVerticesIndexBuffer(gl)
    gl.bindBuffer(ELEMENT_ARRAY_BUFFER,cubeBuffer)
    setMatrixUniforms(gl, program)
    gl.drawElements(TRIANGLES, 36, UNSIGNED_SHORT, 0)
  }

  def initColorBuffer(gl: WebGLRenderingContext): WebGLBuffer = {
    val verticesColorBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, verticesColorBuffer)
    val verticeColors = new Float32Array(js.Array(
      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 1.0f, 0.0f, 1.0f, // green

      0.0f, 0.0f, 1.0f, 1.0f, // blue
      0.0f, 0.0f, 1.0f, 1.0f, // blue
      0.0f, 0.0f, 1.0f, 1.0f, // blue
      0.0f, 0.0f, 1.0f, 1.0f, // blue

      1.0f, 1.0f, 1.0f, 1.0f, // white
      1.0f, 1.0f, 1.0f, 1.0f, // white
      1.0f, 1.0f, 1.0f, 1.0f, // white
      1.0f, 1.0f, 1.0f, 1.0f, // white

      1.0f, 0.0f, 0.0f, 1.0f, // red
      1.0f, 0.0f, 0.0f, 1.0f, // red
      1.0f, 0.0f, 0.0f, 1.0f, // red
      1.0f, 0.0f, 0.0f, 1.0f, // red

      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 1.0f, 0.0f, 1.0f, // green
      0.0f, 1.0f, 0.0f, 1.0f, // green

      1.0f, 0.0f, 0.0f, 1.0f, // red
      1.0f, 0.0f, 0.0f, 1.0f, // red
      1.0f, 0.0f, 0.0f, 1.0f, // red
      1.0f, 0.0f, 0.0f, 1.0f // red
    ))
    gl.bufferData(ARRAY_BUFFER, verticeColors, STATIC_DRAW)
    verticesColorBuffer
  }

  def initVerticesIndexBuffer(gl: WebGLRenderingContext): WebGLBuffer = {
    val cubeVerticesIndexBuffer = gl.createBuffer()
    gl.bindBuffer(ELEMENT_ARRAY_BUFFER, cubeVerticesIndexBuffer)
    val verticeIndices = new Uint16Array(js.Array(
    0,  1,  2,      0,  2,  3,    // front
    4,  5,  6,      4,  6,  7,    // back
    8,  9,  10,     8,  10, 11,   // top
    12, 13, 14,     12, 14, 15,   // bottom
    16, 17, 18,     16, 18, 19,   // right
    20, 21, 22,     20, 22, 23    // left
    ))

    gl.bufferData(ELEMENT_ARRAY_BUFFER, verticeIndices, STATIC_DRAW)
    cubeVerticesIndexBuffer
  }

}