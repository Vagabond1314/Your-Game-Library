package com.your_game_library;

import androidx.annotation.Keep;
@Keep
public class Config {
    // Завантажуємо нашу скомпільовану бібліотеку
    static {
        System.loadLibrary("native-lib");
    }

    // Оголошуємо нативні методи (вони зв'яжуться з C++)
    public static native String getIgdbClientId();
    public static native String getIgdbClientSecret();
    public static native String getSteamAPIKEY();

    // Зручні константи для використання в проекті
    public static final String IGDB_CLIENT_ID = new Config().getIgdbClientId();
    public static final String IGDB_CLIENT_SECRET = new Config().getIgdbClientSecret();
    public static final String STEAM_API_KEY = new Config().getSteamAPIKEY();


}