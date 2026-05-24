package StructuredProductsPricer

import scala.util.Random
import math.{exp, sqrt, log}

// 1. Core Financial Data Models
case class MarketData(spot: Double, domesticRate: Double, foreignRate: Double, volatility: Double)

case class TarfSchedule(
  observationTimes: List[Double], // e.g., List(0.25, 0.50, 0.75, 1.0)
  strike: Double,
  leverage: Double,               // e.g., 2.0 for out-of-the-money put/call
  targetCap: Double,              // Target profit cap to knock out
  isCall: Boolean                 // True for Call TARF, False for Put TARF
)

case class SimulationPath(times: List[Double], spots: List[Double])

// 2. Stochastic Process Implementation
class FXGarmanKohlhagen(market: MarketData) {
  // Drift under domestic risk-neutral measure: r_d - r_f - 0.5 * sigma^2
  private val drift = market.domesticRate - market.foreignRate - 0.5 * market.volatility * market.volatility
  private val sigma = market.volatility

  def evolve(spot: Double, dt: Double, normalZ: Double): Double = {
    spot * exp(drift * dt + sigma * sqrt(dt) * normalZ)
  }
}

// 3. Monte Carlo Path Generator with Branching Capabilities
class PathGenerator(process: FXGarmanKohlhagen, rand: Random = new Random()) {
  
  def nextGaussian(): Double = rand.nextGaussian()

  /**
   * Generates a step from a single point to the next time horizon.
   */
  def simulateStep(currentSpot: Double, t0: Double, t1: Double): Double = {
    val dt = t1 - t0
    if (dt <= 0) currentSpot
    else process.evolve(currentSpot, dt, nextGaussian())
  }
}

// 4. TARF Payoff and Branching Engine
class TarfEngine(
  market: MarketData, 
  schedule: TarfSchedule, 
  numMainPaths: Int, 
  branchingFactor: Int
) {
  
  private val process = new FXGarmanKohlhagen(market)
  private val generator = new PathGenerator(process)

  /**
   * Calculates the payoff of a single observation fix.
   */
  def calculateFixingProfit(spot: Double): Double = {
    if (schedule.isCall) {
      if (spot > schedule.strike) spot - schedule.strike
      else schedule.leverage * (spot - schedule.strike)
    } else { // Put TARF
      if (spot < schedule.strike) schedule.strike - spot
      else schedule.leverage * (schedule.strike - spot)
    }
  }

  /**
   * Runs the Monte Carlo simulation using Recursive Path Branching.
   */
  def price(): Double = {
    val times = 0.0 :: schedule.observationTimes
    
    // Total accumulated value across all main simulation paths
    val totalPayoff = (1 to numMainPaths).map { _ =>
      runBranchedPath(
        currentSpot = market.spot,
        timeIndex = 0,
        accumulatedProfit = 0.0,
        times = times
      )
    }.sum

    val averagePayoff = totalPayoff / numMainPaths
    // Discount back to T0 using domestic risk-neutral rate
    val finalExpiry = schedule.observationTimes.last
    averagePayoff * exp(-market.domesticRate * finalExpiry)
  }

  /**
   * Tail-recursive or tree-walking simulation that branches execution paths 
   * at each fixing date to reduce variance on the knock-out boundary condition.
   */
  private def runBranchedPath(
    currentSpot: Double, 
    timeIndex: Int, 
    accumulatedProfit: Double, 
    times: List[Double]
  ): Double = {
    
    // Base Case: If we reached the end of the schedule
    if (timeIndex >= times.length - 1) {
      0.0
    } else {
      val t0 = times(timeIndex)
      val t1 = times(timeIndex + 1)
      
      // Branch paths from the current node to the next observation date
      val branchPayoffs = (1 to branchingFactor).map { _ =>
        val nextSpot = generator.simulateStep(currentSpot, t0, t1)
        val fixingProfit = calculateFixingProfit(nextSpot)
        val newAccumulatedProfit = accumulatedProfit + fixingProfit
        
        // Check Knock-Out (Target Cap) Condition
        if (newAccumulatedProfit >= schedule.targetCap) {
          // If knocked out, payoff is capped to deliver exactly the target up to this point
          val terminalPayoff = schedule.targetCap - accumulatedProfit
          terminalPayoff
        } else {
          // If not knocked out, collect current payoff and evolve deeper into the tree
          val futurePayoff = runBranchedPath(nextSpot, timeIndex + 1, newAccumulatedProfit, times)
          fixingProfit + futurePayoff
        }
      }.sum

      // Average the payoffs over the branching factor (Conditional Expectation)
      branchPayoffs / branchingFactor
    }
  }
}

// 5. Execution Wrapper
object TarfPricerApp extends App {
  // Setup standard FX Option Parameters (e.g., EURUSD style)
  val market = MarketData(
    spot = 1.1000, 
    domesticRate = 0.04,  // USD rate
    foreignRate = 0.02,   // EUR rate
    volatility = 0.12     // 12% Vol
  )

  val schedule = TarfSchedule(
    observationTimes = List(0.25, 0.50, 0.75, 1.0), // Monthly/Quarterly fixings
    strike = 1.1200,
    leverage = 2.0,
    targetCap = 0.0500,  // Knocks out once 500 pips of profit accumulate
    isCall = true
  )

  val numMainPaths = 50000
  val branchingFactor = 10 // Each path splits into 10 sub-paths at every fixing date

  println("Initializing Monte Carlo TARF Engine with Path Branching...")
  val engine = new TarfEngine(market, schedule, numMainPaths, branchingFactor)
  
  val startTime = System.currentTimeMillis()
  val price = engine.price()
  val endTime = System.currentTimeMillis()

  println(f"Calculated TARF PV: $price%.6f")
  println(s"Execution Time: ${endTime - startTime} ms")
    // the TARF PV we obtain in this example case is -0.289148.
  // economic interpretation of this negative PV can be decomposed into 3 components:
  // 1. Short-vol downside leverage: when client buys call TARF, they are buying asymmetric strip
  // of call options (giving them right to buy foreign ccy at enhanced rate). To pay for these calls
  // without spending cash upfront, client simultaneously sells strip of put options to bank
  // In eg, `leverage` param is set to 2 so if spot falls below 1.12 strike, client is legally forced 
  // to buy twice the notional amount at that unfavourable rate. Negative PV proves that liability of short,
  // leveraged put outweighs value of long call.
  // 2. Asymmetric knock-out feature: `targetCap` of 0.05 acts as automatic circuit breaker, but it only 
  // applies to client's gains. If mkt goes up and client accumulates 500 pips of profit, bank terminates 
  // structure to limit its losses. If mkt goes down, there is no target loss cap. Structure remains active, 
  // and client continues losing `leverage` = 2x at every fixing date until final expiration. This structural
  // asymmetry inherently forces negative value onto contract.
  // 3. OTM inception: Contract was initialized with FX spot = 1.1 and call strike = 1.12
  // since it is OTM call TARF, spot rate must climb significantly for client to break even. 
  // Under domestic risk-neutral probability, prob of drifting deeply into leveraged losing
  // territory is much higher than hitting capped profit boundary.
  // However, in real world scenario, client will not sign contract where there is expected loss of nearly 29%
  // of notional. A pricing platform needs to select params (i.e. strike, premium and leverage) s.t. contract 
  // is initated at zero PV.
}
