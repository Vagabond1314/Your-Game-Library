package com.your_game_library;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class IgdbGame {
    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("summary")
    public String summary;

    @SerializedName("slug")
    public String slug;

    @SerializedName("storyline")
    public String storyline;

    @SerializedName("url")
    public String url;

    @SerializedName("game_type")
    public Integer category;

    @SerializedName("first_release_date")
    public long first_release_date;

    @SerializedName("total_rating")
    public double total_rating;

    @SerializedName("aggregated_rating")
    public double aggregated_rating;

    @SerializedName("cover")
    public Cover cover;

    @SerializedName("genres")
    public List<Genre> genres;

    @SerializedName("themes")
    public List<Theme> themes;

    @SerializedName("keywords")
    public List<Keyword> keywords;

    @SerializedName("platforms")
    public List<Platform> platforms;

    @SerializedName("screenshots")
    public List<Screenshot> screenshots;

    @SerializedName("websites")
    public List<Website> websites;

    @SerializedName("language_supports")
    public List<LanguageSupport> language_supports;

    @SerializedName("similar_games")
    public List<SimilarGame> similar_games;

    @SerializedName("collection")
    public IgdbSeries series;

    @SerializedName("franchises")
    public List<IgdbSeries> franchises;

    @SerializedName("time_to_beat")
    public TimeToBeat time_to_beat;

    @SerializedName("external_games")
    public List<ExternalGame> external_games;

    // --- СТАТИЧНІ КЛАСИ ---

    public static class ExternalGame {
        public int category;
        public String url;
    }

    public static class IgdbSeries {
        @SerializedName("id")
        public int id;
        @SerializedName("name")
        public String name;
        @SerializedName("games")
        public List<GameInCollection> games;
    }

    public static class GameInCollection {
        @SerializedName("id")
        public int id;
        @SerializedName("name")
        public String name;
        @SerializedName("summary")
        public String summary;
        @SerializedName("slug")
        public String slug;
        @SerializedName("total_rating")
        public double total_rating;
        @SerializedName("first_release_date")
        public long first_release_date;
        @SerializedName("cover")
        public Cover cover;
    }

    public static class SimilarGame {
        public int id;
        public String name;
        public String summary;
        public String slug;
        public double total_rating;
        @SerializedName("first_release_date")
        public long first_release_date;
        public Cover cover;
    }

    public static class Cover { public String url; }
    public static class Genre { public String name; }
    public static class Theme { public String name; }
    public static class Keyword { public String name; }
    public static class Platform { public String name; }
    public static class Screenshot { public String url; }
    public static class Website { public String url; public int category; }

    public static class LanguageSupport {
        public Language language;
        public static class Language { public String name; }
    }

    public static class TimeToBeat { public int normally; public int completely; }
}