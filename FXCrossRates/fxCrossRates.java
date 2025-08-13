// Main task: check if the following triangulation rule holds:
// log(EURCHF_t / EURCHF_{t-1}) = log(EURUSD_{t}/EURUSD_{t-1}) - log(CHFUSD_{t}/CHFUSD_{t-1})
// EURCHF close on 13 august 2025: 0.9424
// EURCHF previous close on 12 august 2025: 0.9414
// EURUSD close on 13 august 2025: 1.1704
// EURUSD previous close on 12 August 2025: 1.1679
// CHFUSD close on 13 August 2025: 1.2418
// CHFUSD previous close on 12 August 2025: 1.2405
// can extend to checking for a time series of rates and check if there is any relation
// b/w lhs and rhs results

// Given the following rates on 13 august 2025:
// 3 month sonia rate = 4.2737%
// 1-month SONIA rate: 4.2243%
// 3 month SOFR rate:  4.2271%
// 1 month SOFR rate: 4.36%
// 3 month ESTR rate: 1.92%
// 1 month ESTR rate: 1.904%
// gbpusd rate = 1.3564
// eurusd rate: 1.1706
// eurgbp rate: 0.8622
// can derived 1M and 3M GBPUSD forward rate using the above 1M and 3M rates
// Forward rate = Spot rate * ((1 + r_base) / (1 + r_term))
// note that if fwd rate is less than spot rate i.e. at a discount, we
// expect base currency to appreciate and term currency to depreciate
// infer the eurgbp fwd rate using a cross and extrapolate eurgbp forward points

public class fxCrossRates {
    public static void main(String[] args) {

        double eurchf_t = 0.9424;
        double eurchf_tMinus1 = 0.9414;
        double eurusd_t = 1.1704;
        double eurusd_tMinus1 = 1.1679;
        double chfusd_t = 1.2418;
        double chfusd_tMinus1 = 1.2405;

        double rhs_crossrule = Math.log(1.1704/1.1679) - Math.log(1.2418/1.2405);
        double lhs_crossrule = Math.log(0.9424/0.9414);
        double diff_cross_pips = (lhs_crossrule - rhs_crossrule) / 0.0001;
        System.out.println("LHS - RHS in case of cross triangulation in pips is: " + diff_cross_pips);


        double threeMthSoniaRate = 0.042737;
        double threeMthSofrRate = 0.042271;
        double threeMthEstrRate = 0.0192;
        double gbpUsdSpotRate = 1.3564;
        double eurUsdSpotRate = 1.1706;
        double eurGbpSpotRate = 0.8622;

        double threeMthEurUsdFwdRate = eurUsdSpotRate * ((1 + threeMthEstrRate) / (threeMthSofrRate));
        double threeMthGbpUsdFwdRate = gbpUsdSpotRate * ((1 + threeMthSoniaRate) / (threeMthSofrRate));
        double threeMthEurGbpFwdRateInferred = threeMthEurUsdFwdRate / threeMthGbpUsdFwdRate;
        double threeMthEurGbpFwdRateComputed = eurGbpSpotRate * ((1 + threeMthEstrRate) / (threeMthSoniaRate));
        double diff_fwdrate = threeMthEurGbpFwdRateInferred - threeMthEurGbpFwdRateComputed;
        System.out.println("Inferred - computed for forward rate is: " + diff_fwdrate);
        // this measure for fwd rate a bit inaccurate because it assumes that investors will be indifferent between investing in two
        // different currencies if the expected returns are the same when adjusted for exchange rate changes

    }
}