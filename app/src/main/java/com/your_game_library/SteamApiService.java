package com.your_game_library;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SteamApiService {
    // Отримання списку куплених ігор
    @GET("IPlayerService/GetOwnedGames/v0001/")
    Call<SteamResponse> getOwnedGames(
            @Query("key") String apiKey,
            @Query("steamid") String steamId,
            @Query("include_appinfo") boolean includeAppInfo,
            @Query("format") String format
    );
}