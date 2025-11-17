package com.example.gamestracker;

public class Game {
    private int id;
    private String name;
    private String category;
    private String description;
    private Float rating;
    private String imagePath;  // шлях до файла

    public Game(int id, String name, String category, String description,
                Float rating, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.rating = rating;
        this.imagePath = imagePath;
    }

    public Game(String name, String category, String description,
                Float rating, String imagePath) {
        this(-1, name, category, description, rating, imagePath);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public Float getRating() { return rating; }
    public String getImagePath() { return imagePath; }

    public void setId(int id) { this.id = id; }
}

