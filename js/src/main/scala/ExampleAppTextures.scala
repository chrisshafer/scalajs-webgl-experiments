import org.scalajs.dom.ext.Image
import org.scalajs.dom.html
import org.scalajs.dom.html.Image
import org.scalajs.dom.raw.{WebGLTexture, WebGLBuffer, WebGLProgram, WebGLRenderingContext}
import org.w3c.dom.html.HTMLImageElement

import scala.scalajs.js
import scala.scalajs.js.JSApp

import org.scalajs.dom._
import scala.scalajs.js.typedarray._
import org.scalajs._
import dom._

object ExampleAppTextures extends JSApp {

  import raw.WebGLRenderingContext._

  def vmain(glCode: String) = {
    s"void main(void) { ${glCode} }"
  }

  def varyingFragmentShader(gl: WebGLRenderingContext) = {
    val fragmentShader = gl.createShader(FRAGMENT_SHADER)
    val fragText = "varying highp vec2 vTextureCoord;" +
                   "uniform sampler2D uSampler;" +
                    vmain(s"gl_FragColor = texture2D(uSampler, vec2(vTextureCoord.s, vTextureCoord.t));")
    gl.shaderSource(fragmentShader, fragText)
    gl.compileShader(fragmentShader)
    fragmentShader
  }

  def varyingVertexShader(gl: WebGLRenderingContext) = {
    val vertexShader = gl.createShader(VERTEX_SHADER)
    val vertexText = "attribute vec3 aVertexPosition;" +
      "attribute vec2 aTextureCoord;" +
      "uniform mat4 uMVMatrix;" +
      "uniform mat4 uPMatrix;" +
      "varying highp vec2 vTextureCoord;" +
      vmain("gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1);" +
        "vTextureCoord = aTextureCoord;")

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
    val texturePointer = initTextures(gl)

    js.timers.setInterval(15){
      rotation = if(rotation == Int.MaxValue) 0 else rotation + 1
      renderScene(gl)(rotation)(width,height,texturePointer)
    }
  }

  def rotMod(initial: Double, rotation: Int): Double = {

    initial * Math.sin(Math.toRadians(rotation))
  }



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

  var mvMatrix: Matrix = Matrix.empty
  var perspectiveMatrix: Matrix = Matrix.empty

  def renderScene(gl: WebGLRenderingContext)(rotation: Int)(width: Int, height: Int, texture: WebGLTexture) = {
    gl.clear(COLOR_BUFFER_BIT)
    gl.clear(DEPTH_BUFFER_BIT)

    perspectiveMatrix = Matrix.makePerspective(45, width.toFloat/height.toFloat, 0.1f, 100.0f)
    mvMatrix = Matrix.identity(4)
    rotate(rotation,Vector(1, 0, 1))
    mvMatrix = mvMatrix.*(Matrix.translation(Vector(0,0,-6)))


    val textureBuffer = initTextureBuffer(gl)

    val squareVerticesBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, squareVerticesBuffer)


    gl.bufferData(ARRAY_BUFFER, vertices, STATIC_DRAW)

    val program = initShaders(gl)
    gl.useProgram(program)
    val positionIndex = gl.getAttribLocation(program, "aVertexPosition")
    gl.enableVertexAttribArray(positionIndex)
    gl.vertexAttribPointer(positionIndex, 3, FLOAT, false, 0, 0)

    gl.bindBuffer(ARRAY_BUFFER, textureBuffer)
    val textureCoordAttribute = gl.getAttribLocation(program, "aTextureCoord")
    gl.enableVertexAttribArray(textureCoordAttribute)
    gl.vertexAttribPointer(textureCoordAttribute, 2, FLOAT, false, 0, 0)

    gl.activeTexture(TEXTURE0)
    gl.bindTexture(TEXTURE_2D, texture)
    gl.uniform1i(gl.getUniformLocation(program, "uSampler"), 0)

    val cubeBuffer = initVerticesIndexBuffer(gl)
    gl.bindBuffer(ELEMENT_ARRAY_BUFFER,cubeBuffer)
    setMatrixUniforms(gl, program)
    gl.drawElements(TRIANGLES, 36, UNSIGNED_SHORT, 0)
  }

  def initTextureBuffer(gl: WebGLRenderingContext): WebGLBuffer = {
    val verticesTextureBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, verticesTextureBuffer)
    val textures = new Float32Array(js.Array(
      // Front
      0.0,  0.0,
      1.0,  0.0,
      1.0,  1.0,
      0.0,  1.0,
      // Back
      0.0,  0.0,
      1.0,  0.0,
      1.0,  1.0,
      0.0,  1.0,
      // Top
      0.0,  0.0,
      1.0,  0.0,
      1.0,  1.0,
      0.0,  1.0,
      // Bottom
      0.0,  0.0,
      1.0,  0.0,
      1.0,  1.0,
      0.0,  1.0,
      // Right
      0.0,  0.0,
      1.0,  0.0,
      1.0,  1.0,
      0.0,  1.0,
      // Left
      0.0,  0.0,
      1.0,  0.0,
      1.0,  1.0,
      0.0,  1.0
    ))
    gl.bufferData(ARRAY_BUFFER, textures, STATIC_DRAW)
    verticesTextureBuffer
  }

  def initTextures(gl: WebGLRenderingContext): WebGLTexture = {
    val texture = gl.createTexture()
    val cubeImage = dom.document.createElement("img").asInstanceOf[Image]
    cubeImage.onload = { x: Event =>
      handleTexture(gl)(cubeImage, texture)
    }
    cubeImage.src = "resources/image/cubetexture.png"
    texture
  }

  def handleTexture(gl: WebGLRenderingContext)(image: Image, texture: WebGLTexture) = {
      console.log("handleTextureLoaded, image = " + image)
      gl.bindTexture(TEXTURE_2D, texture)
      gl.texImage2D(TEXTURE_2D, 0, RGBA, RGBA, UNSIGNED_BYTE, image)
      gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, LINEAR)
      gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, LINEAR_MIPMAP_NEAREST)
      gl.generateMipmap(TEXTURE_2D)
      gl.bindTexture(TEXTURE_2D, null)
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