package org.example;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.NormalDistribution;

public class BlackScholesADE {

    /*
        S0 = current stock price
        K = strike price
        T = time to maturity (in years)
        r = risk-free interest rate (annualized)
        sigma = volatility (annualized)
        J = number of spatial (x) steps
        N = number of time (tau) steps
        x_min_mult = Multiplier for the minimum x boundary (determines domain width)
        x_max_mult = Multiplier for the maximum x boundary
     */
    public static double[] solve_bs_ade(double S_0, double K, double T, double r, double sigma,
                                        double S_max, int n_s, int n_t) {
        // Steps 1 and 2: Transformations and grid setup:
        // Coefficients for the transformation
        double alpha_coeff = 0.5 - r / (sigma * sigma);
        double beta_coeff = -0.5 - r / (sigma * sigma) - sigma * sigma / 8.0;
        // Spatial domain X = ln(S/K)
        // Assuming S_min is close to 0, x_min is a large negative number
        double x_min = Math.log(1e-6 / K);
        double x_max = Math.log(S_max / K);

        double dx = (x_max - x_min) / n_s;

        // Time domain Tau = (sigma^2 / 2) * (T - t)
        double tau_min = 0;
        double tau_max = (sigma * sigma / 2) * T;
        double d_tau = (tau_max - tau_min) / n_t;

        // Mesh ratio lambda (often denoted as 'a' in the code)
        double a = d_tau / (dx * dx);

        // Spatial grid array (x_j)
        double[] x = new double[n_s + 1];
        for (int i = 0; i <= n_s; i++) {
            x[i] = x_min + i * dx;
        }

        // Initialize solution arrays
        // u_current holds values at time level tau_n
        // u_next_f and u_next_b hold intermediate values for tau_{n+1}
        double[] u_current = new double[n_s + 1];
        double[] u_next_f = new double[n_s + 1];
        double[] u_next_b = new double[n_s + 1];

        // Check Courant condition for explicit stability for comparison (ADE is generally better)
        if (a > 0.5) {
            System.out.printf("Warning: Standard explicit lambda > 0.5 (a = %.4f). ADE has better stability but check convergence.%n", a);
        }

        // Step 4: Initial Condition (Payoff at T, tau = 0)
        // u(x, 0) = exp(alpha * x) * max(exp(x) - 1, 0)
        for (int j = 0; j <= n_s; j++) {
            double s_val = K * Math.exp(x[j]);
            double payoff = Math.max(s_val - K, 0);
            u_current[j] = Math.exp(-alpha_coeff * x[j]) * payoff / K; // Transformed initial condition
        }

        // Step 5: Time looping (from tau = 0 to tau = max)
        for (int n = 0; n < n_t; n++) {
            // Apply boundary conditions for the current time step (these usually hold constant in transformed space)
            // Assuming far-field conditions are maintained at boundaries
            // Simple zero condition at x_min and known value at x_max
            u_next_f[0] = u_current[0];
            u_next_f[n_s] = u_current[n_s];
            u_next_b[0] = u_current[0];
            u_next_b[n_s] = u_current[n_s];

            // Forward sweep (j = 1 to n_s - 1)
            for (int j = 1; j < n_s; j++) {
                u_next_f[j] = (u_current[j] + a * (u_current[j + 1] + u_next_f[j - 1])) / (1 + 2 * a);
            }

            // Backward sweep (j = n_s - 1 down to 1)
            for (int j = n_s - 1; j > 0; j--) {
                u_next_b[j] = (u_current[j] + a * (u_current[j - 1] + u_next_f[j + 1])) / (1 + 2 * a);
            }

            // Averaging and update
            for (int j = 0; j <= n_s; j++) {
                u_current[j] = 0.5 * (u_next_f[j] + u_next_b[j]);
            }
        }

        // Step 6: Reverse Transformation to get the final option price
        // The final u_current array holds the solution at tau_max (t = 0)
        // V(S, t) = K * exp(-alpha * x - beta * tau) * u(x, tau)

        double[] final_V = new double[n_s + 1];
        for (int j = 0; j <= n_s; j++) {
            final_V[j] = K * Math.exp(-alpha_coeff * x[j] - beta_coeff * tau_max) * u_current[j];
        }

        // Find the price corresponding to the input S_0
        // Find the index closest to S_0
        double[] S_grid = new double[n_s + 1];
        for (int j = 0; j <= n_s; j++) {
            S_grid[j] = K * Math.exp(x[j]);
        }

        int idx = 0;
        double minDiff = Math.abs(S_grid[0] - S_0);
        for (int i = 1; i <= n_s; i++) {
            double diff = Math.abs(S_grid[i] - S_0);
            if (diff < minDiff) {
                minDiff = diff;
                idx = i;
            }
        }

        return new double[]{final_V[idx], S_grid[idx]};
    }

    public static double bs_analytical(double S, double K, double T, double r, double sigma) {
        NormalDistribution norm = new NormalDistribution();
        double d1 = (Math.log(S / K) + (r + sigma * sigma / 2) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
        return S * norm.cumulativeProbability(d1) - K * Math.exp(-r * T) * norm.cumulativeProbability(d2);
    }

    public static void main(String[] args) {
        double[] result = solve_bs_ade(120, 120, 1, 0.05, 0.2, 300, 200, 1000);
        double option_price = result[0];
        double s_at_idx = result[1];
        double analytical_price = bs_analytical(120, 120, 1, 0.05, 0.2);

        System.out.printf("Option price (ADE): %.6f at S = %.6f%n", option_price, s_at_idx);
        System.out.printf("Analytical Black-Scholes price: %.6f%n", analytical_price);
    }
}