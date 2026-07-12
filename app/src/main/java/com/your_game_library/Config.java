package com.your_game_library;

public class Config {
    // Завантажуємо нашу скомпільовану бібліотеку
    static {
        System.loadLibrary("native-lib");
    }

    // Оголошуємо нативні методи (вони зв'яжуться з C++)
    public native String getIgdbClientId();
    public native String getIgdbClientSecret();
    public native String getSteamAPIKEY();

    // Зручні константи для використання в проекті
    public static final String IGDB_CLIENT_ID = new Config().getIgdbClientId();
    public static final String IGDB_CLIENT_SECRET = new Config().getIgdbClientSecret();
    public static final String STEAM_API_KEY = new Config().getSteamAPIKEY();


}