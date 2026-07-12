package com.your_game_library;

import android.util.LruCache;

public class GameCache {
    private static GameCache instance;
    private LruCache<Integer, Game> memoryCache;

    private GameCache() {
        // Ініціалізуємо кеш.
        // Замість кількості мегабайтів вказуємо кількість об'єктів (ігор).
        // 50 ігор у пам'яті заберуть зовсім мало місця (менше 2 МБ),
        // але зроблять роботу додатку блискавичною.
        int cacheSize = 50;

        memoryCache = new LruCache<Integer, Game>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Game game) {
                // Кожна гра рахується як 1 одиниця
                return 1;
            }
        };
    }

    public static synchronized GameCache getInstance() {
        if (instance == null) {
            instance = new GameCache();
        }
        return instance;
    }

    // Зберегти гру в кеш
    public void putGame(int gameId, Game game) {
        if (gameId != -1 && game != null && getGame(gameId) == null) {
            memoryCache.put(gameId, game);
        }
    }

    // Отримати гру з кешу
    public Game getGame(int gameId) {
        return memoryCache.get(gameId);
    }

    // Очистити кеш (опціонально, наприклад, при виході з додатку)
    public void clearCache() {
        memoryCache.evictAll();
    }
}