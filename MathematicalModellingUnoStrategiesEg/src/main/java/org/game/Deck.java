package org.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck{
    private List<Card> cards;
    private static final List<String> COLORS = Arrays.asList("Red", "Green", "Blue", "Yellow");
    private static final List<String> VALUES = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Skip",
            "Reverse", "Draw2");
    private static final List<String> WILD_VALUES = Arrays.asList("Wild", "Draw4");
    private Random random = new Random();

    public Deck(){
        this.cards = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    private void initializeDeck(){
        for (String color: COLORS){
            for (String value: VALUES){
                cards.add(new Card(color, value));
                if (!value.equals("0")){
                    cards.add(new Card(color, value)); // Two of each number except 0
                }
            }
        }
        for (int i = 0; i < 4; i++){
            cards.add(new Card("Wild", "Wild"));
            cards.add(new Card("Wild", "+4"));
        }
    }

    public void shuffle(){
        Collections.shuffle(cards, random);
    }

    public Card drawCard(){
        if (cards.isEmpty()){
            // In a real game, shuffle discard pile back in here
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    public List<Card> dealHand(int numCards){
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < numCards; i++){
            Card card = drawCard();
            if (card != null){
                hand.add(card);
            }
        }
        return hand;
    }
}
