import scala.util.Random
import math.{exp, sqrt, log}

case class FXMarketData(
  s0: Double,       // Spot FX rate (Domestic/Foreign)
  rd: Double,       // Domestic risk-free rate
  rf: Double,       // Foreign risk-free rate
  sigma: Double     // Volatility
)

case class OptionTerms(
  strike: Double,
  expiry: Double,
  numSimulations: Int
)

object MalliavinFxPricer {

  def priceAndGreeks(market: FXMarketData, option: OptionTerms): Map[String, Double] = {
    val rand = new Random()
    
    val drift = (market.rd - market.rf - 0.5 * market.sigma * market.sigma) * option.expiry
    val volSqrtT = market.sigma * sqrt(option.expiry)
    val discountDomestic = exp(-market.rd * option.expiry)

    var sumPrice = 0.0
    var sumDelta = 0.0
    var sumVega = 0.0

    for (_ <- 0 until option.numSimulations) {
      // Generate standard normal variable
      val gauss = rand.nextGaussian()
      val wT = gauss * sqrt(option.expiry)
      
      // Simulate terminal FX spot price
      val sT = market.s0 * exp(drift + market.sigma * wT)
      
      // Plain vanilla European Call payoff
      val payoff = math.max(sT - option.strike, 0.0)
      
      // Malliavin Weights
      val weightDelta = wT / (market.s0 * market.sigma * option.expiry)
      val weightVega = (wT * wT / option.expiry - 1.0) / market.sigma - wT

      // Accumulate discounted expectations
      sumPrice += discountDomestic * payoff
      sumDelta += discountDomestic * payoff * weightDelta
      sumVega  += discountDomestic * payoff * weightVega
    }

    Map(
      "Price" -> sumPrice / option.numSimulations,
      "Delta" -> sumDelta / option.numSimulations,
      "Vega"  -> sumVega  / option.numSimulations
    )
  }

  def main(args: Array[String]): Unit = {
    val market = FXMarketData(s0 = 1.20, rd = 0.05, rf = 0.02, sigma = 0.15)
    val option = OptionTerms(strike = 1.22, expiry = 1.0, numSimulations = 1000000)

    val results = priceAndGreeks(market, option)
    
    results.foreach { case (greek, value) => 
      println(f"$greek%5s: $value%.6f") 
    }
  }
}