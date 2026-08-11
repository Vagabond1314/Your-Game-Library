package com.your_game_library;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Game {
    private int id;
    private String name;
    private String category; // статус користувача: planned, playing, completed
    private String description;
    private Float rating; // оцінка користувача
    private String imagePath; // локальний шлях до головного зображення
    private String imageUrl;
    private List<String> screenshotsUrl;
    private List<String> screenshots;
    private String released;
    private List<String> genres;
    private List<String> tags;     // Themes + Keywords
    private String slug;           // IGDB slug
    private String hltb;           // Час проходження

    // --- НОВІ ПОЛЯ IGDB ---
    private String storyline;      // Розширений сюжет
    private String review;
    private Integer priority;
    private Integer userRating;
    private Integer plays;
    private Float time;
    private String type;
    private String dateStartCompleted;
    private String dateEndCompleted;
    private String dateAddedPlanned;
    private String dateStartPlaying;
    private String igdbUrl;        // Посилання на сторінку IGDB
    private String gameCategory;   // Тип гри (Main Game, DLC, etc.)
    private List<String> platforms;
    private Float aggregatedRating; // Оцінка критиків
    private List<String> similarGames;
    private String collection;     // Назва серії ігор
    private List<String> languages;
    private String steamUrl;       // Пряме посилання на Steam
    private String psUrl;
    private String xboxUrl;
    private String nintendoUrl;
    private List<String> seriesGames;
    private List<String> playtimeLogs;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return id == game.id &&
                Objects.equals(name, game.name) &&
                Objects.equals(rating, game.rating) &&
                Objects.equals(imagePath, game.imagePath) &&
                Objects.equals(imageUrl, game.imageUrl); // Додай це
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, rating, imagePath, imageUrl); // І сю subtract це
    }
    // Повний конструктор
    public Game(int id, String name, String category, String description, Float rating,
                String imagePath, List<String> tags, List<String> genres,
                List<String> screenshots, String released, String hltb, String steamUrl,
                List<String> languages, List<String> similarGames, String collection, List<String> platforms,
                float aggregatedRating, String storyline, String igdbUrl, String gameCategory, String xboxUrl, String psUrl, String nintendoUrl,
                List<String> seriesGames, String imageUrl, Integer userRating, String dateStartCompleted, String dateEndCompleted, String dateAddedPlanned,
                String dateStartPlaying, String review, Integer priority, String type, Integer plays, Float time, List<String> playtimeLogs) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.rating = rating;
        this.imagePath = imagePath;
        this.tags = (tags != null) ? tags : new ArrayList<>();
        this.genres = (genres != null) ? genres : new ArrayList<>();
        this.screenshots = (screenshots != null) ? screenshots : new ArrayList<>();
        this.released = released;
        this.hltb = hltb;
        this.steamUrl = steamUrl;
        this.languages = (languages != null) ? languages : new ArrayList<>();
        this.similarGames = (similarGames != null) ? similarGames : new ArrayList<>();
        this.collection = collection;
        this.platforms = (platforms != null) ? platforms : new ArrayList<>();
        this.aggregatedRating = aggregatedRating;
        this.storyline = storyline;
        this.igdbUrl = igdbUrl;
        this.gameCategory = gameCategory;
        this.xboxUrl = xboxUrl;
        this.psUrl = psUrl;
        this.nintendoUrl = nintendoUrl;
        this.seriesGames = (seriesGames != null) ? seriesGames : new ArrayList<>();
        this.imageUrl = imageUrl;
        this.userRating = userRating;
        this.dateStartCompleted = dateStartCompleted;
        this.dateEndCompleted = dateEndCompleted;
        this.dateAddedPlanned = dateAddedPlanned;
        this.dateStartPlaying = dateStartPlaying;
        this.review = review;
        this.priority = priority;
        this.type = type;
        this.plays = plays;
        this.time = time;
        this.playtimeLogs = playtimeLogs;
    }
    public List<String> getPlaytimeLogs() { return playtimeLogs != null ? playtimeLogs : new ArrayList<>(); }
    public void setPlaytimeLogs(List<String> playtimeLogs) { this.playtimeLogs = playtimeLogs; }
    public Integer getUserRating() { return userRating; }
    public void setUserRating(int userRating) { this.userRating = userRating; }
    public String getDateStartCompleted() { return dateStartCompleted; }
    public void setDateStartCompleted(String dateStartCompleted) { this.dateStartCompleted = dateStartCompleted; }
    public String getDateEndCompleted() { return dateEndCompleted; }
    public void setDateEndCompleted(String dateEndCompleted) { this.dateEndCompleted = dateEndCompleted; }
    public String getDateAddedPlanned() { return dateAddedPlanned; }
    public void setDateAddedPlanned(String dateAddedPlanned) { this.dateAddedPlanned = dateAddedPlanned; }
    public String getDateStartPlaying() { return dateStartPlaying; }
    public void setDateStartPlaying(String dateStartPlaying) { this.dateStartPlaying = dateStartPlaying; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getPlays() { return plays; }
    public void setPlays(Integer plays) { this.plays = plays; }
    public Float getTime() { return time; }
    public void setTime(Float time) { this.time = time; }

    // --- Геттери та сеттери ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Округлення оцінки користувача (8.543 -> 8.5)
    public Float getRating() {
        if (rating == null) return 0.0f;
        return Math.round(rating * 10) / 10.0f;
    }
    public void setRating(Float rating) { this.rating = rating; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getScreenshots() { return screenshots; }
    public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots; }

    public String getReleased() { return released; }
    public void setReleased(String released) { this.released = released; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getRawgSlug() { return slug; }
    public void setRawgSlug(String slug) { this.slug = slug; }

    public String getHltb() { return hltb; }
    public void setHltb(String hltb) { this.hltb = hltb; }

    // --- ГЕТТЕРИ ТА СЕТТЕРИ ДЛЯ НОВИХ ПОЛІВ ---

    public String getStoryline() { return storyline; }
    public void setStoryline(String storyline) { this.storyline = storyline; }

    public String getIgdbUrl() { return igdbUrl; }
    public void setIgdbUrl(String igdbUrl) { this.igdbUrl = igdbUrl; }

    public String getGameCategory() { return gameCategory; }
    public void setGameCategory(String gameCategory) { this.gameCategory = gameCategory; }

    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

    // Округлення оцінки критиків
    public Float getAggregatedRating() {
        if (aggregatedRating == null) return 0.0f;
        return Math.round(aggregatedRating * 10) / 10.0f;
    }
    public void setAggregatedRating(Float aggregatedRating) { this.aggregatedRating = aggregatedRating; }

    public List<String> getSimilarGames() { return similarGames; }
    public void setSimilarGames(List<String> similarGames) { this.similarGames = similarGames; }
    public List<String> getSeriesGames() { return seriesGames; }
    public void setSeriesGames(List<String> seriesGames) { this.seriesGames = seriesGames; }

    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public String getSteamUrl() { return steamUrl; }
    public void setSteamUrl(String steamUrl) { this.steamUrl = steamUrl; }
    public String getPsUrl() { return psUrl; }
    public void setPsUrl(String psUrl) { this.psUrl = psUrl; }

    public String getXboxUrl() { return xboxUrl; }
    public void setXboxUrl(String xboxUrl) { this.xboxUrl = xboxUrl; }

    public String getNintendoUrl() { return nintendoUrl; }
    public void setNintendoUrl(String nintendoUrl) { this.nintendoUrl = nintendoUrl; }
}