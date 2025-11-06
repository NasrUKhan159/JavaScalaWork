package org.game;

public class Card {
    private final String color;
    private final String value;

    public Card(String color, String value){
        this.color = color;
        this.value = value;
    }

    public String getColor(){
        return color;
    }

    public String getValue(){
        return value;
    }

    public boolean isPlayableOn(String topCardColor, String topCardValue){
        if (this.color.equals("Wild")){
            return true;
        }
        if (this.color.equals(topCardColor)){
            return true;
        }
        if (this.color.equals(topCardValue)){
            return true;
        }
        return false;
    }

    @Override
    public String toString(){
        return this.color + " " + this.value;
    }
}
