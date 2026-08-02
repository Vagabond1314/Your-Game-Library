package com.your_game_library;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SteamStoreApiService {
    @GET("api/appdetails")
    Call<JsonObject> getGamePrice(
            @Query("appids") String appId,
            @Query("cc") String countryCode // For local currency
    );
}