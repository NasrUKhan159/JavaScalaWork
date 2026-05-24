package StructuredProductsPricer

import scala.math._
import scala.util.Random

object FxAccumulatorPricer {

    /**
     * Represents the architectural parameters of an FX Accumulator contract.
     * 
     * @param spot Current FX Spot rate (e.g., EURUSD).
     * @param strike The fixed rate at which the buyer accumulates the asset.
     * @param knockOut The upper barrier that terminates the contract if breached.
     * @param domesticRate Risk-free interest rate of the domestic (quote) currency.
     * @param foreignRate Risk-free interest rate of the foreign (base) currency.
     * @param volatility Annualized volatility of the FX pair.
     * @param leverageFactor Multiplier applied to volume if spot falls below the strike (Gearing).
     * @param baseNotional Notional volume per discrete fixing date.
     */
    case class AccumulatorParams(
    spot: Double,
    strike: Double,
    knockOut: Double,
    domesticRate: Double,
    foreignRate: Double,
    volatility: Double,
    leverageFactor: Double,
    baseNotional: Double
    )


    // Standard Normal Cumulative Distribution Function (CDF)
    private def cdf(x: Double): Double = {
        val t = 1.0 / (1.0 + 0.2316419 * math.abs(x))
        val d = 0.3989422804014327 * math.exp(-x * x / 2.0)
        val p = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        if (x > 0) 1.0 - p else p
    }

  /**
   * Prices the FX Accumulator using a semi-closed analytical form.
   * Calculates the discounted expected payoff for each fixing date independently.
   * 
   * @param params Market and contractual constraints.
   * @param fixingTimes A list of times (in years from today) for each scheduled fixing.
   * @return The fair net present value (NPV) of the structure from the buyer's perspective.
   */
    def price(params: AccumulatorParams, fixingTimes: List[Double]): Double = {
        val mu = params.domesticRate - params.foreignRate - 0.5 * pow(params.volatility, 2)
        val sigma = params.volatility

        fixingTimes.map { t =>
        if (t <= 0.0) 0.0 // Skip historical or current-day processed fixings
        else {
            // 1. Calculate the analytic probability that the contract survives up to time 't'
            // Uses the running maximum distribution function under Geometric Brownian Motion
            val d1_ko = (log(params.knockOut / params.spot) - mu * t) / (sigma * sqrt(t))
            val d2_ko = (log(params.knockOut / params.spot) + mu * t) / (sigma * sqrt(t))
            
            val pSurvival = cdf(d1_ko) - math.pow(params.knockOut / params.spot, (2 * mu) / pow(sigma, 2)) * cdf(-d2_ko)

            // 2. Conditioned expected payoff calculations (Ungeared upside vs Geared downside)
            val d1_strike = (log(params.spot / params.strike) + (params.domesticRate - params.foreignRate + 0.5 * pow(sigma, 2)) * t) / (sigma * sqrt(t))
            val d2_strike = d1_strike - sigma * sqrt(t)

            // Expected Spot given survival and location relative to strike
            val expSpotCall = params.spot * exp((params.domesticRate - params.foreignRate) * t) * cdf(d1_strike)
            val expSpotPut  = params.spot * exp((params.domesticRate - params.foreignRate) * t) * cdf(-d1_strike)

            // Expected vanilla options pricing elements
            val callPayoff = expSpotCall - params.strike * cdf(d2_strike)
            val putPayoff  = params.strike * cdf(-d2_strike) - expSpotPut

            // Total expected nominal payoff for this single fixing node
            // Net Payoff = (Upside Profit) - Gearing * (Downside Loss)
            val expectedPayoff = (callPayoff * 1.0) - (putPayoff * params.leverageFactor)

            // 3. Discount the survival-weighted cashflow back to present day
            val discountFactor = exp(-params.domesticRate * t)
            val fixingValue = params.baseNotional * pSurvival * expectedPayoff * discountFactor

            fixingValue
        }}.sum
    } 

    def main(args: Array[String]): Unit = {

        // Define a 1-year contract with 12 monthly observation fixings
        val monthlyFixings = List.tabulate(12)(i => (i + 1) / 12.0)

        val marketData = AccumulatorParams(
            spot = 1.1000,         // EURUSD current rate
            strike = 1.0800,       // Buyer accumulates at a discount
            knockOut = 1.1500,     // If spot touches 1.15, contract terminates
            domesticRate = 0.040,  // USD interest rate (4%)
            foreignRate = 0.025,   // EUR interest rate (2.5%)
            volatility = 0.10,     // 10% FX Volatility
            leverageFactor = 2.0,  // 2x leverage obligation below strike (Gearing)
            baseNotional = 10000.0 // 10,000 EUR per fixing node
        )

        val npv = FxAccumulatorPricer.price(marketData, monthlyFixings)
        
        println(f"=== FX Accumulator Valuation ===")
        println(f"Spot Rate:         ${marketData.spot}%.4f")
        println(f"Strike Rate:       ${marketData.strike}%.4f")
        println(f"Knock-Out Barrier: ${marketData.knockOut}%.4f")
        println(f"Leverage Gearing:  ${marketData.leverageFactor}x")
        println(f"Calculated NPV:    USD $npv%.2f")
    }
}