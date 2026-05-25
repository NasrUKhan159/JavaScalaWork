object HuangMethod:

  // Definition for matrix and vector formats
  type Vector = Array[Double]
  type Matrix = Array[Array[Double]]

  /**
   * Solves Ax = b using the Standard Huang method from the ABS family.
   * @param A Coefficient matrix (m x n)
   * @param b Right-hand side vector (m)
   * @return Option containing the solution vector if found, None if inconsistent
   */
  def solve(A: Matrix, b: Vector): Option[Vector] =
    val m = A.length
    val n = A(0).length
    
    // Initialize solution vector x_1 to zero
    var x = Array.fill(n)(0.0)
    
    // Initialize Deflection matrix H_1 to the Identity Matrix
    var H = Array.tabulate(n, n)((i, j) => if i == j then 1.0 else 0.0)

    for i <- 0 until m do
      val ai = A(i)
      
      // Compute search direction vector: p_i = H_i * a_i
      val p = matVecMul(H, ai)
      
      // Calculate scalar denominator: a_i^T * p_i
      val denominator = dotProduct(ai, p)

      if Math.abs(denominator) < 1e-12 then
        // If p_i is zero, verify consistency
        val residual = b(i) - dotProduct(ai, x)
        if Math.abs(residual) > 1e-9 then
          return None // System is inconsistent
      else
        // Step size scalar: alpha_i = (b_i - a_i^T * x_i) / (a_i^T * p_i)
        val alpha = (b(i) - dotProduct(ai, x)) / denominator
        
        // Update solution: x_{i+1} = x_i + alpha * p_i
        x = Array.tabulate(n)(j => x(j) + alpha * p(j))

        // Update Deflection Matrix: H_{i+1} = H_i - (H_i * a_i * a_i^T * H_i) / (a_i^T * H_i * a_i)
        // Note: (H_i * a_i) is already calculated as vector 'p'
        val outerProduct = Array.tabulate(n, n)((r, c) => p(r) * p(c))
        H = Array.tabulate(n, n)((r, c) => H(r)(c) - (outerProduct(r)(c) / denominator))
    
    Some(x)

  // Helper: Vector dot product
  private def dotProduct(v1: Vector, v2: Vector): Double =
    v1.zip(v2).map((x, y) => x * y).sum

  // Helper: Matrix-Vector multiplication
  private def matVecMul(M: Matrix, v: Vector): Vector =
    M.map(row => dotProduct(row, v))

  def main(args: Array[String]): Unit =
    // Define an ill-conditioned linear system Example
    val A: Matrix = Array(
      Array(2.0, 1.0, 1.0),
      Array(1.0, 3.0, 2.0),
      Array(1.0, 0.0, 0.0)
    )
    val b: Vector = Array(4.0, 6.0, 1.0)

    solve(A, b) match
      case Some(solution) => 
        println("System solved successfully using Huang Method.")
        println(s"Solution Vector x: [${solution.mkString(", ")}]")
      case None => 
        println("The system is inconsistent or has no solution.")
