### Personal academic projects: Advanced FX Product pricing tutorials in Scala

#### Tutorial 1: Projected SOR (Successive Over-Relaxation) method to value American FX options

Solve the Black Scholes equation as a linear complementarity problem (LCP) using Crank-Nicholson finite difference scheme.

Key components:
a. Foreign interest rate (r_f) acts similarly to a continuous dividend yield in the standard Black-Scholes model
b. Linear complementarity: Algo ensures V(S,t) >= Payoff(S) at every iteration, ensuring holder's right to exercise early.
c. SOR: Uses relaxation factor \omega to speed up convergence of implicit system of equations
d. Projection: After each SOR iteration, the calculated value is projected back onto payoff surface if it falls below intrinsic value

Scala file/s: AmericanFXPricer.scala

#### Tutorial 2: Explicit Finite Difference Method for pricing FX cash-or-nothing option

Example code shown for a synthetic FX cash-or-nothing put option.

Extensions:
- Experiment with different values of parameters for cash-or-nothing options.

Scala file/s: FXDigitalPutFDM.scala

#### Tutorial 3: Monte Carlo FX path simulator

Implement stochastic models for FX spot paths, through GBM, GARCH(1,1), stochastic vol model.
Output: 
1. Simulated FX price paths (each function represents a `DenseVector` showing price evolution)
2. Expected forward, computed as the mean of N simulated paths.
3. VaR using the 95% quantile of simulated distribution of final prices. 
4. Option payoff, which is the average of max(0, $S_{T} - K$) across all paths for European call options.

Plus, calibration of parameters is shown for GBM and GARCH(1,1) for GBPJPY spot. GBM calibration is done using maximum likelihood simulation (MLE). GARCH(1,1) is calibrated using the following negative log-likelihood:
LL = $-\sum [ln(\sigma_{t}^{2}) + \frac{r_{t}^{2}}{\sigma_{t}^{2}}]$ where $\sigma_{t} = \omega + \alpha \epsilon_{t-1}^{2} + \beta * \sigma_{t-1}^{2}$

Scala file/s: MCFXPathSimulator.scala

#### Tutorial 4: FX Exotic Structured products pricer

##### 1. Dual Currency Deposit

A dual currency deposit (DCD) is a financial derivative combining a standard time deposit with an embedded FX option. To price a DCD, compute the yield from the deposit and subtract premium of the short FX option, where the option is priced using standard Black-Scholes. As part of selling the FX option, if customer wants to buy secondary currency, they sell a put option. If customer wants to sell primary currency, they sell a call option. The premium earned from selling this option is converted into annualized interest rate format and added directly to bank's base deposit rate. This is why DCDs offer higher yields than traditional savings accounts.

##### 2. Semi-closed forms for pricing FX accumulators

An FX Accumulator decomposes into a series of forward-starting European options and barrier options matching each discrete fixing date. When utilizing a Black-Scholes framework with continuous-barrier approximations (or explicit survival probability adjustments for the discrete fixing dates), the pricing formula can be expressed in a semi-closed form. The total value of accumulator is sum of expected payoffs across all remianing fixing dates, weighted by the contract's survival probability up to that date:

$\text{Value} = \sum_{i=1}^{N} e^{-r_d t_i} \cdot P(\text{Survive to } t_i) \cdot \mathbb{E}\left[\text{Payoff}(S_{t_i})\right]$

Code implementation:
- Path Dependency & Survival: Instead of nesting loops, this semi-closed technique models the global upper knock-out barrier using an analytical multi-period path survival matrix approximation $(P(\text{Survive to } t_i))$.
- Asymmetric Risk Topology (Gearing): Incorporates leverageFactor. If the market drops below the strike, the buyer is forced to buy a multiplied notional amount (putPayoff * leverageFactor), highlighting the structural risk profile of accumulators.
- Performance: Unlike Monte Carlo structures which require intensive compute cycles, this method computes instantaneously $O(N)$ where N is the number of fixing dates, allowing rapid generation of delta and vega surface charts.

##### 3. Multi-barrier pivot knock-in forwards (CONT FROM HERE)

To price a multi-barrier pivot knock-in FX forward, the most scalable and robust framework is using a Monte Carlo simulation framework. Since multi-barrier pivot features depend heavily on entire path of spot exchange rate, closed-form analytical equations do not exist.

A pivot knock-in forward typically has an upper barrier $H_{up}$ and a lower barrier $H_{down}$. If the spot price crosses either barrier during contract's lifetime, a standard forward contract "knocks in" (activates) at a pre-agreed strike/pivot rate $K$. If neither barrier is reached, contract expires worthless or settles at distinct baseline vanilla forward rate. 

Code implementation: 
- Parallelization: MC path generations running independently.
- Early stopping rule: Internal simulation loop optimisation breaks execution immediately when a barrier is breached. 
- Thread safety: Initialising individual `Random` class instances across parallel tasks guarantees deterministic execution pipelines.

##### 4. Pricing TARFs using Monte Carlo with path branching

Pricing an FX TARF (Target Redemption Forward) using Monte Carlo with path branching requires a structured, performance-oriented architecture. Path branching is crucial for TARFs because it reduces variance around knock-out boundaries and allows for accurate estimation of Greeks near the barrier.

Key components of Pricing Engine:
1. Stochastic Process: GBM or local vol model to model the FX spot rate.
2. Path Generator: Simulates evolution of FX spot over time.
3. Branching logic: Clones paths at specific observation dates to simulate alternative realities for variance reduction and Greek calculations.
4. Pricer Engine: Orchestrates simulations, applies TARF payoff, accumulates gains and checks target cap.

##### 5. Pricing snowball coupons with mandatory triggers

Pricing an FX Snowball Coupon with a Mandatory Trigger (Autocall) involves evaluating a path-dependent exotic structured note using Monte Carlo simulation. A snowball structure accumulates unpaid coupons from prior periods, while the mandatory trigger terminates the contract early if the FX spot rate crosses a specified barrier on an observation date.

Code implementation:
- Snowball path memory: If `currentCoupon` is compressed or negative due to adverse spot movement, script avoids setting it directly to zero. It retains numerical deficit via `accumulatedBonus` to handicap future upside potential of next coupon. 
- Mandatory trigger boundary: Path iteration evaluates early check-outs (`alive = false`). Once `currentspot >= terms.barrier`, principal and accumulated interest are returned immediately, nullifying subsequent risk segments. 

Extensions: 
- Extend pricing of DCD to handle knock-out barriers or adjust day-count conventions (e.g. ACT/360 or ACT/365).
- Extend pricing of FX accumulators to compute analytic Greeks, or modify it to handle discrete monitoring correction factors (e.g. Broadie-Glasserman-Kou corrections)
- Incorporate mandatory schedule and path metrics (highest/lowest/average spot) in pricing
- Expand multi-barrier pivot knock-in engine to introduce discrete window barrier dates, or integrate local vol surfaces to improve pricing accuracy over flat assumptions.
- Adjust TARF pricing by adding root-finding solver to compute exact zero-premium strike or pivot rate needed to bring initial PV = 0.- Extend snowball pricing engine to include stochastic volatility or append down-and-in put option to handle client principal losses upon downside breaches.

Scala file/s: 
- StructuredProductsPricer/DualCurrencyDepositPricer.scala
- StructuredProductsPricer/FxAccumulatorPricer.scala
- StructuredProductsPricer/FXSnowballPricer.scala
- StructuredProductsPricer/MultiBarrierFXPricer.scala
- StructuredProductsPricer/TarfPricerApp.scala

#### Tutorial 5: Price FX options Greeks using Malliavin Calculus.

To price FX options and compute Greeks using Malliavin calculus, bypass traditional finite-difference methods (bumping). Instead, you use the Malliavin integration-by-parts formula to express Greeks as the expected value of the payoff multiplied by a Malliavin (stochastic) weight. This eliminates the need to differentiate payoff function directly, making it highly efficient for path-dependent or discontinuous FX payoffs e.g. digital options/barrier options. 

The Malliavin weights $H$ for delta and vega are computed as:

$\Delta = e^{-r_d T} \mathbb{E} \left[ f(X_T) \cdot \frac{W_T}{X_0 \sigma T} \right]$

$\nu = e^{-r_d T} \mathbb{E} \left[ f(X_T) \cdot \left( \frac{W_T^2}{\sigma T} - \frac{W_T}{\sigma \sqrt{T}} - \frac{1}{\sigma} \right) \right]$

Mathematical derivation of delta Malliavin weight (can derive vega in similar manner):

Under the domestic risk-neutral measure, the spot FX rate $X_t$ follows the stochastic differential equation:

$dX_t = (r_d - r_f)X_t dt + \sigma X_t dW_t$

By applying Itô's lemma to the log-price process $Y_t = \ln(X_t)$, we can integrate directly from $t=0$ to $t=T$:

$Y_T = \ln(X_0) + \left(r_d - r_f - \frac{1}{2}\sigma^2\right)T + \sigma W_T$

Exponentiating both sides yields the terminal asset price equation:

$X_T = X_0 \exp\left( \left(r_d - r_f - \frac{1}{2}\sigma^2\right)T + \sigma W_T \right)$

The Malliavin derivative operator $D_t$ acts on the random variable $X_T$ by measuring its sensitivity to an infinitesimal perturbation of the underlying Brownian motion path at time $t \le T$:

$D_t X_T = \frac{\partial X_T}{\partial W_T} D_t W_T = \sigma X_T \mathbf{1}_{[0,T]}(t)$

Integrating this derivative operator over the entire time horizon $[0,T]$ provides the cumulative path sensitivity:

$\int_0^T D_t X_T dt = \int_0^T \sigma X_T dt = \sigma X_T T$

The fundamental duality formula of Malliavin calculus states that the expectation of a derivative equals the expectation of the function multiplied by the Skorokhod integral (which coincides with the Itô integral for adapted processes):

$\mathbb{E}\left[ \int_0^T D_t (f(X_T)) u_t dt \right] = \mathbb{E}\left[ f(X_T) \int_0^T u_t dW_t \right]$

By applying the chain rule for Malliavin derivatives, the left-hand side can be expanded to isolate the payoff derivative $f'(X_T)$:

$D_t (f(X_T)) = f'(X_T) D_t X_T = f'(X_T) \sigma X_T$

To compute the option Delta ($\Delta$), we take the spatial derivative of the discounted expected payoff with respect to the initial spot price $X_0$:

$\Delta = \frac{\partial}{\partial X_0} \mathbb{E}\left[ e^{-r_d T} f(X_T) \right] = e^{-r_d T} \mathbb{E}\left[ f'(X_T) \frac{\partial X_T}{\partial X_0} \right]$

Differentiating the terminal price $X_T$ with respect to $X_0$ yields:

$\frac{\partial X_T}{\partial X_0} = \frac{X_T}{X_0}$

We can substitute this structural relationship into our spatial derivative equation:

$\Delta = e^{-r_d T} \mathbb{E}\left[ f'(X_T) \frac{X_T}{X_0} \right]$

To eliminate $f'(X_T)$, we express the term $f'(X_T)X_T$ via its Malliavin derivative structure:

$f'(X_T) X_T = \frac{1}{\sigma T} \int_0^T f'(X_T) \sigma X_T dt = \frac{1}{\sigma T} \int_0^T D_t (f(X_T)) dt$

Substituting this representation back into the Delta equation gives:

$\Delta = e^{-r_d T} \mathbb{E}\left[ \frac{1}{X_0 \sigma T} \int_0^T D_t (f(X_T)) dt \right]$

Choosing the constant process $u_t = 1$ allows us to apply the Malliavin integration-by-parts duality formula:

$\mathbb{E}\left[ \int_0^T D_t (f(X_T)) \cdot 1 \, dt \right] = \mathbb{E}\left[ f(X_T) \int_0^T 1 \, dW_t \right] = \mathbb{E}\left[ f(X_T) W_T \right]$

Factoring out the deterministic variables yields the final non-differentiated expectation for Delta:

$\Delta = e^{-r_d T} \mathbb{E}\left[ f(X_T) \cdot \frac{W_T}{X_0 \sigma T} \right]$

Note: 
The foreign risk free interest rate $r_{f}$ is implicitly included in calculation of Greeks because it dictates final distribution of asset price $X_{T}$. However, it does not explicitly appear in the algebraic formulae for Malliavin weights. This is because the weight is a projection onto the Brownian path ($W_{T}$) so it is scaled strictly by the diffusion coefficient $\sigma$, not the drift $r_{d} - r_{f}$. 

Code implementation:
- Adaptation done to FX options by applying the Garman-Kohlhagen model, simulating spot rate and discounting using the domestic asset.
- No payoff differentiation: Works on discontinuous payoffs e.g. binary/digital FX options.
- Single simulation pass: Compute price, delta and vega simultaneously using exact same simulated paths.
- Functional programming approach for MC simulations - `case class` structures ensure thread-safe encapsulation of market states. - For performance within single JVM node, `for` loop accumulates simulation sums rather than relying on heavy functional `.map`/`.reduce` chain, avoiding excessive object allocation overhead over the iterations.

Extensions:
- Extend this to multi-asset FX basket, and/or add local vol surface.
- Implement quasi-MC sequence for faster convergence.

Scala file/s: FXGreeksMalliavinCalculus.scala

#### Tutorial 6: Use Quasi-Monte Carlo methods to price Quanto FX Options.

Use the Sobol sequence, the Faure sequence and the Halton sequence (compare and contrast these three) in pricing quanto FX options.

To price a Quanto FX option using Quasi-Monte Carlo (QMC) methods, you need to simulate two correlated assets using a low-discrepancy sequence (like Sobol). In a Quanto FX option, the underlying asset is an FX rate, but the payoff is settled in a third currency using a fixed conversion rate. Because of this, you must apply a quanto drift adjustment to the FX rate simulation to account for the correlation between the FX rate and the payment currency's exchange rate.

For a European-style Quanto option, the pricing problem requires 2 dimensions per simulation path (Dimension 1 for the FX rate asset vector, and Dimension 2 for the payment currency exchange rate matrix used to compute the drift adjustment correlation). If you price path-dependent options (like Asian or American Quantos) across $T$ time steps, the dimensionality increases to $2T$.

Here is a comparison matrix between the 3 sequences used in quasi-MC modelling:

| Feature | Halton Sequence | Faure Sequence | Sobol Sequence |
| :--- | :--- | :--- | :--- |
| **Mathematical Basis** | Uses co-prime numbers as bases (2, 3, 5, etc.). | Generalized Pascal matrices over a single prime base &ge; dimension. | Base-2 radical inversion using directional binary integers. |
| **Dimensionality** | Excellent in low dimensions (&le; 3). | Superb uniformity in high dimensions. | Best overall for financial path generation. |
| **High-Dimension Flaw** | Correlation phenomenon (bases form lines/cycles). | Can suffer from poor uniformity if the prime base is massive. | Requires carefully chosen initialization direction numbers. |
| **Computation Speed** | Very fast (simple bit/modulo arithmetic). | Slower (requires modular matrix multiplication). | Fastest (leverages hardware bit-shifts via Gray code). |
| **Suitability for Quantos** | Great for simple, single-period European Quantos. | Best for highly path-dependent/multi-asset Quantos. | Industry standard for both vanilla and exotic Quantos. |

Methodology and Code implementation:
- In terms of performance, Sobol wins because it performs all transformations using digital bitwise operations. Since computer processors execute bitwise logic within a single clock cycle, the Sobol engine computes millions of Quanto asset simulations much faster than Halton or Faure, which rely heavily on division, modulo operations or matrix combinatorial loops. 
- High dimensionality: As a future extension, if this Quanto engine is scaled to support multi-asset or path-dependent adjustments (e.g. barrier or Asian quanto options), Halton's performance degrades quickly. If one reaches dimension 15, it relies on prime base 47. The sequence will create visible diagonal tracking lines through your random space, causing pricing model to cluster values incorrectly. However, Faure and Sobol do well here since Faure uses single base across all dimensions, preserving spatial distribution symmetry. Sobol achieves consistency across high dimensions through its directional integers.

Extensions:
- When building this pricer in a production environment, one needs to be mindful of computing high-dimensional primitive polynomials being manually prone to numerical overflow. For production environments, use optimised Java/Scala bindings e.g. Apache Commons Math.
- Extend Quanto pricing engine to multi-asset or path adjustment cases. 
- Build scrambled Halton's sequence to fix high-dimensional issues with standard Halton.
- Impact of QMC prices with correlation sign changing, and extend QMC pricing engine to compute Greeks

Scala file/s: QuantoMultiQMCPricer.scala

#### References:
1. https://www.fe.training/free-resources/financial-markets/dual-currency-deposit/
2. https://www.investopedia.com/terms/d/dualcurrencydeposit.asp
3. https://www.citibank.pl/en/investments/dual-currency-investment/
4. https://finpricing.com/lib/FxAccumulator.html
5. https://www.crisil.com/content/dam/crisil-integral-iq/what-we-think/all-our-thinking/reports/2025/11/fx-accumulator/fx-accumulator.pdf
6. https://mingze-gao.com/posts/accumulator-option-pricing/
7. https://www.bocionline.com/en/info_center/education/equity_structured_product/index.shtml
8. https://www.investopedia.com/terms/b/barrieroption.asp
9. https://www.investopedia.com/terms/k/knock-inoption.asp
10. https://www.kantox.com/glossary/knock-in-forward
11. https://www.youtube.com/watch?v=Z5BDK8mP82E&t=26
12. https://quantlib.wordpress.com/tag/fx-tarf/
13. https://www.acenumerics.com/miscellaneous/how-fast-and-accurate-is-your-fx-tarf-pricing-engine
14. https://quant.stackexchange.com/questions/80576/tarf-cumulative-profit-and-knocking-out
15. https://iongroup.com/blog/markets/handling-the-complexities-of-tarf-fx-options/
16. https://hedgebook.com/understanding-target-redemption-forwards-tarfs/
17. https://www.fideres.com/banks-push-risky-tarf-products-despite-history-of-billion-dollar-fines-and-settlements/
18. https://quant.stackexchange.com/questions/47252/how-to-price-a-phoenix-and-snowball-type-autocallable-options
19. https://idd.ice.com/IRHelp/Content/FM/Snowball.htm
20. “Applications of Malliavin Calculus to Monte Carlo Methods in Finance” (1999) by Fournié, Lasry, Lebuchoux, Lions, and Touzi
21. Fournié, E., Lasry, J. M., Lebuchoux, J., & Lions, P. L. (2001). Applications of Malliavin calculus to Monte Carlo methods in finance. II. Finance and Stochastics, 5(2), 201–236.
22. de Diego, Sergio. "Malliavin Calculus: Computation of Greeks for European Options under Black-Scholes Model." (2012).
23. Nualart, D. (2006). The Malliavin Calculus and Related Topics (2nd ed.). Springer-Verlag.
24. Gobet, E., & Kohatsu-Higa, A. (2003). Computation of Greeks using Malliavin calculus. Annals of Applied Probability, 13(1), 20-46.
25. https://www.financestrategists.com/wealth-management/alternative-investment/quantity-adjusting-option-quanto-option/
26. Jaeckel, P. (2002), Monte Carlo Methods in Finance, Wiley
27. Morokoff, W. J., & Caflisch, R. E. (1995), Quasi-Monte Carlo integration, Journal of Computational Physics, 122(2), 218-230.
28. Faure, Henri, Gracia Y. Dong, and Christiane Lemieux. "A negative dependence framework to assess different forms of scrambling." arXiv preprint arXiv:2209.02013 (2022).
29. Harrison, J. M., & Pliska, S. R. (1981), "Martingales and stochastic integrals in the theory of continuous trading", Stochastic Processes and their Applications, 11(3), 215-260.
30. Reiner, E. (1992), "Quanto Mechanics", Risk Magazine, 5(7), 147-154
31. Paskov, S. H., & Traub, J. F. (1995), "Faster Valuation of Financial Derivatives", Journal of Portfolio Management, 22(1), 113-120
32. The Mathematics of Financial Derivatives by Paul Wilmott
33. Financial Engineering and Computation by Yuh-Dauh Lyuu