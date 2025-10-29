package org.example;

import java.sql.Time;
import java.sql.Timestamp;

public class tradeData {
    public Timestamp timestamp;
    public String side;
    public double tradeRequestedPrice;
    public double size;

    // Default constructor
    public tradeData(){}

    // get, set methods
    public Timestamp getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(String timestamp){

        this.timestamp = Timestamp.valueOf(timestamp);
    }

    public String getSide(){
        return side;
    }
    public void setSide(String side){
        this.side = side;
    }

    public double getTradeRequestedPrice(){
        return tradeRequestedPrice;
    }
    public void setTradeRequestedPrice(double tradeRequestedPrice){
        this.tradeRequestedPrice = tradeRequestedPrice;
    }

    public double getSize(){
        return size;
    }
    public void setSize(double size){
        this.size = size;
    }
}
