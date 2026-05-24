import scala.math._

object AmericanFXPricer {
  
  /**
   * Prices an American FX Put Option using Projected SOR.
   * @param S0      Current exchange rate
   * @param K       Strike price
   * @param T       Time to maturity (years)
   * @param rd      Domestic risk-free rate
   * @param rf      Foreign risk-free rate (dividend yield equivalent)
   * @param sigma   Volatility
   * @param M       Number of price steps
   * @param N       Number of time steps
   * @return        Estimated option price
   */
  def priceAmericanFXPut(S0: Double, K: Double, T: Double, rd: Double, 
                         rf: Double, sigma: Double, M: Int, N: Int): Double = {
    
    val Smax = S0 * 3.0  // Sufficiently large boundary
    val dS = Smax / M
    val dt = T / N
    val omega = 1.2      // Relaxation parameter (1 < omega < 2)
    val tol = 1e-6       // Convergence tolerance
    val maxIter = 1000

    // Initialize grid and payoff (intrinsic value)
    val S = Array.tabulate(M + 1)(i => i * dS)
    var v = Array.tabulate(M + 1)(i => max(K - S(i), 0.0)) // Initial: Payoff at T
    val payoff = v.clone()

    // Finite difference coefficients (Crank-Nicolson)
    def a(i: Int) = 0.25 * dt * (sigma * sigma * i * i - (rd - rf) * i)
    def b(i: Int) = -0.5 * dt * (sigma * sigma * i * i + rd)
    def c(i: Int) = 0.25 * dt * (sigma * sigma * i * i + (rd - rf) * i)

    // Time-stepping backward
    for (j <- 1 to N) {
      val rhs = new Array[Double](M + 1)
      for (i <- 1 until M) {
        rhs(i) = a(i) * v(i - 1) + (1 + b(i)) * v(i) + c(i) * v(i + 1)
      }
      
      // Boundary conditions (Put option)
      rhs(0) = K // Lower boundary
      rhs(M) = 0 // Upper boundary

      // Projected SOR Iteration
      var diff = tol + 1.0
      var iter = 0
      val vNew = v.clone()

      while (diff > tol && iter < maxIter) {
        var currentDiffSq = 0.0
        
        for (i <- 1 until M) {
          val oldVal = vNew(i)
          // SOR step
          var y = (rhs(i) + a(i) * vNew(i - 1) + c(i) * vNew(i + 1)) / (1 - b(i))
          y = oldVal + omega * (y - oldVal)
          
          // Projection step: Value cannot be less than payoff
          vNew(i) = max(y, payoff(i))
          
          currentDiffSq += pow(vNew(i) - oldVal, 2)
        }
        diff = sqrt(currentDiffSq)
        iter += 1
      }
      v = vNew
    }

    // Linear interpolation for S0
    val idx = (S0 / dS).toInt
    v(idx) + (v(idx + 1) - v(idx)) * (S0 - S(idx)) / dS
  }

  def main(args: Array[String]): Unit = {
    val price = priceAmericanFXPut(100.0, 100.0, 1.0, 0.05, 0.03, 0.2, 100, 1000)
    println(f"American FX Put Option Price: $$${price}%.4f")
  }
}