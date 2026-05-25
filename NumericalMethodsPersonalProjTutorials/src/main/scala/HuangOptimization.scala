object HuangOptimization {

  // Type aliases for easier reading
  type Vector1D = Vector[Double]
  type Matrix2D = Vector[Vector[Double]]

  // --- Vector & Matrix Helpers ---
  def dot(u: Vector1D, v: Vector1D): Double = 
    u.zip(v).map { case (a, b) => a * b }.sum

  def scale(v: Vector1D, s: Double): Vector1D = 
    v.map(_ * s)

  def add(u: Vector1D, v: Vector1D): Vector1D = 
    u.zip(v).map { case (a, b) => a + b }

  def sub(u: Vector1D, v: Vector1D): Vector1D = 
    u.zip(v).map { case (a, b) => a - b }

  def matVecMul(A: Matrix2D, v: Vector1D): Vector1D =
    A.map(row => dot(row, v))

  def outerProduct(u: Vector1D, v: Vector1D): Matrix2D =
    u.map(ui => v.map(vj => ui * vj))

  def matAdd(A: Matrix2D, B: Matrix2D): Matrix2D =
    A.zip(B).map { case (rA, rB) => rA.zip(rB).map { case (a, b) => a + b } }

  def matScale(A: Matrix2D, s: Double): Matrix2D =
    A.map(row => row.map(_ * s))

  /**
   * Generalized Huang Update Formula for Inverse Hessian Approximation (Bk)
   * 
   * Bk_next = rho * Bk 
   *           + (s_k * [omega * s_k + (1 - omega) * Bk * y_k]^T) / ((omega * s_k + (1 - omega) * Bk * y_k)^T * y_k)
   *           - (rho * Bk * y_k * [theta * s_k + (1 - theta) * Bk * y_k]^T) / ((theta * s_k + (1 - theta) * Bk * y_k)^T * y_k)
   */
  def huangUpdate(
      Bk: Matrix2D, 
      sk: Vector1D, 
      yk: Vector1D, 
      rho: Double, 
      omega: Double, 
      theta: Double
  ): Matrix2D = {
    
    val Bkyk = matVecMul(Bk, yk)

    // Intermediate vector: p = omega * sk + (1 - omega) * Bk * yk
    val p = add(scale(sk, omega), scale(Bkyk, 1.0 - omega))
    val denom1 = dot(p, yk)

    // Intermediate vector: q = theta * sk + (1 - theta) * Bk * yk
    val q = add(scale(sk, theta), scale(Bkyk, 1.0 - theta))
    val denom2 = dot(q, yk)

    // Term 1: rho * Bk
    val term1 = matScale(Bk, rho)

    // Term 2: (sk outer p) / denom1
    val term2 = matScale(outerProduct(sk, p), 1.0 / denom1)

    // Term 3: (Bkyk outer q) / denom2
    val term3 = matScale(outerProduct(Bkyk, q), 1.0 / denom2)

    // Bk_next = term1 + term2 - term3
    matAdd(matAdd(term1, term2), matScale(term3, -1.0))
  }

  // --- Preset Configurations ---
  
  // Classical BFGS initialization via Huang's parameters
  def bfgsUpdate(Bk: Matrix2D, sk: Vector1D, yk: Vector1D): Matrix2D = {
    // rho = 1, omega = 1, theta = 0 yields the symmetric BFGS formula
    huangUpdate(Bk, sk, yk, rho = 1.0, omega = 1.0, theta = 0.0)
  }

  // Classical DFP initialization via Huang's parameters
  def dfpUpdate(Bk: Matrix2D, sk: Vector1D, yk: Vector1D): Matrix2D = {
    // rho = 1, omega = 0, theta = 1 yields the symmetric DFP formula
    huangUpdate(Bk, sk, yk, rho = 1.0, omega = 0.0, theta = 1.0)
  }

  def main(args: Array[String]): Unit = {
    // Identity matrix serving as initial inverse Hessian guess (2x2 problem)
    val B0: Matrix2D = Vector(
        Vector(1.0, 0.0),
        Vector(0.0, 1.0)
    )

    // Example optimization updates from iteration step k
    val sk: Vector1D = Vector(0.5, -0.2) // Change in position x
    val yk: Vector1D = Vector(1.2,  0.4) // Change in gradient g

    // 1. Evaluate an Asymmetric Custom Huang Update Configuration
    val B1_huang = huangUpdate(B0, sk, yk, rho = 0.9, omega = 0.7, theta = 0.3)
    
    // 2. Evaluate standard symmetric updates derived via Huang's parameters
    val B1_bfgs = bfgsUpdate(B0, sk, yk)

    // Output formatting
    println("--- Custom Asymmetric Huang Matrix Update ---")
    B1_huang.foreach(row => println(row.map(f => f"$f%6.3f").mkString("[", ", ", "]")))

    println("\n--- Derived BFGS Matrix Update ---")
    B1_bfgs.foreach(row => println(row.map(f => f"$f%6.3f").mkString("[", ", ", "]")))
  }
}