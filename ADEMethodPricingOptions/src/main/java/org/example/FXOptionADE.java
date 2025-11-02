package org.example;

import org.apache.commons.math3.distribution.NormalDistribution;

// in FX options, the pricing model used is the Garman-Kohlhagen model,
// which extends Black-Scholes by incorporating a foreign risk-free interest rate (r_f)
// The primary change for FX options is in the drift term of the underlying stochastic process
// Garman-Kohlhagen PDE: (\partial V/ \partial t) + (r - r_f)*F*(\partial V / \partial F)
// + (1/2) * (\sigma^{2} * F^{2}) * (\partial^{2} V / \partial F^{2}) - r*V = 0
// where F = spot exchange rate, r = domestic interest rate, r_f = foreign interest rate
// The ADE transformations remain very similar, except \alpha and \beta need to be recalculated
// to reflect the r_f term. Output calculated FX option price at t=0 (tau = T_max)

/*
    F0 = current spot exchange rate
    K = strike price
    T = time to maturity (in years)
    r_d = domestic risk-free interest rate (annualized)
    r_f = foreign risk-free interest rate (annualized)
    sigma = volatility (annualized)
    J = number of spatial (x) steps
    N = number of time (tau) steps
    x_min_mult = Multiplier for the minimum x boundary
    x_max_mult = Multiplier for the maximum x boundary
 */
public class FXOptionADE {

    public static double ADE_FX_Call(double F0, double K, double T, double r_d, double r_f,
                                     double sigma, int J, int N, double x_min_mult, double x_max_mult){
        double mu = r_d - r_f;

        // Alpha and beta coefficients adjusted for FX options
        double alpha = 0.5 - mu / (sigma * sigma);
        double beta = -0.5 - mu / (sigma * sigma) - (sigma * sigma) / 8.0;

        // Spatial boundaries X = ln(F/K)
        double x_min = x_min_mult * sigma * Math.sqrt(T);
        double x_max = x_max_mult * sigma * Math.sqrt(T);

        // Grid steps
        double dx = (x_max - x_min) / J;
        double dtau = (sigma * sigma / 2) * T / N;

        // Mesh ratio
        double lam = dtau / (dx * dx);

        // Spatial grid
        double[] x = new double[J + 1];
        for (int i = 0; i <= J; i++){
            x[i] = x_min + i * dx;
        }

        // Initialize u grid (we only need the current and next time step)
        double[] u_current = new double[J + 1];

        // Step 4: Initial Condition (at T, tau = 0)
        // Transformed payoff u(x, 0) for an FX call option
        // V(F, T) = max(F_T - K, 0)
        for (int i = 0; i <= J; i++)
        {
            double exp_x = Math.exp(x[i]);
            u_current[i] = Math.exp(alpha * x[i]) * Math.max(exp_x - 1, 0);
        }

        // Step 5: Time stepping loop (from tau = 0 to tau = T_max)
        for (int n = 0; n < N; n++){
            double[] u_next_F = new double[J + 1];
            double[] u_next_B = new double[J + 1];

            // Apply boundary conditions for u_next_F (left boundary, S=0)
            u_next_F[0] = 0;
            // Forward sweep (left to right, j = 1 to J-1)
            for (int j = 1; j < J; j++){
                u_next_F[j] = (u_current[j] + lam * (u_current[j+1] + u_next_F[j-1])) / (1 + 2*lam);
            }
            // Apply boundary conditions for u_next_B (right boundary, S=max)
            u_next_B[J] = u_current[J];

            // Backward sweep (right to left, j = J - 1 down to 1)
            for (int j = J - 1; j > 0; j--){
                u_next_B[j] = (u_current[j] + lam * (u_current[j - 1] + u_next_B[j + 1])) / (1 + 2*lam);
            }

            // Average the sweeps to get the final solution for time n+1
            for (int i = 0; i <= J; i++){
                u_current[i] = 0.5 * (u_next_F[i] + u_next_B[i]);
            }
        }
        // Step 6: Reverse the transformation

        // Find the index 'idx' corresponding to the current spot rate F0
        double x_target = Math.log(F0 / K);
        int idx = 0;
        double minDiff = Math.abs(x[0] - x_target);
        for (int i = 1; i <= J; i++) {
            double diff = Math.abs(x[i] - x_target);
            if (diff < minDiff) {
                minDiff = diff;
                idx = i;
            }
        }

        // Reverse the transform at the final tau_max to get the price at t=0
        double V0 = K * Math.exp(-alpha * x[idx] - beta * ((sigma * sigma / 2) * T)) * u_current[idx];
        return V0;
    }
    public static double ADE_FX_Call(double F0, double K, double T, double r_d, double r_f, double sigma,
                                     int J, int N){
        return ADE_FX_Call(F0, K, T, r_d, r_f, sigma, J, N, -50, 50);
    }

    // Verify with analytical Garman-Kohlhagen formula
    public static double garman_kohlhagen_call(double F, double K, double T, double r_d, double r_f, double sigma){
        NormalDistribution norm = new NormalDistribution();
        double d1 = (Math.log(F / K) + (r_d - r_f + sigma*sigma / 2) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
        return F * Math.exp(-r_f * T) * norm.cumulativeProbability(d1) - K*Math.exp(-r_d * T) * norm.cumulativeProbability(d2);
    }

    public static void main(String[] args){
        double fx_option_price_ade = ADE_FX_Call(1.3, 1.3, 1, 0.5, 0.2, 0.15, 2000, 5000);
        double fx_option_price_analytical = garman_kohlhagen_call(1.3, 1.3, 1, 0.5, 0.2, 0.15);

        System.out.println("FX Option Price (ADE method): " + fx_option_price_ade);
        System.out.println("FX Option Price (Analytical Garman-Kohlhagen): " + fx_option_price_analytical);
    }
}
