import scala.math._

object FXDigitalPutFDM {

  def main(args: Array[String]): Unit = {
    // Parameters
    val S0 = 1.10      // example FX Spot (e.g., EUR/USD)
    val K = 1.05       // Strike Price
    val T = 0.5        // Time to maturity (6 months)
    val rd = 0.05      // Domestic risk-free rate (USD)
    val rf = 0.03      // Foreign risk-free rate (EUR)
    val sigma = 0.10   // Volatility (10%)
    val cash = 1.0     // Digital payout amount
    
    // Grid Parameters
    val Smax = S0 * 2.0 
    val M = 100        // Price steps
    val N = 10000      // Time steps (High for stability)

    val price = priceCashNothingPut(S0, K, T, rd, rf, sigma, cash, Smax, M, N)
    println(f"FX Cash-or-Nothing Put Price: $price%.5f")
  }

  def priceCashNothingPut(S0: Double, K: Double, T: Double, rd: Double, 
                          rf: Double, sigma: Double, cash: Double, 
                          Smax: Double, M: Int, N: Int): Double = {
    
    val ds = Smax / M
    val dt = T / N
    val S = Array.tabulate(M + 1)(i => i * ds)
    
    // 1. Initialize Terminal Payoff (at T)
    // Functional approach to initialize the price array
    var V = S.map { s =>
      if (s < K) cash
      else if (abs(s - K) < 1e-9) cash / 2.0
      else 0.0
    }

    // 2. Iterate Backward in Time
    // Using foldLeft to iterate through time steps j = 1 to N
    V = (1 to N).foldLeft(V) { (vNext, j) =>
      val currentV = new Array[Double](M + 1)
      
      for (i <- 1 until M) {
        val delta = (vNext(i + 1) - vNext(i - 1)) / (2 * ds)
        val gamma = (vNext(i + 1) - 2 * vNext(i) + vNext(i - 1)) / (ds * ds)

        // BS PDE: dV/dt + (rd-rf)S*dV/dS + 0.5*sigma^2*S^2*d2V/dS2 - rd*V = 0
        val theta = (rd * vNext(i)) - 
                    (rd - rf) * S(i) * delta - 
                    0.5 * pow(sigma, 2) * pow(S(i), 2) * gamma
        
        currentV(i) = vNext(i) - theta * dt
      }

      // 3. Boundary Conditions
      currentV(0) = cash * exp(-rd * j * dt) // S=0: Always ITM
      currentV(M) = 0.0                      // S=Smax: Always OTM
      
      currentV
    }

    // 4. Linear Interpolation to find price at S0
    interpolate(S0, S, V)
  }

  private def interpolate(targetS: Double, S: Array[Double], V: Array[Double]): Double = {
    val idx = S.indexWhere(_ >= targetS)
    if (idx <= 0) V(0)
    else {
      val slope = (V(idx) - V(idx - 1)) / (S(idx) - S(idx - 1))
      V(idx - 1) + slope * (targetS - S(idx - 1))
    }
  }
}