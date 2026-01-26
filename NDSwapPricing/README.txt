Purpose of code: Academic demonstration of different 
methodologies to price NDSs (non-deliverable FX swaps).
An NDS is a combination of a spot FX transaction and an
NDF (non-deliverable forward) leg. The forward rate is determined by the interest
rate differential between the 2 currencies:
NDF_Rate = SpotRate * ((1 + (InterestRate_term * Days/360)) / (1 + (InterestRate_base * Days/360)))
The actual value of the NDF at maturity is the cash settlement based on the difference
between the agreed NDF rate and the official fixing rate on the fixing date, multiplied by the 
notional amount.
The special characteristics that affect NDFs and NDSs are:
1. Tenor/maturity: Longer tenors often have wider bid-ask spreads and higher volatility 
compared to shorter ones. Therefore, the discount factors in the pricing model must be appropriate for 
the specific maturity date.
2. NDFs and NDSs are used for currencies with trading restrictions, e.g. BRL, INR. The illiquidity and
specific onshore/offshore market dynamics for these currencies can affect the expected future spot rate
which can impact NDF pricing.
3. Settlement date: Settlement is purely cash, typically in a major currency e.g. USD based on single, 
official fixing rate at specific time and date. The pricing therefore must adhere to agreed-upon fixing 
source and date to determine the final cash flow.

The codebase shows 3 broad implementations of NDS pricing:
Case 1: Basic NDS pricing
Case 2: NDS pricing via Monte Carlo simulations
Case 3: NDS pricing using full multi-curve bootstrapping.
More about Case 3: Multi-curve bootstrapping framework is used to price derivatives because it separates
the rate to forecast future cash flow values, from the rate to discount those cash flows back to PV.
For NDFs, this means using specific forecast curve for NDF's underlying interest rates (e.g. 3M USD rate)
and different, generally risk-free OIS curve for discounting all cash flows to PV.
In NDF pricing, multi-curve approach used the forecast curve (projection curve) to determine the expected
forward rate. This yield curve is built using instruments reflecting specific tenor of the NDF's underlying
rate e.g. deposits, futures, interest rate swaps with matching maturities.
The deposit yield curve (OIS curve) is constructed from OIS swaps and is used to bring the future cash 
settlement amount back to PV. In this case, the NDF rate is measured as:
NDF rate = (Spot Rate) * (DiscountFactor_TermCcy_maturityT / DiscountFactor_BaseCcy_maturityT) * 
((1 + (InterestRate_term * Days/360)) / (1 + (InterestRate_base * Days/360)))
The discount factor terms represent the discount factors derived from the appropriate OIS curves for the 
respective currencies at maturity. So the OIS and forecast curves need to be constructed for both curves 
involved in the NDF. The curves are interdependent, and the bootstrapping process involves solving a system 
of equations s.t. model prices of input instruments match market prices.