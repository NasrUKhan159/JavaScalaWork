package ndswappricing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class NdfSimplePricingCases {
    /**
     * Prices a single leg of an NDF contract using the Interest Rate Parity formula.
     * @param spotRate The current spot exchange rate (Base/Quote, e.g., USD/CNY).
     * @param rateBase The interest rate of the base currency (e.g., USD rate, as a decimal 0.05 for 5%).
     * @param rateQuote The interest rate of the quote currency (e.g., CNY rate, as a decimal 0.04 for 4%).
     * @param settlementDate The date the NDF will settle.
     * @param valuationDate The date the contract is being priced.
     * @param dayCountBasis The day count convention (e.g., 360, 365).
     * @return The calculated NDF Forward Rate.
     */
    public static double calculateNdfForwardRate(
            double spotRate,
            double rateBase,
            double rateQuote,
            LocalDate settlementDate,
            LocalDate valuationDate,
            int dayCountBasis) {

        long daysBetween = ChronoUnit.DAYS.between(valuationDate, settlementDate);
        double timeToMaturity = (double) daysBetween / dayCountBasis;

        // Interest Rate Parity Formula: F = S * (1 + R_quote * T) / (1 + R_base * T)
        double forwardRate = spotRate *
                             (1 + rateQuote * timeToMaturity) /
                             (1 + rateBase * timeToMaturity);

        return forwardRate;
    }

    /**
     * Calculates the cash settlement amount of the NDF at maturity.
     *
     * @param agreedNdfRate The forward rate agreed upon at trade inception.
     * @param fixingRate The official market spot rate on the fixing date.
     * @param notionalAmount The principal amount of the trade (in base currency).
     * @param quoteCurrencyValue The value of one unit of quote currency (typically 1.0 if settling in USD).
     * @return The final cash settlement amount in the quote currency.
     */
    public static double calculateNdfSettlement(
            double agreedNdfRate,
            double fixingRate,
            double notionalAmount,
            double quoteCurrencyValue) {
        
        // Settlement formula: Notional * (AgreedRate - FixingRate) * QuoteCurrencyValue
        double rateDifference = agreedNdfRate - fixingRate;
        double settlementAmount = notionalAmount * rateDifference * quoteCurrencyValue;

        // Note: Sign convention may vary by standard (buy/sell).
        return settlementAmount; 
    }
}
