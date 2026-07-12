package com.your_game_library;

public class GameCollection {
    private int id;
    private String name;
    private String imagePath;
    private int color;
    private int gameCount;
    private int showName;

    // Конструктор

    public GameCollection(int id, String name, String imagePath, int color, int gameCount, int showName) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.color = color;
        this.gameCount = gameCount;
        this.showName = showName;
    }

    // Геттери
    public int getId() { return id; }
    public String getName() { return name; }
    public String getImagePath() { return imagePath; }
    public int getColor() { return color; }
    public int getGameCount() { return gameCount; }
    public int getShowName() {return showName;}
}