package com.your_game_library;

public class WheelItem {
    private Game game;
    private boolean isIncluded;
    private int weight; // 1 - стандартно, 2 - вдвічі більше і т.д.

    public WheelItem(Game game) {
        this.game = game;
        this.isIncluded = true;
        this.weight = 1;
    }

    public Game getGame() { return game; }
    public boolean isIncluded() { return isIncluded; }
    public void setIncluded(boolean included) { isIncluded = included; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}