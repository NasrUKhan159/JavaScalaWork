package StructuredProductsPricer

import scala.math._
import scala.util.Random
import math.*

object DualCurrencyDepositPricer {

  /**
   * Cumulative standard normal distribution function (approximation).
   */
  def cnd(x: Double): Double = {
    val a1 = 0.319381530
    val a2 = -0.356563782
    val a3 = 1.781477937
    val a4 = -1.821255978
    val a5 = 1.330274429
    val p  = 0.2316419
    val L  = abs(x)
    val k  = 1.0 / (1.0 + p * L)
    
    val w = 1.0 - 1.0 / sqrt(2.0 * Pi) * exp(-L * L / 2.0) * 
      (a1 * k + a2 * k * k + a3 * pow(k, 3) + a4 * pow(k, 4) + a5 * pow(k, 5))
    
    if (x < 0) 1.0 - w else w
  }

  /**
   * Prices a European Garman-Kohlhagen option (FX extension of Black-Scholes).
   * 
   * @param s          Current spot FX rate (Base/Quote)
   * @param k          Strike price
   * @param t          Time to maturity in years
   * @param rBase      Risk-free interest rate of the base currency
   * @param rQuote     Risk-free interest rate of the quote currency
   * @param vol        Volatility of the FX pair
   * @param isCall     True for a Call option, False for a Put option
   * @return           Option value in the quote currency
   */
  def priceGarmanKohlhagen(
    s: Double, k: Double, t: Double, rBase: Double, rQuote: Double, vol: Double, isCall: Boolean
  ): Double = {
    val d1 = (log(s / k) + (rQuote - rBase + 0.5 * vol * vol) * t) / (vol * sqrt(t))
    val d2 = d1 - vol * sqrt(t)
    
    if (isCall) {
      s * exp(-rBase * t) * cnd(d1) - k * exp(-rQuote * t) * cnd(d2)
    } else {
      k * exp(-rQuote * t) * cnd(-d2) - s * exp(-rBase * t) * cnd(-d1)
    }
  }

  /**
   * Prices a Dual Currency Deposit and computes the enhanced yield.
   * 
   * @param principal  Deposit principal amount in base currency
   * @param spot       Current FX spot rate
   * @param strike     FX strike rate (conversion barrier)
   * @param days       Deposit duration in days
   * @param rBase      Standard risk-free deposit rate for base currency
   * @param rQuote     Risk-free rate for quote currency
   * @param vol        FX Volatility
   * @return           A map containing DCD evaluation metrics
   */
  def priceDCD(
    principal: Double, spot: Double, strike: Double, days: Double, 
    rBase: Double, rQuote: Double, vol: Double
  ): Map[String, Double] = {
    val t = days / 365.0
    
    // Determine option type (Assuming standard base-currency deposit where bank buys quote currency)
    // If strike < spot, bank holds a put option on base currency
    val isCall = strike > spot 
    
    // Calculate FX option premium per unit of currency
    val optionPremiumPerUnit = priceGarmanKohlhagen(spot, strike, t, rBase, rQuote, vol, isCall)
    val totalOptionPremium = optionPremiumPerUnit * principal
    
    // Normal interest earned on a plain vanilla deposit
    val standardInterest = principal * rBase * t
    
    // Enhanced payout (Standard interest + Option premium pocketed by investor)
    val enhancedInterest = standardInterest + totalOptionPremium
    
    // Annualized Enhanced Yield (Yield to Maturity)
    val enhancedYield = (enhancedInterest / principal) / t

    Map(
      "Standard Interest" -> standardInterest,
      "Option Premium"     -> totalOptionPremium,
      "Total Yield payout" -> enhancedInterest,
      "Enhanced APR (Annualized)" -> enhancedYield
    )
  }

  def main(args: Array[String]): Unit = {
    // Example Scenario: 
    // Investor deposits 100,000 EUR (Base). Alternative currency is USD (Quote).
    val principal = 100000.0
    val spotFX    = 1.1000    // 1 EUR = 1.1000 USD
    val strikeFX  = 1.0800    // Conversion strike rate
    val duration  = 30.0      // 30-day deposit
    val eurRate   = 0.015     // 1.5% EUR base rate
    val usdRate   = 0.035     // 3.5% USD quote rate
    val fxVol     = 0.10      // 10% implied volatility

    val results = priceDCD(principal, spotFX, strikeFX, duration, eurRate, usdRate, fxVol)

    println(f"--- DCD Pricing Analysis for $principal%,.2f EUR ---")
    results.foreach { case (metric, value) => 
      if (metric.contains("APR")) println(f"$metric%s: ${value * 100}%.2f%%")
      else println(f"$metric%s: $value%,.2f")
    }
  }
}