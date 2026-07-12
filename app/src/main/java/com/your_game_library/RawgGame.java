package com.your_game_library;

import java.util.ArrayList;
import java.util.List;

public class RawgGame {
    private int id;
    private String name;
    private String backgroundImage; // URL
    private String released;
    private float rating;
    private String rawgId;
    private String slug;

    // Нові поля
    private List<String> tags;
    private List<String> genres;
    private List<String> screenshots;

    public RawgGame(int id, String name, String backgroundImage, String released, float rating, String slug) {
        this.id = id;
        this.name = name;
        this.backgroundImage = backgroundImage;
        this.released = released;
        this.rating = rating;
        this.slug = slug;
        this.tags = new ArrayList<>();
        this.genres = new ArrayList<>();
        this.screenshots = new ArrayList<>();
    }

    // --- Геттери ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getBackgroundImage() { return backgroundImage; }
    public String getReleased() { return released; }
    public float getRating() { return rating; }
    public List<String> getTags() { return tags; }
    public List<String> getGenres() { return genres; }
    public List<String> getScreenshots() { return screenshots; }
    public String getRawgId(){
            return rawgId;
    }
    public String getSlug() {
        return slug;
    }

    // --- Сеттери ---
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }
    public void setReleased(String released) { this.released = released; }
    public void setRating(float rating) { this.rating = rating; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }
    public void setGenres(List<String> genres) { this.genres = genres != null ? genres : new ArrayList<>(); }
    public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots != null ? screenshots : new ArrayList<>(); }
}