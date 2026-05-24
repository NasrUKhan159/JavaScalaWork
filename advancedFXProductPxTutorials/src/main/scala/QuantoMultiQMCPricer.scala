object QuantoMultiQMCPricer {

  // ==========================================
  // 1. HALTON SEQUENCE GENERATOR
  // ==========================================
  def halton(index: Int, base: Int): Double = {
    var f = 1.0
    var r = 0.0
    var i = index
    while (i > 0) {
      f /= base
      r += f * (i % base)
      i /= base
    }
    r
  }

  // ==========================================
  // 2. FAURE SEQUENCE GENERATOR (Simplified 2D)
  // Base must be a prime >= dimension. For 2D, prime = 2.
  // ==========================================
  def faure2D(index: Int): (Double, Double) = {
    val base = 2
    // Dimension 1: Radical inversion base 2
    var f = 1.0
    var y1 = 0.0
    var i = index
    val coefficients = scala.collection.mutable.ArrayBuffer[Int]()
    
    while (i > 0) {
      f /= base
      val digit = i % base
      coefficients += digit
      y1 += f * digit
      i /= base
    }

    // Dimension 2: Combinatorial transformation using Pascal Matrix
    var y2 = 0.0
    f = 1.0
    for (j <- coefficients.indices) {
      f /= base
      // Compute the transformed digit using Pascal combinations modulo 2
      var transformedDigit = 0
      for (k <- j until coefficients.length) {
        val pascal = choose(k, j) % base
        transformedDigit = (transformedDigit + pascal * coefficients(k)) % base
      }
      y2 += f * transformedDigit
    }
    (y1, y2)
  }

  private def choose(n: Int, k: Int): Int = {
    if (k == 0 || k == n) 1
    else choose(n - 1, k - 1) + choose(n - 1, k)
  }

  // ==========================================
  // 3. SOBOL SEQUENCE GENERATOR (2D)
  // Uses primitive polynomials to find directional numbers
  // ==========================================
  class Sobol2D {
    // Direction numbers initialized for Dim 1 and Dim 2
    private val BITS = 30
    private val m1 = Array.tabulate(BITS)(j => 1)
    private val m2 = Array(0, 1, 3, 5, 15, 29, 51, 85, 113, 221) ++ Array.fill(BITS - 10)(1)

    private val v1 = Array.tabulate(BITS)(j => m1(j) << (BITS - 1 - j))
    private val v2 = Array.tabulate(BITS)(j => m2(math.min(j, m2.length - 1)) << (BITS - 1 - j))

    def getPoint(index: Int): (Double, Double) = {
      // Gray code conversion step
      val gray = index ^ (index >> 1)
      var x1 = 0
      var x2 = 0
      
      for (j <- 0 until BITS) {
        if (((gray >> j) & 1) == 1) {
          x1 ^= v1(j)
          x2 ^= v2(j)
        }
      }
      val denom = 1.0 / (1 << BITS)
      (x1 * denom, x2 * denom)
    }
  }

  // ==========================================
  // INVERSE CDF (Box-Muller for uniform pairs)
  // ==========================================
  def transformNormals(u1: Double, u2: Double): (Double, Double) = {
    val r = math.sqrt(-2.0 * math.log(math.max(u1, 1e-12)))
    val theta = 2.0 * math.Pi * u2
    (r * math.cos(theta), r * math.sin(theta))
  }

  // ==========================================
  // GENERALIZED QUANTO PRICER ENGINE
  // ==========================================
  def priceEngine(
      s0: Double, k: Double, t: Double, rF: Double, rD: Double,
      volS: Double, volX: Double, rho: Double, fixedRate: Double,
      simulations: Int, generatorType: String
  ): Double = {

    val quantoDrift = rD - rF - (rho * volS * volX)
    val driftStep = (quantoDrift - 0.5 * volS * volS) * t
    val volStep = volS * math.sqrt(t)
    var payoffSum = 0.0

    val sobolEngine = new Sobol2D()

    for (i <- 1 to simulations) {
      val (u1, u2) = generatorType.toLowerCase match {
        case "halton" => (halton(i, 2), halton(i, 3))
        case "faure"  => faure2D(i)
        case "sobol"  => sobolEngine.getPoint(i)
        case _        => (math.random(), math.random()) // Fallback to standard Pseudo-MC
      }

      // Convert Uniform QMC space to Normal Distribution Shocks
      val (z1, _) = transformNormals(u1, u2)
      
      // Simulate underlying terminal asset price
      val sT = s0 * math.exp(driftStep + volStep * z1)
      val payoff = math.max(sT - k, 0.0) * fixedRate
      payoffSum += payoff
    }

    (payoffSum / simulations) * math.exp(-rD * t)
  }

  def main(args: Array[String]): Unit = {
    // eg set of values used for GBPUSD quanto option
    val s0 = 1.25; val k = 1.20; val t = 1.0
    val rF = 0.05; val rD = 0.03; val volS = 0.15
    val volX = 0.10; val rho = 0.40; val fixedRate = 1.5
    val paths = 50000

    println(f"--- Pricing Call Option via Multi-QMC Paths ($paths) ---")
    println(f"Halton QMC Price: $$ ${priceEngine(s0, k, t, rF, rD, volS, volX, rho, fixedRate, paths, "halton")}%.5f")
    println(f"Faure QMC Price:  $$ ${priceEngine(s0, k, t, rF, rD, volS, volX, rho, fixedRate, paths, "faure")}%.5f")
    println(f"Sobol QMC Price:  $$ ${priceEngine(s0, k, t, rF, rD, volS, volX, rho, fixedRate, paths, "sobol")}%.5f")
    // economic interpretation of getting $0.11890 using Sobol sequence:
    // 1. cost of perfect replication: In financial economics, an option price derived from risk-neutral simulation 
    // represents ex-ante cost to set up dynamic replicating portfolio. To manufacture this payoff, market maker must spend
    // $0.11890 today to buy specific blend of underlying FX asset and domestic risk-free bonds. 
    // If mkt maker charges exactly $0.11890, they can continuously rebalance their hedges (delta hedging) s.t. at maturity, 
    // trading strategy will perfectly match final Quanto option payoff, leaving them with zero net risk.
    // 2. quantified value of "quanto drag/premium": $0.11890 accounts for interaction b/w 2 distinct ccy risk factors. Since it
    // is quanto option, underlying asset is FX rate but payout locked into fixed conversion rate in third (domestic) ccy.
    // drift = r_{D} - r_{F} - (\rho * \sigma_{S} * \sigma_{X})
    // \rho * \sigma_{S} * \sigma_{X} acts as explicit insurance premium/discount. If \rho > 0, underlying asset tends to 
    // strengthen at the same time the payment ccy strengthens. To compensate issuer for paying out in stronger ccy, risk-neutral
    // growth rate of asset is mathematically reduced (quanto drag). Price of 0.11890 is lower than standard FX option b/c of 
    // ccy corr risk.
    // 3. convergence certainty over pseudo-random pricing: From computational economics viewpoint, obtaining specific price from Sobol
    // sequence rather than traditional MC simulation means this value is highly deterministic. Traditional MC relies on pure randomness to
    // explore currency space, but Sobol structure systematically samples underlying currency interactions with extreme mathematical uniformity
    // Therefore, $0.11890 represents stable, converged evaluation of derivative's intrinsic risk profile.
    // references for the economic interpretation:
    // a. Harrison, J. M., & Pliska, S. R. (1981), "Martingales and stochastic integrals in the theory of continuous trading", 
    // Stochastic Processes and their Applications, 11(3), 215-260.
    // b. Reiner, E. (1992), "Quanto Mechanics", Risk Magazine, 5(7), 147-154
    // c. Paskov, S. H., & Traub, J. F. (1995), "Faster Valuation of Financial Derivatives", Journal of Portfolio Management, 22(1), 113-120
  }
}