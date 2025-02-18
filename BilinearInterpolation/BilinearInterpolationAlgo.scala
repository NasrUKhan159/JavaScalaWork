// code that performs bilinear interpolation given the coordinates and the values
// at the corners of a rectangle

object BilinearInterpolation {
  def interpolate(x: Double, y: Double,
                  x1: Double, y1: Double, f11: Double,
                  x2: Double, y2: Double, f21: Double,
                  f12: Double, f22: Double): Double = {
    val a = (x2 - x1)
    val b = (y2 - y1)
    val d = a * b

    val term1 = (f11 * (x2 - x) * (y2 - y)) / d
    val term2 = (f21 * (x - x1) * (y2 - y)) / d
    val term3 = (f12 * (x2 - x) * (y - y1)) / d
    val term4 = (f22 * (x - x1) * (y - y1)) / d

    term1 + term2 + term3 + term4
  }

  def main(args: Array[String]): Unit = {
    val x1 = 0.0
    val y1 = 0.0
    val x2 = 1.0
    val y2 = 1.0
    val f11 = 0.0
    val f21 = 1.0
    val f12 = 1.0
    val f22 = 0.0

    val x = 0.5
    val y = 0.5
    val result = interpolate(x, y, x1, y1, f11, x2, y2, f21, f12, f22)

    println(s"Interpolated value at ($x, $y) is: $result")
  }
}