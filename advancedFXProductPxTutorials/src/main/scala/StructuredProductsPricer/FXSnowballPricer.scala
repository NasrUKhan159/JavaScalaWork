package StructuredProductsPricer

import scala.util.Random
import math.{exp, sqrt, log}

// 1. Data Structure Definitions
case class FXMarketData(
  spot: Double,
  domesticRate: Double, // r_d
  foreignRate: Double,  // r_f
  volatility: Double
)

case class SnowballTermSheet(
  strike: Double,
  barrier: Double,       // Mandatory upper trigger
  gearing: Double,       // Snowball levered multiplier
  fixedCoupon: Double,   // High baseline coupon
  notional: Double,
  observationTimes: List[Double] // Yearly fractions (e.g., List(0.25, 0.5, 0.75, 1.0))
)

object FXSnowballPricer {

  // 2. Monte Carlo Simulation Engine
  def price(
    market: FXMarketData, 
    terms: SnowballTermSheet, 
    numSimulations: Int, 
    stepsPerPeriod: Int = 10
  ): Double = {
    
    val rng = new Random(42) // Seeded for reproducibility
    val times = terms.observationTimes.sorted
    val totalTime = times.last
    val numPeriods = times.size
    val dt = totalTime / (numPeriods * stepsPerPeriod)
    
    // Garman-Kohlhagen Drift: r_d - r_f - 0.5 * sigma^2
    val drift = market.domesticRate - market.foreignRate - 0.5 * market.volatility * market.volatility
    val volSqrtDt = market.volatility * sqrt(dt)
    
    // Maps continuous steps back to designated observation index
    val obsSteps = times.map(t => math.round(t / dt).toInt)

    val totalPayoff = (1 to numSimulations).map { _ =>
      var currentSpot = market.spot
      var step = 0
      var currentPeriodIdx = 0
      var accumulatedBonus = 0.0
      var alive = true
      var simulationPayoff = 0.0

      while (alive && currentPeriodIdx < numPeriods) {
        // Advance path step-by-step
        val tNextObs = obsSteps(currentPeriodIdx)
        while (step < tNextObs) {
          val z = rng.nextGaussian()
          currentSpot *= exp(drift * dt + volSqrtDt * z)
          step += 1
        }

        // --- Observation Date Coupon & Trigger Evaluation ---
        val tObs = times(currentPeriodIdx)
        val df = exp(-market.domesticRate * tObs)

        if (currentSpot >= terms.barrier) {
          // Mandatory Trigger Hit (Autocall Event)
          val callPayoff = terms.notional * (terms.fixedCoupon + accumulatedBonus) * df
          simulationPayoff += callPayoff
          alive = false // Contract terminates early
        } else {
          // Snowball Mechanism Condition
          val currentCoupon = terms.fixedCoupon - terms.gearing * (currentSpot - terms.strike) / terms.strike
          
          if (currentCoupon > 0) {
            // Payoff achieved, clear accumulated memory bonus
            simulationPayoff += terms.notional * (currentCoupon + accumulatedBonus) * df
            accumulatedBonus = 0.0 
          } else {
            // Coupon drops below floor; roll forward deficit memory into the next window
            accumulatedBonus += currentCoupon
          }
          currentPeriodIdx += 1
        }
      }
      
      // Add principal protection return at maturity if not knocked out
      val finalDf = exp(-market.domesticRate * totalTime)
      val principalReturn = if (alive) terms.notional * finalDf else 0.0
      simulationPayoff + principalReturn
      
    }.sum

    totalPayoff / numSimulations
  }

  def main(args: Array[String]): Unit = {
    // EUR/USD synthetic example
    val market = FXMarketData(
        spot = 1.10,          // eg spot
        domesticRate = 0.04,  // USD Risk Free Rate (4%)
        foreignRate = 0.015,  // EUR Risk Free Rate (1.5%)
        volatility = 0.12     // 12% Implied Volatility
    )

    val terms = SnowballTermSheet(
        strike = 1.10,
        barrier = 1.15,       // Mandatory Autocall Barrier
        gearing = 2.0,        // Leverage coefficient 
        fixedCoupon = 0.08,   // 8% base coupon
        notional = 1000000.0, // $1M
        observationTimes = List(0.25, 0.50, 0.75, 1.0) // Quarterly 1-Year Note
    )

    val npv = FXSnowballPricer.price(market, terms, numSimulations = 200000)
    println(f"FX Snowball Note Present Value (NPV): $$${npv}%,.2f")
    }
}