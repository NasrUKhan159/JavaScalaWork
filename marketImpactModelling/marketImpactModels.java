// The different mkt impact models I aim to model are:
// 1. Almgren et al (2005)
// This model assumes order completion at uniform rate of trading over a volume interval
// 2. Kissell et al (2004)
// This model assumes an instantaneous impact cost incurred by investors if all orders
// were placed in the market


public class marketImpactModels {
    private double pctAdv;
    private double mins;
    private double annVolPercent;
    private double minsInDay;
    private double inverseTurnover;
    private double annVol;
    private double thirtyDayAvgDailyVol;
    private double intervalVol;
    private double orderSize;
    private double tempImpactParam;
    private double a1;
    private double a2;
    private double a3;
    private double a4;


    public marketImpactModels(double pctAdv, double mins, double annVolPercent, double minsInDay, double inverseTurnover,
                              double annVol, double thirtyDayAvgDailyVol, double intervalVol, double orderSize,
                              double tempImpactParam, double a1, double a2, double a3, double a4) {
        this.pctAdv = pctAdv;
        this.mins = mins;
        this.annVolPercent = annVolPercent;
        this.minsInDay = minsInDay;
        this.inverseTurnover = inverseTurnover;
        this.annVol = annVol;
        this.thirtyDayAvgDailyVol = thirtyDayAvgDailyVol;
        this.intervalVol = intervalVol;
        this.orderSize = orderSize;
        this.tempImpactParam = tempImpactParam;
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.a4 = a4;
    }

    public double almgrenn(double pctAdv, double mins, double annVolPercent, double minsInDay,
                                                 double inverseTurnover)
    {
        double gamma = 0.314; // coefficient in permanent component of mkt impact model
        double permImpact = 10000 * gamma * (annVolPercent / 16) * pctAdv * Math.pow(inverseTurnover, 0.25);
        // now we compute the temporary part of model
        double dayFrac = mins / minsInDay;
        double eta = 0.142; // coefficient in temporary component of mkt impact model
        double tempImpact = 10000 * eta * (annVolPercent / 16) * Math.pow(Math.abs(pctAdv / dayFrac), 0.6);
        return (0.5 * permImpact) + tempImpact;
    }

    public double kissell(double thirtyDayAvgDailyVol, double annVol, double intervalVol, double orderSize,
                          double tempImpactParam, double a1, double a2, double a3, double a4)
    {
        // compute I = a_1 * (Q / ADV)**a_2 * sigma^{a_3} where I = impact cost
        double i_star = a1 * (Math.pow((orderSize / thirtyDayAvgDailyVol), a2) * Math.pow(annVol, a3));
        double pctOfVol = orderSize / (orderSize + thirtyDayAvgDailyVol);
        double out = tempImpactParam * i_star * Math.pow(pctOfVol, a4) + (1 - tempImpactParam)*i_star;
        return out;
    }

    public static void main(String[] args) {
        // looking at an example case of values one may see in FX for eg
        double pctAdv = 0.1;
        double mins = 0.3 * 60 * 6.5;
        double annVolPercent = 16 * 0.02; // this would assume your daily volatility (sigma) is 2%
        double minsInDay = 60 * 6.5;
        double inverseTurnover = 200; // this is inverse of daily amt i.e. for eg fraction of FX volume owned for
        // some ccypair traded in a day
        // now we get to inputs for Kissell (2004)
        double annVol = 0.2;
        double thirtyDayAvgDailyVol = 800000;
        double intervalVol = thirtyDayAvgDailyVol * 0.06;
        double orderSize = 0.01 * thirtyDayAvgDailyVol;
        // now we define the temporary and permanent parameters - these can be calibrated
        double tempImpactParam = 0.9;
        double a1 = 750;
        double a2 = 0.2;
        double a3 = 0.95;
        double a4 = 0.4;

        marketImpactModels mktImpactModels = new marketImpactModels(pctAdv, mins, annVolPercent, minsInDay,
                inverseTurnover, annVol, thirtyDayAvgDailyVol, intervalVol, orderSize, tempImpactParam, a1, a2, a3, a4);
        double almgrenn_tc = mktImpactModels.almgrenn(pctAdv, mins, annVolPercent, minsInDay, inverseTurnover);
        double kissell_tc = mktImpactModels.kissell(thirtyDayAvgDailyVol, annVol, intervalVol, orderSize, tempImpactParam, a1, a2, a3, a4);

        System.out.println("Transaction cost in bps (Almgrenn): " + almgrenn_tc);
        System.out.println("Transaction cost in bps (Kissell): " + kissell_tc);
    }
}