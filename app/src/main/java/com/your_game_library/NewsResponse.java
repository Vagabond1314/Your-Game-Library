package com.your_game_library;

import java.util.List;

public class NewsResponse {
    private String status;
    private int totalResults;
    private List<NewsArticle> articles;

    public List<NewsArticle> getArticles() {
        return articles;
    }
}
