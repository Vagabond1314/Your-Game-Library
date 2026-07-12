package com.your_game_library;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface VideoGamesNewsApi {

    @GET("news")
    Call<List<NewsArticle>> getNews(
            @Header("x-rapidapi-key") String apiKey,
            @Header("x-rapidapi-host") String host,
            @Query("locale") String locale,
            @Query("country") String country,
            @Query("language") String language,
            @Query("timezone") String timezone
    );
}
