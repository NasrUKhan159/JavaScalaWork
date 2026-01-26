package ndswappricing;

import java.util.*;

/*
* Mini version of full multi-curve bootstrapping engine 
* 1. Market Data: Simple structures for OIS and NDF quotes. 
* 2. Pricing functions: Logic to derive forward rates from a multi-curve setup.
* 3. Solver: Newton-Raphson implementation to solve for "zero rate" of curve.
*/
public class NdfPricingMultiCurveBootstrap {
    // --- 1. Market Data Structures ---
    static class MarketQuote {
        double tenorInYears;
        double rate; // Annualized rate (e.g., 0.05 for 5%)

        MarketQuote(double tenor, double rate) {
            this.tenorInYears = tenor;
            this.rate = rate;
        }
    }

    // --- 2. Yield Curve Implementation (Linear Interpolation) ---
    // The TreeMap allows for linear interpolation b/w any 2 maturity dates, 
    // ensuring we can price a "broken date" (e.g. an NDF maturing in 4.5 mths)
    static class YieldCurve {
        TreeMap<Double, Double> nodes = new TreeMap<>(); // Tenor -> Zero Rate

        void addNode(double tenor, double rate) { nodes.put(tenor, rate); }

        double getZeroRate(double t) {
            if (nodes.containsKey(t)) return nodes.get(t);
            Map.Entry<Double, Double> low = nodes.floorEntry(t);
            Map.Entry<Double, Double> high = nodes.ceilingEntry(t);
            if (low == null) return high.getValue();
            if (high == null) return low.getValue();
            // Linear Interpolation
            return low.getValue() + (high.getValue() - low.getValue()) * 
                   (t - low.getKey()) / (high.getKey() - low.getKey());
        }

        double getDiscountFactor(double t) {
            return Math.exp(-getZeroRate(t) * t);
        }
    }

    // --- 3. Solver: Newton-Raphson for Bootstrapping ---
    // Finds the Zero Rate that makes the NPV of an instrument zero.
    // This illustrates how you would "extract" curve rate from market price. 
    // In a full system, would loop through multiple market instruments (OIS,
    // Swaps, Tenor Basis Swaps) to build the full nodes map
    // Future extension: expand method to handle vector of instruments simultaneously
    // using a Jacobian matrix.
    public static double solveZeroRate(double targetPrice, double tenor, YieldCurve currentCurve) {
        double rate = 0.03; // Initial guess
        double tolerance = 1e-8;
        for (int i = 0; i < 100; i++) {
            double df = Math.exp(-rate * tenor);
            double diff = df - targetPrice; // Simple Zero-Coupon Bond logic
            if (Math.abs(diff) < tolerance) break;
            // Derivative of exp(-rt) is -t*exp(-rt)
            rate = rate - diff / (-tenor * df);
        }
        return rate;
    }

    // --- 4. Pricing Functions ---
    public static double priceNdfMultiCurve(
            double spot, 
            double tenor, 
            YieldCurve oisBase, 
            YieldCurve oisQuote, 
            YieldCurve forecastBase) {
        
        // Multi-curve logic: 
        // 1. Discount Factors from OIS curves
        double dfBaseOis = oisBase.getDiscountFactor(tenor);
        double dfQuoteOis = oisQuote.getDiscountFactor(tenor);
        
        // 2. Forecast Rate from the Projection Curve
        double forecastRateBase = forecastBase.getZeroRate(tenor);
        
        // Theoretical NDF Forward: Spot * (DF_Base_OIS / DF_Quote_OIS) * (ForwardAdjustment)
        // This is a simplified representation of the dual-curve parity
        return spot * (dfBaseOis / dfQuoteOis) * Math.exp(forecastRateBase * tenor);
    }
}
