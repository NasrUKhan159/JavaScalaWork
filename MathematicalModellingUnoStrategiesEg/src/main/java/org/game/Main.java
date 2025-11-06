package org.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main {

    private List<Player> players;
    private Deck deck;
    private String current_color;
    private String current_value;
    private int current_player_idx;
    private int direction; // 1 for clockwise, -1 for anti-clockwise
    private Random random = new Random();

    public String runGame(List<String> strategyTypes, int terminatingTurnNumber, int numCardsEachStartsWith){
        deck = new Deck();
        players = new ArrayList<>();
        for (int i = 0; i < strategyTypes.size(); i++){
            players.add(new Player("P" + (i + 1) + "_" + strategyTypes.get(i), strategyTypes.get(i)));
        }
        for (Player player: players){
            player.setHand(deck.dealHand(numCardsEachStartsWith));
        }
        Card topCard = deck.drawCard();
        while (topCard.getColor().equals("Wild")){
            // Cannot start with a Wild card - reshuffle!
            deck.shuffle();
            topCard = deck.drawCard();
        }
        // In this simulation we don't track the full discard pile, just the top card state
        current_color = topCard.getColor();
        current_value = topCard.getValue();

        current_player_idx = 0; // what happens if this starts from 0?
        direction = 1;

        int turn_number = 0;

        while (true){
            if (turn_number==terminatingTurnNumber) return "StaleMate-3StrategiesDoNotWork";
            System.out.println("At turn number " + (turn_number + 1));
            System.out.println("Player 0 has: " + players.get(0).getHandSize() + " cards");
            System.out.println("Player 1 has: " + players.get(1).getHandSize() + " cards");
            System.out.println("Player 2 has: " + players.get(2).getHandSize() + " cards");
            Player currentPlayer = players.get(current_player_idx);

            Player.PlayResult result = currentPlayer.chooseCard(current_color, current_value);
            Card chosenCard = result.chosenCard;
            String newColor = result.newColor;

            if (chosenCard != null){
                // Update game state
                current_color = (newColor != null) ? newColor : chosenCard.getColor();
                current_value = chosenCard.getValue();

                if (currentPlayer.getHandSize() == 0){
                    return currentPlayer.toString(); // Game over, player won
                }

                // Handle action cards (simplified)
                if (current_value.equals("Skip")){
                    if (direction==-1) direction *= -1;
                    current_player_idx = (current_player_idx + direction) % players.size();
                }
                else if (current_value.equals("Reverse")){
                    direction *= -1;
                }
                else if (current_value.equals("Draw2")){
                    if (direction==-1) direction *= -1;
                    int nextPlayerIdx = (current_player_idx + direction) % players.size();
                    for (int i = 0; i < 2; i++){
                        Card drawn = deck.drawCard();
                        if (drawn != null) {
                            players.get(nextPlayerIdx).addCard(drawn);
                        }
                    }
                }
                else if (current_value.equals("Draw4")){
                    if (direction==-1) direction *= -1;
                    int nextPlayerIdx = (current_player_idx + direction) % players.size();
                    for (int i = 0; i < 4; i++){
                        Card drawn = deck.drawCard();
                        if (drawn != null) players.get(nextPlayerIdx).addCard(drawn);
                    }
                }
            } else {
                // Player draws a card, turn ends
                Card drawnCard = deck.drawCard();
                if (drawnCard != null){
                    currentPlayer.addCard(drawnCard);
                }
            }

            // Move to the next player
            current_player_idx = (current_player_idx + direction) % players.size();
            if (current_player_idx < 0) current_player_idx += players.size();
            turn_number += 1;
        }
    }

    public static void main(String[] args) {
        // List the possible set of strategies in our simple game
        List<String> strategies = Arrays.asList("random", "most_color", "save_wild", "save_draw");
        Map<String, Integer> winCounts = new HashMap<>();

        int numSimulations = 100;

        System.out.println("Running " + numSimulations + " simulations with " + strategies + " strategies...");

        for (int i = 0; i < numSimulations; i++){
            System.out.println("Running Uno game number: " + (i+1));
            Main simulation = new Main();
            String winnerName = simulation.runGame(strategies, 299, 7);
            winCounts.put(winnerName, winCounts.getOrDefault(winnerName, 0) + 1);
            System.out.println("Finished Uno game number: " + (i+1));
        }

        System.out.println("\nSimulation results:");
        for (Map.Entry<String, Integer> entry : winCounts.entrySet()){
            System.out.printf("%s won %d times (%.2f%%)\n", entry.getKey(), entry.getValue(), (double) entry.getValue() / numSimulations * 100);
        }
    }
}