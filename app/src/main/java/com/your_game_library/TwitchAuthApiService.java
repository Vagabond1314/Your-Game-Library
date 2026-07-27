package com.your_game_library;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface TwitchAuthApiService {
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<TokenResponse> getToken(
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("grant_type") String grantType
    );
}