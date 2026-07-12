package com.your_game_library;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    @GET("v2/everything")
    Call<NewsResponse> getNews(
            @Query("q") String query,
            @Query("domains") String domains, // Додаємо цей параметр
            @Query("language") String lang,
            @Query("sortBy") String sortBy,
            @Query("pageSize") int pageSize,
            @Query("apiKey") String apiKey
    );
}
