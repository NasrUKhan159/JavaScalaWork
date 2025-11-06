package org.game;

import java.util.*;

public class Player {
    private final String name;
    private final List<Card> hand;
    private final String strategyType;
    private static final Random random = new Random();
    private static final List<String> COLORS = Arrays.asList("Red", "Green", "Blue", "Yellow");

    public Player(String name, String strategyType){
        this.name = name;
        this.hand = new ArrayList<>();
        this.strategyType = strategyType;
    }

    public void setHand(List<Card> initialHand){
        this.hand.addAll(initialHand);
    }

    public void addCard(Card card){
        if (card != null){
            hand.add(card);
        }
    }

    public int getHandSize(){
        return hand.size();
    }

    private List<Card> getPlayableCards(String topCardColor, String topCardValue){
        List<Card> playable = new ArrayList<>();
        for (Card card : hand){
            if (card.isPlayableOn(topCardColor, topCardValue)){
                playable.add(card);
            }
        }
        return playable;
    }

    public PlayResult chooseCard(String topCardColor, String topCardValue){
        List<Card> playable = getPlayableCards(topCardColor, topCardValue);

        if (playable.isEmpty()){
            return new PlayResult(null, null);
        }

        Card chosenCard;

        if (strategyType.equals("random")){
            // Strategy 1: Play a random legal card
            chosenCard = playable.get(random.nextInt(playable.size()));
        }
        else if (strategyType.equals("most_color")){
            // Strategy 2: Play a card that matches the player's most frequent color
            chosenCard = chooseCardMostColor(playable);
        }
        else if (strategyType.equals("save_wild")){
            // Strategy 3: Avoid playing Wild cards until necessary
            chosenCard = chooseCardSaveWild(playable);
        }
        else if (strategyType.equals("save_draw")){
            // Strategy 3: Avoid playing Draw2 or Draw4 cards until necessary
            chosenCard = chooseCardSaveDraw(playable);
        }
        else {
            chosenCard = playable.get(random.nextInt(playable.size()));
        }

        // Remove card from hand
        hand.remove(chosenCard);

        // Decide new color for wild cards (most frequent color in hand is common choice)
        String newColor = null;
        if (chosenCard.getColor().equals("Wild")){
            newColor = determineWildColor();
        }
        return new PlayResult(chosenCard, newColor);
    }

    private Card chooseCardMostColor(List<Card> playable){
        Map<String, Integer> colorCounts = new HashMap<>();
        for (Card card: hand){
            if (!card.getColor().equals("Wild")){
                colorCounts.put(card.getColor(), colorCounts.getOrDefault(card.getColor(), 0) + 1);
            }
        }

        String mostCommonColor = null;
        int maxCount = -1;
        for (Map.Entry<String, Integer> entry: colorCounts.entrySet()){
            if (entry.getValue() > maxCount){
                maxCount = entry.getValue();
                mostCommonColor = entry.getKey();
            }
        }

        if (mostCommonColor != null){
            List<Card> colorMatches = new ArrayList<>();
            for (Card card: playable){
                if (card.getColor().equals(mostCommonColor)){
                    colorMatches.add(card);
                }
            }
            if (!colorMatches.isEmpty()){
                return colorMatches.get(random.nextInt(colorMatches.size()));
            }
        }
        return playable.get(random.nextInt(playable.size()));
    }

    private Card chooseCardSaveWild(List<Card> playable){
        List<Card> nonWildPlayable = new ArrayList<>();
        for (Card card: playable){
            if (!card.getColor().equals("Wild")){
                nonWildPlayable.add(card);
            }
        }
        if (!nonWildPlayable.isEmpty()){
            return nonWildPlayable.get(random.nextInt(nonWildPlayable.size()));
        }
        return playable.get(random.nextInt(playable.size())); // play wild if that is only option
    }

    private Card chooseCardSaveDraw(List<Card> playable){
        List<Card> nonWildPlayable = new ArrayList<>();
        for (Card card: playable){
            if (!card.getColor().contains("Draw")){
                nonWildPlayable.add(card);
            }
        }
        if (!nonWildPlayable.isEmpty()){
            return nonWildPlayable.get(random.nextInt(nonWildPlayable.size()));
        }
        return playable.get(random.nextInt(playable.size())); // play draw if that is only option
    }

    private String determineWildColor(){
        Map<String, Integer> colorCounts = new HashMap<>();
        for (String color : COLORS){
            colorCounts.put(color, 0);
        }
        for (Card card : hand){
            if (!card.getColor().equals("Wild")){
                colorCounts.put(card.getColor(), colorCounts.get(card.getColor()) + 1);
            }
        }
        String bestColor = COLORS.get(0);
        int maxCount = -1;
        for (Map.Entry<String, Integer> entry: colorCounts.entrySet()){
            if (entry.getValue() > maxCount){
                maxCount = entry.getValue();
                bestColor = entry.getKey();
            }
        }
        return bestColor;
    }

    @Override
    public String toString(){
        return name;
    }

    // Helper class to return multiple values from chooseCard
    public static class PlayResult{
        public final Card chosenCard;
        public final String newColor;

        public PlayResult(Card chosenCard, String newColor){
            this.chosenCard = chosenCard;
            this.newColor = newColor;
        }
    }
}
