package com.your_game_library;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

@Keep // <-- Prevents R8 from renaming this class in Release builds!
public class TokenResponse {
    @SerializedName("access_token")
    public String access_token;

    @SerializedName("expires_in")
    public int expires_in;

    @SerializedName("token_type")
    public String token_type;
}