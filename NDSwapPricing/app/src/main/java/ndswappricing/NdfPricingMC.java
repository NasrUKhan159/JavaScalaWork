package ndswappricing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/*
* Simulation of XD paths using geometric brownian motion under a risk neutral measure
* Simulate paths from today until NDF fixing date. For each path, determine final payoff of NDF 
* at maturity. Average all simulated payoffs, discount result back to PV using RFR interest rate
* to get NDF current price. MC simulation is best when we need to incorporate stochastic volatility
* or correlations between multiple risk factors which is difficult to model analytically.
*/
public class NdfPricingMC {
    public static double simulateFutureSpotRate(
            double spotRate, 
            double riskFreeRate, // Use one rate as a proxy for risk-neutral drift
            double volatility, // Annualized volatility of the exchange rate
            LocalDate valuationDate, 
            LocalDate fixingDate) {
        
        long daysBetween = ChronoUnit.DAYS.between(valuationDate, fixingDate);
        double timeToMaturity = (double) daysBetween / 365.0; // Use 365 for GBM
        Random rand = new Random();
        double randomFactor = rand.nextGaussian(); // Standard normal random number

        // GBM formula under risk-neutral measure: S(T) = S(0) * exp((r - 0.5*sigma^2)*T + sigma*sqrt(T)*Z)
        double futureSpot = spotRate * Math.exp(
            (riskFreeRate - 0.5 * volatility * volatility) * timeToMaturity + 
            volatility * Math.sqrt(timeToMaturity) * randomFactor
        );

        return futureSpot;
    }
}
