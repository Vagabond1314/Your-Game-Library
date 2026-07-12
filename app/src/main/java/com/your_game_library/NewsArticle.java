package com.your_game_library;

import com.google.gson.annotations.SerializedName;

public class NewsArticle {
    private String title;
    private String description;
    @SerializedName("url")
    private String url;

    @SerializedName("urlToImage")
    private String urlToImage;

    @SerializedName("publishedAt")
    private String publishedAt;

    // Геттери
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() {
        // Якщо поле порожнє, повертаємо "" замість null, щоб не було крашу
        return url != null ? url : "";
    }
    public String getUrlToImage() { return urlToImage; }
    public String getPublishedAt() { return publishedAt; }
}
