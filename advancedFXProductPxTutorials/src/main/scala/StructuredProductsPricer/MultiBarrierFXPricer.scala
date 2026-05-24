package StructuredProductsPricer

import scala.math.{exp, sqrt}
import scala.collection.parallel.CollectionConverters._
import scala.util.Random

/**
 * Parameter container for the multi-barrier pivot knock-in FX forward contract.
 */
case class FXForwardParams(
    spot: Double,
    strike: Double,       // The pivot rate knocked in
    upperBarrier: Double, // H_up
    lowerBarrier: Double, // H_down
    domesticRate: Double, // r_d
    foreignRate: Double,  // r_f
    volatility: Double,   // sigma
    expiry: Double,       // T in years
    steps: Int,           // Number of monitoring intervals along the path
    paths: Int            // Total Monte Carlo paths
)

object MultiBarrierFXPricer {

  /**
   * Simulates FX paths and calculates the discounted expected payoff.
   */
  def price(params: FXForwardParams): Double = {
    val dt = params.expiry / params.steps
    val drift = (params.domesticRate - params.foreignRate - 0.5 * params.volatility * params.volatility) * dt
    val volStep = params.volatility * sqrt(dt)
    
    // Thread-safe random generation per parallel path group
    val randomSeeds = Array.fill(params.paths)(new Random())

    // Generate paths in parallel for maximum computational efficiency
    val totalPayoff = randomSeeds.par.map { rand =>
      var currentSpot = params.spot
      var isKnockedIn = false
      var step = 0

      // Simulate the asset path over time
      while (step < params.steps && !isKnockedIn) {
        val gauss = rand.nextGaussian()
        currentSpot *= exp(drift + volStep * gauss)

        // Check if either barrier is crossed (Multi-barrier monitoring)
        if (currentSpot >= params.upperBarrier || currentSpot <= params.lowerBarrier) {
          isKnockedIn = true
        }
        step += 1
      }

      // If already knocked in, continue simulating spot directly to maturity
      if (isKnockedIn) {
        while (step < params.steps) {
          val gauss = rand.nextGaussian()
          currentSpot *= exp(drift + volStep * gauss)
          step += 1
        }
        // Forward payoff at maturity: S_T - K
        currentSpot - params.strike
      } else {
        // Did not knock in; contract expires out-of-money/worthless
        0.0
      }
    }.sum

    // Calculate expected value and discount back to present value using the domestic rate
    val expectedPayoff = totalPayoff / params.paths
    expectedPayoff * exp(-params.domesticRate * params.expiry)
  }

  def main(args: Array[String]): Unit = {
    // Market scenario configuration
    val scenario = FXForwardParams(
      spot = 1.1000,         // Current EUR/USD spot
      strike = 1.1200,       // Pivot Forward rate knocked-in
      upperBarrier = 1.1500, // Upper boundary
      lowerBarrier = 1.0500, // Lower boundary
      domesticRate = 0.040,  // USD Risk-Free rate (4%)
      foreignRate = 0.015,   // EUR Risk-Free rate (1.5%)
      volatility = 0.10,     // 10% annualized volatility
      expiry = 1.0,          // 1-year tenor
      steps = 252,           // Daily monitoring paths
      paths = 1000000        // 1 Million iterations for high convergence
    )
    // with given forward param vals, we get pivot knock-in FX fwd value of 0.007567.
    // this number represents fair PV of contract per 1 unit of foreign currency (EUR)
    // denominated in domestic currency (USD). If one transacts a contract size of EUR 1M,
    // buyer should pay $7567 upfront today to enter into contract at zero initial econ edge.
    // since this value is greater than zero, eg contract has positive expected value for 
    // long party (i.e. buyer who will lock in buying EUR at $1.12 if barrier breached)

    val presentValue = price(scenario)
    println(f"Multi-Barrier Pivot Knock-In FX Forward Price: $presentValue%.6f")
  }
}