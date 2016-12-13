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
                   "varying highp vec3 vLighting;" +
                   "uniform sampler2D uSampler;" +
                    vmain(s"highp vec4 texelColor = texture2D(uSampler, vec2(vTextureCoord.s, vTextureCoord.t));"+
                      s"gl_FragColor = vec4(texelColor.rgb * vLighting, texelColor.a);")
    gl.shaderSource(fragmentShader, fragText)
    gl.compileShader(fragmentShader)

    fragmentShader
  }

  def varyingVertexShader(gl: WebGLRenderingContext) = {
    val vertexShader = gl.createShader(VERTEX_SHADER)
    val vertexText = {
      "attribute highp vec3 aVertexNormal;\n" +
      "attribute highp vec3 aVertexPosition;\n" +
      "attribute highp vec2 aTextureCoord;\n" +
      "uniform highp mat4 uNormalMatrix;\n" +
      "uniform highp mat4 uMVMatrix;\n" +
      "uniform highp mat4 uPMatrix;\n" +
      "varying highp vec2 vTextureCoord;\n" +
      "varying highp vec3 vLighting;\n" +
      vmain("gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);\n" +
        "vTextureCoord = aTextureCoord;\n" +
        "highp vec3 ambientLight = vec3(0.3, 0.3, 0.3);\n" +
        "highp vec3 directionalLightColor = vec3(0.5, 0.5, 0.65);\n" +
        "highp vec3 directionalVector = vec3(0.5, 0.5, 1.0);\n" +
        "highp vec4 transformedNormal = uNormalMatrix * vec4(aVertexNormal, 1.0);\n" +
        "highp float directional = max(dot(transformedNormal.xyz, directionalVector), 0.0);\n" +
        "vLighting = ambientLight + (directionalLightColor * directional);\n") }

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
    val program = initShaders(gl)
    js.timers.setInterval(15){
      rotation = if(rotation == Int.MaxValue) 0 else rotation + 1
      renderScene(gl)(rotation)(width,height,texturePointer,program)
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

    val normalMatrix: Matrix = mvMatrix.transpose
    val nUniform     = gl.getUniformLocation(shaderProgram, "uNormalMatrix")
    gl.uniformMatrix4fv(nUniform, false, new Float32Array(normalMatrix.toJs.flatten.map(_.toFloat)))
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

  def renderScene(gl: WebGLRenderingContext)(rotation: Int)(width: Int, height: Int, texture: WebGLTexture, shaderProgram: WebGLProgram) = {
    gl.clear(COLOR_BUFFER_BIT)
    gl.clear(DEPTH_BUFFER_BIT)

    perspectiveMatrix = Matrix.makePerspective(45, width.toFloat/height.toFloat, 0.1f, 100.0f)
    mvMatrix = Matrix.identity(4)
    rotate(rotation,Vector(1, 0.2, 1))
    mvMatrix = mvMatrix.*(Matrix.translation(Vector(0,0,-6)))


    val textureBuffer = initTextureBuffer(gl)

    val squareVerticesBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, squareVerticesBuffer)


    gl.bufferData(ARRAY_BUFFER, vertices, STATIC_DRAW)
    gl.useProgram(shaderProgram)

    val positionIndex = gl.getAttribLocation(shaderProgram, "aVertexPosition")
    gl.enableVertexAttribArray(positionIndex)
    gl.vertexAttribPointer(positionIndex, 3, FLOAT, false, 0, 0)

    gl.bindBuffer(ARRAY_BUFFER, textureBuffer)
    val textureCoordAttribute = gl.getAttribLocation(shaderProgram, "aTextureCoord")
    gl.enableVertexAttribArray(textureCoordAttribute)
    gl.vertexAttribPointer(textureCoordAttribute, 2, FLOAT, false, 0, 0)

    val normalsBuffer = initNormalsBuffer(gl)
    gl.bindBuffer(ARRAY_BUFFER, normalsBuffer)
    val vertexNormalAttribute = gl.getAttribLocation(shaderProgram, "aVertexNormal")
    gl.enableVertexAttribArray(vertexNormalAttribute)
    gl.vertexAttribPointer(vertexNormalAttribute, 3, FLOAT, false, 0, 0)

    gl.activeTexture(TEXTURE0)
    gl.bindTexture(TEXTURE_2D, texture)
    gl.uniform1i(gl.getUniformLocation(shaderProgram, "uSampler"), 0)

    val cubeBuffer = initVerticesIndexBuffer(gl)
    gl.bindBuffer(ELEMENT_ARRAY_BUFFER,cubeBuffer)

    setMatrixUniforms(gl, shaderProgram)
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

  def initNormalsBuffer(gl: WebGLRenderingContext) = {
    val cubeVerticesNormalBuffer = gl.createBuffer()
    gl.bindBuffer(ARRAY_BUFFER, cubeVerticesNormalBuffer)

    val vertexNormals = js.Array(
    // Front
    0.0,  0.0,  1.0,
    0.0,  0.0,  1.0,
    0.0,  0.0,  1.0,
    0.0,  0.0,  1.0,

    // Back
    0.0,  0.0, -1.0,
    0.0,  0.0, -1.0,
    0.0,  0.0, -1.0,
    0.0,  0.0, -1.0,

    // Top
    0.0,  1.0,  0.0,
    0.0,  1.0,  0.0,
    0.0,  1.0,  0.0,
    0.0,  1.0,  0.0,

    // Bottom
    0.0, -1.0,  0.0,
    0.0, -1.0,  0.0,
    0.0, -1.0,  0.0,
    0.0, -1.0,  0.0,

    // Right
    1.0,  0.0,  0.0,
    1.0,  0.0,  0.0,
    1.0,  0.0,  0.0,
    1.0,  0.0,  0.0,

    // Left
    -1.0,  0.0,  0.0,
    -1.0,  0.0,  0.0,
    -1.0,  0.0,  0.0,
    -1.0,  0.0,  0.0
    )

    gl.bufferData(ARRAY_BUFFER, new Float32Array(vertexNormals), STATIC_DRAW)
    cubeVerticesNormalBuffer
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