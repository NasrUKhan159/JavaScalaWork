public class FXSkewSpreadAlgo {
    private double baseSpread;
    private double skewFactor;

    public FXSkewSpreadAlgo(double baseSpread, double skewFactor) {
        this.baseSpread = baseSpread;
        this.skewFactor = skewFactor;
    }

    public double calculateSpread(double marketLiquidity, double marketVolatility) {
        /**
         * Adjust spread based on liquidity and volatility
         */
        double adjustedSpread = baseSpread * (1 + marketVolatility) / marketLiquidity;
        return adjustedSpread;
    }

    public double[] applySkew(String marketSentiment) {
        /**
         * Adjust prices based on market sentiment
         */
        double bidAdjustment;
        double askAdjustment;

        if (marketSentiment.equals("bullish")) {
            bidAdjustment = -skewFactor;
            askAdjustment = skewFactor;
        } else if (marketSentiment.equals("bearish")) {
            bidAdjustment = skewFactor;
            askAdjustment = -skewFactor;
        } else {
            bidAdjustment = 0;
            askAdjustment = 0;
        }
        return new double[]{bidAdjustment, askAdjustment};
    }

    public static void main(String[] args) {
        FXSkewSpreadAlgo fxAlgo = new FXSkewSpreadAlgo(0.0002, 0.0001);
        double spread = fxAlgo.calculateSpread(100, 0.05);
        double[] adjustments = fxAlgo.applySkew("bullish");
        double bidAdjustment = adjustments[0];
        double askAdjustment = adjustments[1];

        System.out.println("Adjusted Spread: " + spread);
        System.out.println("Bid Adjustment: " + bidAdjustment + ", Ask Adjustment: " + askAdjustment);
    }
}