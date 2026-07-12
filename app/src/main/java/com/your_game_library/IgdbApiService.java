package com.your_game_library;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface IgdbApiService {
    // 1. Отримання токена
    @POST("https://id.twitch.tv/oauth2/token")
    Call<TokenResponse> getToken(
            @Query("client_id") String clientId,
            @Query("client_secret") String clientSecret,
            @Query("grant_type") String grantType
    );

    // 2. Запит до ігор (використовує POST та текстове тіло Apicalypse)
    @POST("v4/games")
    Call<List<IgdbGame>> getGames(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String bearerToken,
            @Body String query
    );
    @POST("v4/game_time_to_beats")
    Call<List<IgdbTimeToBeat>> getTimeToBeat(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String bearerToken,
            @Body String query
    );

    @POST("v4/themes")
    Call<List<IgdbNameModel>> getThemes(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String bearerToken,
            @Body String query
    );

    @POST("v4/genres")
    Call<List<IgdbNameModel>> getGenres(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String bearerToken,
            @Body String query
    );

    @POST("v4/games")
    Call<String> getGamesRaw(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String bearerToken,
            @Body String query
    );
    @POST("v4/collections")
    Call<List<IgdbGame.IgdbSeries>> getCollections(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String bearerToken,
            @Body String query
    );

    @POST("v4/franchises")
    Call<List<IgdbGame.IgdbSeries>> getFranchises(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String token,
            @Body String query
    );

    @POST("v4/platforms")
    Call<List<IgdbNameModel>> getPlatforms(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String token,
            @Body String query
    );

    @POST("v4/languages")
    Call<List<IgdbNameModel>> getLanguages(
            @Header("Client-ID") String clientId,
            @Header("Authorization") String token,
            @Body String query
    );
}

// Універсальна модель для Жанрів та Тем
class IgdbNameModel {
    public int id;
    public String name;
    public boolean isTheme; // Допоміжне поле для нас
}