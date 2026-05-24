import breeze.linalg._
import breeze.stats._
import breeze.stats.distributions._
import breeze.numerics._
import breeze.optimize.DiffFunction
import breeze.optimize.LBFGS
import breeze.stats.distributions.Rand.FixedSeed.randBasis

object FXSimulator {
  // Spot Indicative Rates
  val spots = Map("AUDUSD" -> 0.7195, "CHFNOK" -> 11.8329, "GBPJPY" -> 212.98)
  
  case class Params(mu: Double, vol: Double, dt: Double = 1.0/252.0)

  // 1. Geometric Brownian Motion (GBM)
  def simulateGBM(s0: Double, p: Params, steps: Int): DenseVector[Double] = {
    val path = DenseVector.zeros[Double](steps + 1)
    path(0) = s0
    val rnorm = Gaussian(0, 1)
    for (i <- 1 to steps) {
      val drift = (p.mu - 0.5 * p.vol * p.vol) * p.dt
      val shock = p.vol * math.sqrt(p.dt) * rnorm.draw()
      path(i) = path(i-1) * math.exp(drift + shock)
    }
    path
  }

  // 2. GARCH(1,1) Simulation
  def simulateGARCH(s0: Double, mu: Double, omega: Double, alpha: Double, beta: Double, steps: Int): DenseVector[Double] = {
    val path = DenseVector.zeros[Double](steps + 1)
    val volPath = DenseVector.zeros[Double](steps + 1)
    path(0) = s0
    volPath(0) = omega / (1 - alpha - beta) // Long-run variance
    val rnorm = Gaussian(0, 1)
    val dt = 1.0/252.0

    for (i <- 1 to steps) {
      val z = rnorm.draw()
      val ret = (mu - 0.5 * volPath(i-1)) * dt + math.sqrt(volPath(i-1) * dt) * z
      path(i) = path(i-1) * math.exp(ret)
      // Update variance for next step
      val epsilon2 = math.pow(math.sqrt(volPath(i-1)) * z, 2)
      volPath(i) = omega + alpha * epsilon2 + beta * volPath(i-1)
    }
    path
  }

  // 3. Stochastic Volatility (Heston discretization)
  def simulateHeston(s0: Double, v0: Double, mu: Double, kappa: Double, theta: Double, sigmaV: Double, rho: Double, steps: Int): DenseVector[Double] = {
    val path = DenseVector.zeros[Double](steps + 1)
    val vPath = DenseVector.zeros[Double](steps + 1)
    path(0) = s0
    vPath(0) = v0
    val dt = 1.0/252.0
    val multiNormal = MultivariateGaussian(DenseVector(0.0, 0.0), diag(DenseVector(1.0, 1.0)))

    for (i <- 1 to steps) {
      val shocks = multiNormal.draw()
      val z1 = shocks(0)
      val z2 = rho * z1 + math.sqrt(1 - rho * rho) * shocks(1) // Correlated shocks
      
      val vol = math.sqrt(math.max(0, vPath(i-1)))
      path(i) = path(i-1) * math.exp((mu - 0.5 * vol * vol) * dt + vol * math.sqrt(dt) * z1)
      vPath(i) = vPath(i-1) + kappa * (theta - vPath(i-1)) * dt + sigmaV * vol * math.sqrt(dt) * z2
    }
    path
  }

  // Metrics: Value at Risk (VaR) and Option Payoff
  def computeVaR(paths: Seq[DenseVector[Double]], confidence: Double = 0.95): Double = {
    val finalPrices = paths.map(path => path(path.length - 1)).sorted
    val index = ((1 - confidence) * finalPrices.size).toInt
    finalPrices(index)
  }

  def callPayoff(spot: Double, strike: Double): Double = math.max(0, spot - strike)

  object Calibrator {
    
    /** 1. GBM Calibration (MLE for normal log-returns) */
    def calibrateGBM(prices: DenseVector[Double]): Params = {
      val logReturns = diff(log(prices))
      val mu = mean(logReturns) * 252 // Annualise
      val sigma = stddev(logReturns) * math.sqrt(252)
      Params(mu, sigma)
    }

    /** 2. GARCH(1,1) Calibration using L-BFGS */
    def calibrateGARCH(prices: DenseVector[Double]): (Double, Double, Double) = {
      val returns = diff(log(prices))
      val initialVar = variance(returns)
      
      // Define the Negative Log-Likelihood objective function
      val nll = new DiffFunction[DenseVector[Double]] {
        def calculate(params: DenseVector[Double]) = {
          val omega = params(0)
          val alpha = params(1)
          val beta = params(2)
          
          // Basic constraints: params must be positive and stationary
          if (omega <= 0 || alpha < 0 || beta < 0 || (alpha + beta) >= 1) {
            (1e10, DenseVector.zeros[Double](3))
          } else {
            var logLikelihood = 0.0
            var h = initialVar
            for (r <- returns) {
              logLikelihood += math.log(h) + (r * r) / h
              h = omega + alpha * (r * r) + beta * h
            }
            // Value -> (0.5 * LL), Gradient -> Omitted (L-BFGS approximates)
            (0.5 * logLikelihood, DenseVector.zeros[Double](3)) 
          }
        }
      }

      val lbfgs = new LBFGS[DenseVector[Double]](maxIter = 100)
      // Initial guesses: [omega, alpha, beta]
      val result = lbfgs.minimize(nll, DenseVector(1e-6, 0.05, 0.90))
      (result(0), result(1), result(2))
    }
  }
}

// Generate mock history daya: synthesised historical prices starting from
// spot value for GBPJPY = 212.98 
// Runnable Application Object
object SimulationRunner extends App {
  import FXSimulator._

  // 1. Fetch current GBPJPY spot rate
  val initialSpot = spots("GBPJPY") 
  println(s"=== Initializing Simulation Suite for GBPJPY (Current Spot: $initialSpot) ===")

  // 2. Generate a mock history dataset (252 trailing business days)
  val totalHistoricalDays = 252
  val mockHistory = DenseVector.zeros[Double](totalHistoricalDays + 1)
  mockHistory(0) = initialSpot
  
  val sampleDrift = 0.02
  val sampleVol = 0.12
  val dt = 1.0 / 252.0
  val standardNormal = Gaussian(0, 1)

  for (i <- 1 to totalHistoricalDays) {
    val shock = sampleVol * math.sqrt(dt) * standardNormal.draw()
    mockHistory(i) = mockHistory(i - 1) * math.exp((sampleDrift - 0.5 * sampleVol * sampleVol) * dt + shock)
  }
  println(s"Generated $totalHistoricalDays days of mock price data.")

  // 3. Calibrate Models using the data history
  println("\n--- Calibrating Models ---")
  
  val gbmParams = Calibrator.calibrateGBM(mockHistory)
  println(f"GBM Calibration Results -> Annualized Drift (mu): ${gbmParams.mu * 100}%.2f%%, Volatility (sigma): ${gbmParams.vol * 100}%.2f%%")

  val (omega, alpha, beta) = Calibrator.calibrateGARCH(mockHistory)
  val longRunVol = math.sqrt((omega / (1 - alpha - beta)) * 252)
  println(f"GARCH(1,1) Calibration Results -> Omega: $omega%.6f, Alpha: $alpha%.4f, Beta: $beta%.4f")
  println(f"GARCH Long-Run Annualized Volatility: ${longRunVol * 100}%.2f%%")

  // 4. Run Forward Monte Carlo Projections (e.g., 1000 paths over 30 forward steps)
  println("\n--- Running Monte Carlo Projections (30 Days Forward) ---")
  val projectionSteps = 30
  val totalPathsCount = 1000
  // generate 1000 future simulation timelines across GBM, GARCH, Heston
  // evaluate final path values to identify 99% VaR threshold for each model

  // extract last element
  val lastHistoricalPrice = mockHistory(mockHistory.length - 1)

  val gbmPaths = (1 to totalPathsCount).map(_ => simulateGBM(lastHistoricalPrice, gbmParams, projectionSteps))
  val garchPaths = (1 to totalPathsCount).map(_ => simulateGARCH(lastHistoricalPrice, gbmParams.mu, omega, alpha, beta, projectionSteps))
  
  // Heston uses assumed variance parameters since calibration wasn't specified
  val initialVarianceV0 = sampleVol * sampleVol
  val hestonPaths = (1 to totalPathsCount).map(_ => 
    simulateHeston(lastHistoricalPrice, v0 = initialVarianceV0, mu = gbmParams.mu, kappa = 2.0, theta = initialVarianceV0, sigmaV = 0.1, rho = -0.5, projectionSteps)
  )

  // 5. Evaluate and Print Value At Risk (VaR) Metrics
  val confidenceInterval = 0.99
  val gbmVaR = computeVaR(gbmPaths, confidenceInterval)
  val garchVaR = computeVaR(garchPaths, confidenceInterval)
  val hestonVaR = computeVaR(hestonPaths, confidenceInterval)

  println(f"GBM   99%% Forward VaR Price Floor: $gbmVaR%.2f")
  println(f"GARCH 99%% Forward VaR Price Floor: $garchVaR%.2f")
  println(f"Heston 99%% Forward VaR Price Floor: $hestonVaR%.2f")
  println("\n=== Processing Complete ===")
}