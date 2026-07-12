package com.your_game_library;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoritesManager {
    private static final String PREF_NAME = "news_prefs";
    private static final String KEY_FAVORITES = "favorite_news";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addFavorite(NewsArticle article) {
        if (article == null || article.getUrl().isEmpty()) return; // Не додаємо "биті" новини

        List<NewsArticle> favorites = getFavorites();

        // Безпечна перевірка на дублікати
        for (NewsArticle a : favorites) {
            if (a.getUrl() != null && a.getUrl().equals(article.getUrl())) {
                return; // Вже є в списку
            }
        }

        favorites.add(article);
        saveFavorites(favorites);
    }

    public void removeFavorite(String url) {
        List<NewsArticle> favorites = getFavorites();
        favorites.removeIf(article -> article.getUrl().equals(url));
        saveFavorites(favorites);
    }

    public boolean isFavorite(String url) {
        if (url == null || url.isEmpty()) return false; // Якщо шуканий URL null, це не фаворит

        List<NewsArticle> favorites = getFavorites();
        if (favorites == null) return false;

        for (NewsArticle a : favorites) {
            // Використовуємо Objects.equals (доступно з API 19+)
            // або ручну перевірку, щоб уникнути NullPointerException
            if (a.getUrl() != null && a.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    public List<NewsArticle> getFavorites() {
        String json = sharedPreferences.getString(KEY_FAVORITES, null);
        if (json == null) return new ArrayList<>();

        Gson gson = new Gson();
        Type type = new TypeToken<List<NewsArticle>>() {}.getType();
        List<NewsArticle> allSaved = gson.fromJson(json, type);

        List<NewsArticle> validFavorites = new ArrayList<>();
        boolean wasCleaned = false;

        if (allSaved != null) {
            for (NewsArticle article : allSaved) {
                // ПЕРЕВІРКА: якщо URL порожній, значить це стара зламана новина
                if (article.getUrl() != null && !article.getUrl().isEmpty()) {
                    validFavorites.add(article);
                } else {
                    wasCleaned = true; // Ми знайшли і прибрали зламаний запис
                }
            }
        }

        // Якщо ми знайшли "сміття", автоматично оновлюємо пам'ять телефону, щоб воно більше не заважало
        if (wasCleaned) {
            saveFavorites(validFavorites);
        }

        return validFavorites;
    }

    private void saveFavorites(List<NewsArticle> favorites) {
        String json = gson.toJson(favorites);
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply();
    }
}