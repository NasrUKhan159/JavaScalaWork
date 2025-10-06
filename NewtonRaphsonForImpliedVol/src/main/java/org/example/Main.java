// The purpose of this script is to compute
// the implied volatility of call options using
// Newton Raphson and compare result with computation
// using the bisection method

package org.example;

import java.util.function.Function;
import java.util.ArrayList;
import org.apache.commons.math3.distribution.NormalDistribution;

public class Main {

    public static double newtonStep(Function<Double, Double> f, double x0) {
        double dx = 0.00001;
        Function<Double, Double> df = (x) -> (f.apply(x + dx) - f.apply(x)) / dx;
        return x0 - f.apply(x0) / df.apply(x0);
    }

    public static double newton(Function<Double, Double> f, double x0, double tol) {
        while (Math.abs(newtonStep(f, x0) - x0) > tol) {
            x0 = newtonStep(f, x0);
        }
        return x0;
    }

    public static double callPrice(double S, double sigma, double K, double T, double r) {
        NormalDistribution norm = new NormalDistribution();
        double d1 = (Math.log(S / K) + (r + 0.5 * Math.pow(sigma, 2)) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
        double n1 = norm.cumulativeProbability(d1);
        double n2 = norm.cumulativeProbability(d2);
        double DF = Math.exp(-r * T);
        double price = S * n1 - K * DF * n2;
        return price;
    }

    public static double inflexionPoint(double S, double K, double T, double r) {
        double m = S / (K * Math.exp(-r * T));
        return Math.sqrt(2 * Math.abs(Math.log(m)) / T);
    }

    public static double vega(double S, double sigma, double K, double T, double r){
        NormalDistribution norm = new NormalDistribution();
        double d1 = (Math.log(S / K) + (r + 0.5*Math.pow(sigma, 2)) * T) / (sigma * Math.pow(T, 0.5));
        double vega = S * Math.pow(T, 0.5) * norm.probability(d1);
        return vega;
    }

    public static double impliedVolCall(double C, double S, double K, double r, double T, double tol){
        double x0 = inflexionPoint(S, K, T, r);
        double p = callPrice(S, x0, K, T, r);
        double v = vega(S, x0, K, T, r);
        while (Math.abs((p - C) / v) > tol){
            x0 = x0 - (p - C) / v;
            p = callPrice(S, x0, K, T, r);
            v = vega(S, x0, K, T, r);
        }
        return x0;
    }

    public static double impliedVolBisectionMethod(double S, double K, double T, double r, double Price) {
        double epsilonAbs = 0.0000001;
        double epsilonStep = 0.0000001;
        int niter = 0;
        double volLower = 0.001;
        double volUpper = 1;

        while ((volUpper - volLower >= epsilonStep ||
                Math.abs(callPrice(S, volLower, K, T, r) - Price) >= epsilonAbs) &&
                epsilonAbs <= Math.abs(callPrice(S, volUpper, K, T, r) - Price)) {

            double volMid = (volLower + volUpper) / 2;
            double priceMid = callPrice(S, volMid, K, T, r);
            if (Math.abs(priceMid - Price) <= epsilonAbs) {
                break;
            } else if ((callPrice(S, volLower, K, T, r) - Price) * (priceMid - Price) < 0) {
                volUpper = volMid;
            } else {
                volLower = volMid;
            }
            niter++;
        }
        return volLower;
    }

    public static void main(String[] args) {
        double init = 0.1;
        double tol = Math.pow(10, -8);
        double S = 100; // asset price
        double K = 105; // strike price
        ArrayList<Double> tList = new ArrayList<>();
        tList.add(1.0); // 1Y time to maturity
        tList.add(0.5); // 6M time to maturity
        tList.add(0.25); // 3M time to maturity
        ArrayList<Double> rList = new ArrayList<>();
        rList.add(0.09); // risk-free interest rate set to 9%
        rList.add(0.06); // risk-free interest rate set to 6%
        rList.add(0.03); // risk-free interest rate set to 3%
        ArrayList<Double> volList = new ArrayList<>();
        volList.add(0.3); // implied volatility of 3%
        volList.add(0.2); // implied volatility of 2%
        volList.add(0.1); // implied volatility of 1%
        for (double vol: volList){
            for (double T: tList){
                for (double r: rList){
                    System.out.println("Running results for: K = " + K + " vol = " + vol + " r = " + r);
                    double C = callPrice(S, vol, K, T, r); // target price
                    Function<Double, Double> callPriceVol = (volatility) -> callPrice(S, volatility, K, T, r) - C;
                    System.out.println(newton(callPriceVol, init, tol));
                    double x0 = init;
                    for (int i = 0; i < 5; i++) {
                        System.out.println("Iteration number: " + i);
                        System.out.println(x0);
                        x0 = newtonStep(callPriceVol, x0);
                    }
                    double I = inflexionPoint(S, K, T, r);
                    System.out.println("Inflexion Point of call option:" + I);
                    double impliedVolx0 = impliedVolCall(C, S, K, r, T, tol);
                    double impliedVolBisection = impliedVolBisectionMethod(S, K, T, r, C);
                    System.out.println("Implied volatility using Bisection Method: " + impliedVolBisection);
                    System.out.println("Done with results for: K = " + K + " vol = " + vol + " r = " + r);
                }
            }
        }
    }
}