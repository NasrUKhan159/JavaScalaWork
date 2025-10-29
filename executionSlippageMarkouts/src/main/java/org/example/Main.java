package org.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;

// Synthetic trades generated in GBPUSD spot
// Synthetic trades at 12:{min}:00, 12:{min}:05, 12:{min}:21, 12:{min}:32, 12:{min}:43
// for min = 00, 01, 02, 03, 04, 05, 06
// tradeRequestedPrice = 1.3 + Unif[0,0.01]
// size = 1M * RandomChoice{1,2,3}

// If sell trade, markout at given time is trade requested price minus ask price, else bid price minus
// trade requested price. PnL would be this markout value times notional. Here, we compute the
// markout at time of the trade, markout one minute after, markout 2 minutes after, markout 3 mins after.

public class Main {

    public static ArrayList<tradeData> readCsv(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        ArrayList<tradeData> trades = new ArrayList<>();

        String line;
        while ((line = br.readLine())!= null){
            String[] tradeCsv = line.split(",");
            // create trade object to store trade information
            tradeData tradesObject = new tradeData();
            tradesObject.setTimestamp(tradeCsv[0]);
            tradesObject.setSide(tradeCsv[1]);
            tradesObject.setTradeRequestedPrice(Double.parseDouble(tradeCsv[2]));
            tradesObject.setSize(Double.parseDouble(tradeCsv[3]));
            trades.add(tradesObject);
        }
        return trades;
    }

    public static void generateMarkouts(ArrayList<tradeData> trades){
        ArrayList<Double> bidPx = new ArrayList<>();
        ArrayList<Double> askPx = new ArrayList<>();
        ArrayList<Integer> mktDepth = new ArrayList<>();
        ArrayList<Double> mkoutTrdTime = new ArrayList<>();
        ArrayList<Double> mkout1minAfterTrd = new ArrayList<>();
        ArrayList<Double> mkout2minAfterTrd = new ArrayList<>();
        ArrayList<Double> mkout3minAfterTrd = new ArrayList<>();
        for (tradeData trade : trades){
            mktDepth.add(1 + (int)(2 * Math.random()));
            if (trade.side.equals("Sell")){
                bidPx.add(trade.tradeRequestedPrice - 0.0002);
                askPx.add(trade.tradeRequestedPrice - 0.0001);
                mkoutTrdTime.add(trade.tradeRequestedPrice - trade.tradeRequestedPrice + 0.0001);
            }
            else{
                bidPx.add(trade.tradeRequestedPrice + 0.0002);
                askPx.add(trade.tradeRequestedPrice + 0.0003 + (Math.random() / 1000));
                mkoutTrdTime.add(trade.tradeRequestedPrice + 0.0002 - trade.tradeRequestedPrice);
            }
        }
        // compute markouts after trade time.
        for (int i = 0; i < trades.size(); i++){
            // compute markouts at 1 minute
            if (i < trades.size()-5){
                if (trades.get(i).side.equals("Sell")){
                    mkout1minAfterTrd.add(trades.get(i).tradeRequestedPrice - askPx.get(i+5));
                }
                else{
                    mkout1minAfterTrd.add(bidPx.get(i+5) - trades.get(i).tradeRequestedPrice);
                }
            }
            // compute markouts at 2 minute
            if (i < trades.size()-10){
                if (trades.get(i).side.equals("Sell")){
                    mkout2minAfterTrd.add(trades.get(i).tradeRequestedPrice - askPx.get(i+10));
                }
                else{
                    mkout2minAfterTrd.add(bidPx.get(i+10) - trades.get(i).tradeRequestedPrice);
                }
            }
            // compute markouts at 3 minute
            if (i < trades.size()-15){
                if (trades.get(i).side.equals("Sell")){
                    mkout3minAfterTrd.add(trades.get(i).tradeRequestedPrice - askPx.get(i+15));
                }
                else{
                    mkout3minAfterTrd.add(bidPx.get(i+15) - trades.get(i).tradeRequestedPrice);
                }
            }
        }
        System.out.println("Markouts at trade time across the 35 trades:");
        System.out.println(mkoutTrdTime);
        System.out.println("Markouts 1 min after trade across the first 30 trades:");
        System.out.println(mkout1minAfterTrd);
        System.out.println("Markouts 2 mins after trade across the first 25 trades:");
        System.out.println(mkout2minAfterTrd);
        System.out.println("Markouts 3 mins after trade across the first 20 trades:");
        System.out.println(mkout3minAfterTrd);
    }

    public static void main(String[] args) throws IOException {
        String filePath = "tradeData.csv";
        ArrayList<tradeData> allData = readCsv(filePath);
        generateMarkouts(allData);
    }
}