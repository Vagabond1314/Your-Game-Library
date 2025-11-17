package com.example.gamestracker;

public class RawgGame {
    private int id;
    private String name;
    private String backgroundImage; // url
    private String released;
    private float rating;

    public RawgGame(int id, String name, String backgroundImage, String released, float rating) {
        this.id = id;
        this.name = name;
        this.backgroundImage = backgroundImage;
        this.released = released;
        this.rating = rating;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBackgroundImage() { return backgroundImage; }
    public String getReleased() { return released; }
    public float getRating() { return rating; }
}
