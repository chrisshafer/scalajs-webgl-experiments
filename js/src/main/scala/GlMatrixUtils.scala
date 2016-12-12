import scala.scalajs.js

object Matrix{

  def empty = new Matrix(Array[Array[Double]]())
  def makePerspective(fovy: Double, aspect: Double, znear: Double, zfar: Double): Matrix = {
    val ymax: Double = znear.toFloat * Math.tan(fovy * Math.PI / 360.0).toFloat
    val ymin: Double = -ymax
    val xmin: Double = ymin * aspect
    val xmax: Double = ymax * aspect

    makeFrustum(xmin, xmax, ymin, ymax, znear, zfar)
  }

  //
  // glFrustum
  //
  def makeFrustum(left: Double, right: Double, bottom: Double, top: Double, znear: Double, zfar: Double): Matrix = {
    val X = 2*znear/(right-left)
    val Y = 2*znear/(top-bottom)
    val A = (right+left)/(right-left)
    val B = (top+bottom)/(top-bottom)
    val C = -(zfar+znear)/(zfar-znear)
    val D = -2*zfar*znear/(zfar-znear)

    new Matrix(Array(Array(X, 0.0, A, 0.0),
      Array(0.0, Y, B, 0.0),
      Array(0.0, 0.0, C, D),
      Array(0.0, 0.0, -1, 0.0)))
  }

  def identity(size: Int): Matrix = {
    val rows = (0 to (size - 1) by 1).map{ idx =>
      val indexed = Array.fill(size)(0.0)
      indexed(idx) = 1
      indexed
    }
    val matrix = Array( rows.toSeq : _*)
    new Matrix(matrix)
  }

  def translation(v: Vector[Double]): Matrix = {
    if (v.length == 2) {
      var r = Matrix.identity(3)
      r.columnVectors(2)(0) = v(0)
      r.columnVectors(2)(1) = v(1)
      r
    } else if (v.length == 3) {
      var r = Matrix.identity(4)
      r.columnVectors(0)(3) = v(0)
      r.columnVectors(1)(3) = v(1)
      r.columnVectors(2)(3) = v(2)
      r
    } else throw new Exception("Invalid vector length for translation")
  }



}

class Matrix(private val columnVectors: Array[Array[Double]]) {
  val columns = columnVectors.size
  val rows    = columnVectors.headOption.map(_.size).getOrElse(0)

  def *(v: Array[Double]): Array[Double] = {
    val newValues = Array.ofDim[Double](rows)
    var col = 0
    while(col < columns) {
      val n = v(col)
      val column = columnVectors(col)
      var row = 0
      while(row < newValues.size) {
        newValues(row) += column(row) * n
        row += 1
      }
      col += 1
    }
    newValues
  }

  val toJs: js.Array[js.Array[Double]] = {
    import scalajs.js.JSConverters._
    columnVectors.transpose.map(_.toJSArray).toJSArray 
  }


  def *(other: Matrix): Matrix = {
    new Matrix(other.columnVectors.map(col => this * col))
  }

  override def toString = {
    columnVectors.transpose.map(_.mkString(", ")).mkString("\n")
  }
}
