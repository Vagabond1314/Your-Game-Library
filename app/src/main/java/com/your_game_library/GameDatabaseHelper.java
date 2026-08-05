package com.your_game_library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "games.db";
    private static final int DATABASE_VERSION = 17;
    private static GameDatabaseHelper instance;

    public GameDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Вмикаємо підтримку зовнішніх ключів і каскадного видалення
        db.setForeignKeyConstraintsEnabled(true);
        migrateLegacyStatsToColumns(db);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE games (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE," +
                "category TEXT," +
                "description TEXT," +
                "rating REAL," +
                "image_path TEXT," +
                "tags TEXT," +
                "genres TEXT," +
                "screenshots TEXT," +
                "released TEXT," +
                "hltb TEXT," +
                "storyline TEXT," +
                "igdb_url TEXT," +
                "game_category TEXT," +
                "platforms TEXT," +
                "aggregated_rating REAL," +
                "similar_games TEXT," +
                "collection TEXT," +
                "languages TEXT," +
                "steam_url TEXT," +
                "created_at INTEGER," +
                "xbox_url TEXT," +
                "playstation_url TEXT," +
                "nintendo_url TEXT," +
                "series_games TEXT," +
                "image_url TEXT," +
                "userRating INTEGER," +
                "review TEXT," +
                "priority INTEGER," +
                "plays INTEGER," +
                "time FLOAT," +
                "type TEXT," +
                "dateStartCompleted TEXT," +
                "dateEndCompleted TEXT," +
                "dateAddedPlanned TEXT," +
                "dateStartedPlaying TEXT" +
                ")";
        db.execSQL(CREATE_TABLE);
        db.execSQL("CREATE TABLE collections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "image_path TEXT," +
                "color INTEGER," +
                "position_index INTEGER," +
                "show_name INTEGER" +
                ")");
        // Таблиця зв'язків (багато до багатьох)
        db.execSQL("CREATE TABLE collection_games (" +
                "collection_id INTEGER," +
                "game_id INTEGER," +
                "PRIMARY KEY(collection_id, game_id)," +
                "FOREIGN KEY(collection_id) REFERENCES collections(id) ON DELETE CASCADE," +
                "FOREIGN KEY(game_id) REFERENCES games(id) ON DELETE CASCADE)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE games ADD COLUMN storyline TEXT");
                db.execSQL("ALTER TABLE games ADD COLUMN igdb_url TEXT");
                db.execSQL("ALTER TABLE games ADD COLUMN game_category TEXT");
                db.execSQL("ALTER TABLE games ADD COLUMN platforms TEXT");
                db.execSQL("ALTER TABLE games ADD COLUMN aggregated_rating REAL");
                db.execSQL("ALTER TABLE games ADD COLUMN similar_games TEXT");
                db.execSQL("ALTER TABLE games ADD COLUMN collection TEXT");
                db.execSQL("ALTER TABLE games ADD COLUMN languages TEXT");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE games ADD COLUMN steam_url TEXT");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE games ADD COLUMN created_at INTEGER");
        }
        if(oldVersion < 12){
            db.execSQL("ALTER TABLE games ADD COLUMN xbox_url TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN playstation_url TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN nintendo_url TEXT");
        }
        if (oldVersion < 13) {
            db.execSQL("ALTER TABLE games ADD COLUMN series_games TEXT");
        }
        if (oldVersion < 14) {
            db.execSQL("ALTER TABLE games ADD COLUMN image_url TEXT");
        }
        if (oldVersion < 15) {
            db.execSQL("CREATE TABLE collections (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, image_path TEXT, color INTEGER, position_index INTEGER)");
            db.execSQL("CREATE TABLE collection_games (collection_id INTEGER, game_id INTEGER, PRIMARY KEY(collection_id, game_id))");
        }
        if (oldVersion < 16) {
            try {
                db.execSQL("ALTER TABLE collections ADD COLUMN show_name INTEGER DEFAULT 1");
            } catch (Exception e) {
                Log.e("DB_UPGRADE", "Column show_name might already exist");
            }
        }
        if(oldVersion < 17) {
            db.execSQL("ALTER TABLE games ADD COLUMN userRating INTEGER");
            db.execSQL("ALTER TABLE games ADD COLUMN review TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN priority INTEGER");
            db.execSQL("ALTER TABLE games ADD COLUMN plays INTEGER");
            db.execSQL("ALTER TABLE games ADD COLUMN time FLOAT");
            db.execSQL("ALTER TABLE games ADD COLUMN type TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN dateStartCompleted TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN dateEndCompleted TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN dateAddedPlanned TEXT");
            db.execSQL("ALTER TABLE games ADD COLUMN dateStartedPlaying TEXT");

            migrateLegacyStatsToColumns(db);
        }
    }

    public static synchronized GameDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            // Використовуємо getApplicationContext(), щоб уникнути витоку пам'яті
            instance = new GameDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    // Отримати гру по точній назві (для перевірки дублікатів при імпорті)
    public Game getGameByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Game game = null;
        Cursor cursor = db.query("games", null, "name=?", new String[]{name}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            game = cursorToGame(cursor);
            cursor.close();
        }
        return game;
    }
    // Допоміжний метод для конвертації CSV рядка з БД у List<String>
    private List<String> stringToList(String data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(data.split(",")));
    }

    // Додати гру
    // Додати гру з урахуванням усіх нових полів статистики
    public long addGame(Game game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 1. НАЙВАЖЛИВІШЕ: Тільки якщо ID валідний (більше нуля), ми передаємо його в базу.
        // Якщо це 0 або -1 (нова гра), ми взагалі не чіпаємо колонку "id",
        // щоб SQLite на 100% самостійно згенерував AUTOINCREMENT.
        if (game.getId() > 0) {
            values.put("id", game.getId());
        }

        values.put("name", game.getName());
        values.put("category", game.getCategory());
        values.put("description", game.getDescription());
        values.put("rating", game.getRating());
        values.put("image_path", game.getImagePath());
        values.put("tags", String.join(",", game.getTags()));
        values.put("genres", String.join(",", game.getGenres()));
        values.put("screenshots", String.join(",", game.getScreenshots()));
        values.put("released", game.getReleased());
        values.put("storyline", game.getStoryline());
        values.put("igdb_url", game.getIgdbUrl());
        values.put("game_category", game.getGameCategory());
        values.put("platforms", String.join(",", game.getPlatforms()));
        values.put("aggregated_rating", game.getAggregatedRating());
        values.put("similar_games", String.join(",", game.getSimilarGames()));
        values.put("series_games", String.join(",", game.getSeriesGames()));
        values.put("collection", game.getCollection());
        values.put("languages", String.join(",", game.getLanguages()));
        values.put("steam_url", game.getSteamUrl());
        values.put("hltb", game.getHltb());
        values.put("created_at", System.currentTimeMillis());
        values.put("xbox_url", game.getXboxUrl());
        values.put("playstation_url", game.getPsUrl());
        values.put("nintendo_url", game.getNintendoUrl());
        values.put("image_url", game.getImageUrl());

        values.put("userRating", game.getUserRating());
        values.put("review", game.getReview());
        values.put("priority", game.getPriority());
        values.put("plays", game.getPlays());
        values.put("time", game.getTime());
        values.put("type", game.getType());
        values.put("dateStartCompleted", game.getDateStartCompleted());
        values.put("dateEndCompleted", game.getDateEndCompleted());
        values.put("dateAddedPlanned", game.getDateAddedPlanned());
        values.put("dateStartedPlaying", game.getDateStartPlaying());

        long id = -1;
        try {
            // Повертаємося до найнадійнішого методу вставки, який ніколи не крашить додаток.
            // CONFLICT_IGNORE гарантує, що якщо гра з такою назвою вже є (UNIQUE),
            // база просто проігнорує запис, а не впаде і не видалить стару гру.
            id = db.insertWithOnConflict("games", null, values, SQLiteDatabase.CONFLICT_IGNORE);

            if (id == -1) {
                Log.e("SQLITE_ERROR", "Не вдалося записати гру [" + game.getName() + "]. Можливо, така назва вже існує.");
            }
        } catch (Exception e) {
            Log.e("SQLITE_ERROR", "Невідома помилка при записі в БД: " + e.getMessage());
        } finally {
            db.close();
        }

        return id;
    }

    // Отримати гру по ID
    public Game getGameByIdObject(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Game game = null;
        Cursor cursor = db.query("games", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            game = cursorToGame(cursor);
            cursor.close();
        }
        return game;
    }

    // Універсальний метод перетворення рядка курсора в об'єкт Game
    private Game cursorToGame(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
        String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        Float rating = cursor.isNull(cursor.getColumnIndexOrThrow("rating")) ? 0.0f : cursor.getFloat(cursor.getColumnIndexOrThrow("rating"));
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
        String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url"));
        String released = cursor.getString(cursor.getColumnIndexOrThrow("released"));
        String hltb = cursor.getString(cursor.getColumnIndexOrThrow("hltb"));
        String steamUrl = cursor.getString(cursor.getColumnIndexOrThrow("steam_url"));
        String xboxUrl = cursor.getString(cursor.getColumnIndexOrThrow("xbox_url"));
        String nintendoUrl = cursor.getString(cursor.getColumnIndexOrThrow("nintendo_url"));
        String playstationUrl = cursor.getString(cursor.getColumnIndexOrThrow("playstation_url"));
        String storyline = cursor.getString(cursor.getColumnIndexOrThrow("storyline"));
        String igdbUrl = cursor.getString(cursor.getColumnIndexOrThrow("igdb_url"));
        String gameCategory = cursor.getString(cursor.getColumnIndexOrThrow("game_category"));
        String collection = cursor.getString(cursor.getColumnIndexOrThrow("collection"));
        float aggregatedRating = cursor.getFloat(cursor.getColumnIndexOrThrow("aggregated_rating"));
        Integer userRating = cursor.isNull(cursor.getColumnIndexOrThrow("userRating")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("userRating"));
        String dateStartCompleted = cursor.getString(cursor.getColumnIndexOrThrow("dateStartCompleted"));
        String dateEndCompleted = cursor.getString(cursor.getColumnIndexOrThrow("dateEndCompleted"));
        String dateAddedPlanned = cursor.getString(cursor.getColumnIndexOrThrow("dateAddedPlanned"));
        String dateStartedPlaying = cursor.getString(cursor.getColumnIndexOrThrow("dateStartedPlaying"));
        String review = cursor.getString(cursor.getColumnIndexOrThrow("review"));
        Integer priority = cursor.isNull(cursor.getColumnIndexOrThrow("priority")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
        String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        Integer plays = cursor.isNull(cursor.getColumnIndexOrThrow("plays")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("plays"));
        Float time = cursor.isNull(cursor.getColumnIndexOrThrow("time")) ? null : cursor.getFloat(cursor.getColumnIndexOrThrow("time"));

        List<String> tags = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("tags")));
        List<String> genres = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("genres")));
        List<String> screenshots = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("screenshots")));
        List<String> languages = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("languages")));
        List<String> similarGames = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("similar_games")));
        List<String> seriesGames = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("series_games")));
        List<String> platforms = stringToList(cursor.getString(cursor.getColumnIndexOrThrow("platforms")));
        //Log.d("PRIORITY_DEBUG", "Зчитано з БД: " + name + " | Колонка priority = " + priority);

        return new Game(id, name, category, description, rating, imagePath, tags, genres, screenshots,
                released, hltb, steamUrl, languages, similarGames, collection, platforms,
                aggregatedRating, storyline, igdbUrl, gameCategory, xboxUrl, playstationUrl, nintendoUrl,seriesGames,imageUrl,
                userRating, dateStartCompleted, dateEndCompleted, dateAddedPlanned, dateStartedPlaying, review, priority, type, plays, time);
    }

    // Оновити гру
    public boolean updateGame(Game game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", game.getName());
        values.put("category", game.getCategory());
        values.put("description", game.getDescription());
        values.put("rating", game.getRating());
        values.put("image_path", game.getImagePath());
        values.put("tags", String.join(",", game.getTags()));
        values.put("genres", String.join(",", game.getGenres()));
        values.put("screenshots", String.join(",", game.getScreenshots()));
        values.put("released", game.getReleased());
        values.put("storyline", game.getStoryline());
        values.put("igdb_url", game.getIgdbUrl());
        values.put("game_category", game.getGameCategory());
        values.put("platforms", String.join(",", game.getPlatforms()));
        values.put("aggregated_rating", game.getAggregatedRating());
        values.put("similar_games", String.join(",", game.getSimilarGames()));
        values.put("series_games", String.join(",", game.getSeriesGames()));
        values.put("collection", game.getCollection());
        values.put("languages", String.join(",", game.getLanguages()));
        values.put("steam_url", game.getSteamUrl());
        values.put("hltb", game.getHltb());
        values.put("xbox_url", game.getXboxUrl());
        values.put("playstation_url", game.getPsUrl());
        values.put("nintendo_url", game.getNintendoUrl());
        values.put("image_url", game.getImageUrl());
        values.put("userRating", game.getUserRating());
        values.put("review", game.getReview());
        values.put("priority", game.getPriority());
        values.put("plays", game.getPlays());
        values.put("time", game.getTime());
        values.put("type", game.getType());
        values.put("dateStartCompleted", game.getDateStartCompleted());
        values.put("dateEndCompleted", game.getDateEndCompleted());
        values.put("dateAddedPlanned", game.getDateAddedPlanned());
        values.put("dateStartedPlaying", game.getDateStartPlaying());

        int rows = db.update("games", values, "id=?", new String[]{String.valueOf(game.getId())});
        db.close();
        return rows > 0;
    }

    public List<Game> getAllGames() {
        List<Game> gamesList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("games", null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                gamesList.add(cursorToGame(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return gamesList;
    }
    // Метод динамічно додає фільтр в SQL запит, щоб приховувати ігри без потрібних даних
    private void appendFilterForSort(StringBuilder selection, String orderBy, String tablePrefix) {
        if (orderBy == null || orderBy.isEmpty()) return;

        String cleanOrder = orderBy.trim();
        if (cleanOrder.endsWith(" ASC")) cleanOrder = cleanOrder.replace(" ASC", "").trim();
        if (cleanOrder.endsWith(" DESC")) cleanOrder = cleanOrder.replace(" DESC", "").trim();
        if (cleanOrder.startsWith("g.")) cleanOrder = cleanOrder.substring(2);

        switch (cleanOrder) {
            case "priority":
                selection.append(" AND ").append(tablePrefix).append("priority IS NOT NULL");
                break;
            case "time_spent":
                selection.append(" AND ").append(tablePrefix).append("time IS NOT NULL");
                break;
            case "playthroughs":
                selection.append(" AND ").append(tablePrefix).append("plays IS NOT NULL");
                break;
            case "comp_type":
                selection.append(" AND ").append(tablePrefix).append("type IS NOT NULL AND ").append(tablePrefix).append("type != ''");
                break;
            case "date_added":
                selection.append(" AND ").append(tablePrefix).append("dateAddedPlanned IS NOT NULL AND ").append(tablePrefix).append("dateAddedPlanned != ''");
                break;
            case "date_started":
                selection.append(" AND ").append(tablePrefix).append("dateStartedPlaying IS NOT NULL AND ").append(tablePrefix).append("dateStartedPlaying != ''");
                break;
            case "date_completed":
                selection.append(" AND ").append(tablePrefix).append("dateEndCompleted IS NOT NULL AND ").append(tablePrefix).append("dateEndCompleted != ''");
                break;
        }
    }
    public List<Game> getGamesByCategoryObject(String category) {
        List<Game> games = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("games", null, "category=?", new String[]{category}, null, null, "created_at DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                games.add(cursorToGame(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return games;
    }
    private String mapOrderBy(String orderBy, String tablePrefix) {
        if (orderBy == null || orderBy.isEmpty() || orderBy.equals("id DESC")) {
            return tablePrefix + "created_at DESC";
        }

        String cleanOrder = orderBy.trim();
        String direction = " DESC";
        String directionPri = " ASC";

        if (cleanOrder.endsWith(" ASC")) {
            direction = " ASC";
            directionPri = " DESC";
            cleanOrder = cleanOrder.replace(" ASC", "").trim();
        } else if (cleanOrder.endsWith(" DESC")) {
            cleanOrder = cleanOrder.replace(" DESC", "").trim();
        }

        if (cleanOrder.startsWith("g.")) {
            cleanOrder = cleanOrder.substring(2);
        }

        switch (cleanOrder) {
            case "priority":
                return tablePrefix + "priority" + directionPri;
            case "time_spent":
                return tablePrefix + "time" + direction;
            case "playthroughs":
                return tablePrefix + "plays" + direction;
            case "comp_type":
                return tablePrefix + "type" + direction;

            case "date_added":
                String colAdd = tablePrefix + "dateAddedPlanned";
                return "SUBSTR(" + colAdd + ", 7, 4) || '-' || SUBSTR(" + colAdd + ", 4, 2) || '-' || SUBSTR(" + colAdd + ", 1, 2)" + direction;

            case "date_started":
                String colStart = tablePrefix + "dateStartedPlaying";
                return "SUBSTR(" + colStart + ", 7, 4) || '-' || SUBSTR(" + colStart + ", 4, 2) || '-' || SUBSTR(" + colStart + ", 1, 2)" + direction;

            case "date_completed":
                String colEnd = tablePrefix + "dateEndCompleted";
                return "SUBSTR(" + colEnd + ", 7, 4) || '-' || SUBSTR(" + colEnd + ", 4, 2) || '-' || SUBSTR(" + colEnd + ", 1, 2)" + direction;

            default:
                return tablePrefix + cleanOrder + direction;
        }
    }
    public List<Game> getGamesByCategorySorted(String category, String orderBy, String genreFilter, String tagFilter, String platformFilter, String languageFilter) {
        List<Game> games = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String finalOrderBy = mapOrderBy(orderBy, "");

        StringBuilder selection = new StringBuilder("category = ?");

        // --- МАГІЯ: Динамічно ховаємо ігри без потрібних даних ---
        appendFilterForSort(selection, orderBy, "");

        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(category);

        // Фільтри
        if (genreFilter != null && !genreFilter.isEmpty()) {
            for (String genre : genreFilter.split("\\|")) {
                selection.append(" AND genres LIKE ?");
                selectionArgs.add("%" + genre + "%");
            }
        }
        if (tagFilter != null && !tagFilter.isEmpty()) {
            for (String tag : tagFilter.split("\\|")) {
                selection.append(" AND tags LIKE ?");
                selectionArgs.add("%" + tag + "%");
            }
        }
        if (platformFilter != null && !platformFilter.isEmpty()) {
            for (String plat : platformFilter.split("\\|")) {
                selection.append(" AND platforms LIKE ?");
                selectionArgs.add("%" + plat + "%");
            }
        }
        if (languageFilter != null && !languageFilter.isEmpty()) {
            for (String lan : languageFilter.split("\\|")) {
                selection.append(" AND languages LIKE ?");
                selectionArgs.add("%" + lan + "%");
            }
        }

        Cursor cursor = db.query("games", null, selection.toString(),
                selectionArgs.toArray(new String[0]), null, null, finalOrderBy);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                games.add(cursorToGame(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return games;
    }

    public List<Game> getGamesByCollectionSorted(int collectionId, String orderBy, String genreFilter, String tagFilter, String platformFilter, String languageFilter) {
        List<Game> games = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String finalOrderBy = mapOrderBy(orderBy, "g.");

        StringBuilder query = new StringBuilder(
                "SELECT g.* FROM games g " +
                        "JOIN collection_games cg ON g.id = cg.game_id " +
                        "WHERE cg.collection_id = ?"
        );

        // --- МАГІЯ: Динамічно ховаємо ігри без потрібних даних для колекцій ---
        StringBuilder sortFilter = new StringBuilder();
        appendFilterForSort(sortFilter, orderBy, "g.");
        query.append(sortFilter.toString());

        List<String> args = new ArrayList<>();
        args.add(String.valueOf(collectionId));

        // Додавання фільтрів
        if (genreFilter != null && !genreFilter.isEmpty()) {
            for (String genre : genreFilter.split("\\|")) {
                query.append(" AND g.genres LIKE ?");
                args.add("%" + genre + "%");
            }
        }
        if (tagFilter != null && !tagFilter.isEmpty()) {
            for (String tag : tagFilter.split("\\|")) {
                query.append(" AND g.tags LIKE ?");
                args.add("%" + tag + "%");
            }
        }
        if (platformFilter != null && !platformFilter.isEmpty()) {
            for (String plat : platformFilter.split("\\|")) {
                query.append(" AND g.platforms LIKE ?");
                args.add("%" + plat + "%");
            }
        }
        if (languageFilter != null && !languageFilter.isEmpty()) {
            for (String lan : languageFilter.split("\\|")) {
                query.append(" AND g.languages LIKE ?");
                args.add("%" + lan + "%");
            }
        }

        query.append(" ORDER BY ").append(finalOrderBy);

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));

        if (cursor != null && cursor.moveToFirst()) {
            do {
                games.add(cursorToGame(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return games;
    }
    // Універсальний метод для отримання унікальних значень з конкретної колонки ігор певної колекції
    private List<String> getUniqueItemsInCollection(int collectionId, String columnName) {
        java.util.Set<String> uniqueItems = new java.util.HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // JOIN таблиць, щоб дістати дані ігор лише з поточної колекції
        String query = "SELECT g." + columnName + " FROM games g " +
                "JOIN collection_games cg ON g.id = cg.game_id " +
                "WHERE cg.collection_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(collectionId)});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String data = cursor.getString(0);
                if (data != null && !data.isEmpty()) {
                    String[] parts = data.split(",");
                    for (String part : parts) {
                        uniqueItems.add(part.trim());
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        List<String> sortedList = new ArrayList<>(uniqueItems);
        java.util.Collections.sort(sortedList);
        return sortedList;
    }

    // Публічні обгортки
    public List<String> getGenresInCollection(int colId) { return getUniqueItemsInCollection(colId, "genres"); }
    public List<String> getTagsInCollection(int colId) { return getUniqueItemsInCollection(colId, "tags"); }
    public List<String> getPlatformsInCollection(int colId) { return getUniqueItemsInCollection(colId, "platforms"); }
    public List<String> getLanguagesInCollection(int colId) { return getUniqueItemsInCollection(colId, "languages"); }

    // 4. Метод для отримання списку всіх унікальних ПЛАТФОРМ (для вибору в Spinner чи діалозі)
    public List<String> getAllUniquePlatforms() {
        List<String> platformList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT platforms FROM games", null);

        if (cursor.moveToFirst()) {
            java.util.Set<String> uniquePlatforms = new java.util.HashSet<>();
            do {
                String plats = cursor.getString(0);
                if (plats != null && !plats.isEmpty()) {
                    uniquePlatforms.addAll(Arrays.asList(plats.split(",")));
                }
            } while (cursor.moveToNext());
            platformList.addAll(uniquePlatforms);
            java.util.Collections.sort(platformList);
        }
        cursor.close();
        return platformList;
    }
    public List<String> getAllUniqueLanguages() {
        List<String> languageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT languages FROM games", null);

        if (cursor.moveToFirst()) {
            java.util.Set<String> uniqueLanguages = new java.util.HashSet<>();
            do {
                String plats = cursor.getString(0);
                if (plats != null && !plats.isEmpty()) {
                    uniqueLanguages.addAll(Arrays.asList(plats.split(",")));
                }
            } while (cursor.moveToNext());
            languageList.addAll(uniqueLanguages);
            java.util.Collections.sort(languageList);
        }
        cursor.close();
        return languageList;
    }

    // 1. Отримати список усіх унікальних ЖАНРІВ
    public List<String> getAllUniqueGenres() {
        java.util.Set<String> uniqueGenres = new java.util.HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Беремо всі рядки з колонки genres
        Cursor cursor = db.rawQuery("SELECT genres FROM games", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String genresLine = cursor.getString(0);
                if (genresLine != null && !genresLine.isEmpty()) {
                    // Розбиваємо рядок "RPG,Action" на окремі елементи
                    String[] parts = genresLine.split(",");
                    for (String part : parts) {
                        uniqueGenres.add(part.trim()); // trim() прибирає випадкові пробіли
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Перетворюємо Set у список та сортуємо за алфавітом
        List<String> sortedGenres = new ArrayList<>(uniqueGenres);
        java.util.Collections.sort(sortedGenres);
        return sortedGenres;
    }

    // 2. Отримати список усіх унікальних ТЕГІВ (Themes + Keywords)
    public List<String> getAllUniqueTags() {
        java.util.Set<String> uniqueTags = new java.util.HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT tags FROM games", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String tagsLine = cursor.getString(0);
                if (tagsLine != null && !tagsLine.isEmpty()) {
                    String[] parts = tagsLine.split(",");
                    for (String part : parts) {
                        uniqueTags.add(part.trim());
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        List<String> sortedTags = new ArrayList<>(uniqueTags);
        java.util.Collections.sort(sortedTags);
        return sortedTags;
    }

    // Оновлений метод підрахунку ігор з урахуванням прихованих при сортуванні ігор
    public int getGameCount(String category, String orderBy, String genreFilter, String tagFilter, String platformFilter, String languageFilter) {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM games WHERE category = ?");
        List<String> args = new ArrayList<>();
        args.add(category);

        // --- МАГІЯ: Застосовуємо той самий фільтр приховування порожніх ігор, що й для списку ---
        appendFilterForSort(query, orderBy, "");

        if (genreFilter != null && !genreFilter.isEmpty()) {
            for (String g : genreFilter.split("\\|")) {
                query.append(" AND genres LIKE ?");
                args.add("%" + g + "%");
            }
        }

        if (tagFilter != null && !tagFilter.isEmpty()) {
            for (String t : tagFilter.split("\\|")) {
                query.append(" AND tags LIKE ?");
                args.add("%" + t + "%");
            }
        }

        if (platformFilter != null && !platformFilter.isEmpty()) {
            for (String plat : platformFilter.split("\\|")) {
                query.append(" AND platforms LIKE ?");
                args.add("%" + plat + "%");
            }
        }

        if (languageFilter != null && !languageFilter.isEmpty()) {
            for (String lan : languageFilter.split("\\|")) {
                query.append(" AND languages LIKE ?");
                args.add("%" + lan + "%");
            }
        }

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public boolean isGameExists(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM games WHERE name=?", new String[]{name});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
    public String getCategoryByGameName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String category = null;
        Cursor cursor = null;

        try {
            cursor = db.query(
                    "games",                        // Назва таблиці
                    new String[]{"category"},       // Потрібна колонка
                    "name=?",                       // Умова
                    new String[]{name},             // Значення для умови
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return category; // повертає null, якщо гра не знайдена
    }

    // Решта методів (deleteGame, isGameExists, getCategoryByGameName) залишаються без змін
    public boolean deleteGame(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1. СПОЧАТКУ видаляємо всі записи про цю гру з проміжної таблиці колекцій.
        // Це очистить лічильник колекцій від видаленої гри.
        db.delete("collection_games", "game_id=?", new String[]{String.valueOf(id)});

        // 2. Тепер видаляємо саму гру з таблиці ігор.
        int rows = db.delete("games", "id=?", new String[]{String.valueOf(id)});

        db.close();
        return rows > 0;
    }

    // 1. Отримати всі колекції з підрахунком ігор
    public List<GameCollection> getAllCollections() {
        List<GameCollection> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT c.id, c.name, c.image_path, c.color,COUNT(cg.game_id) as gcount, c.show_name  " +
                "FROM collections c " +
                "LEFT JOIN collection_games cg ON c.id = cg.collection_id " +
                "GROUP BY c.id ORDER BY c.position_index ASC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(new GameCollection(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getInt(4), 
                        cursor.getInt(5)
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    // 2. Додати нову колекцію
    public long addCollection(String name, String imagePath, int color, int showName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("image_path", imagePath);
        values.put("color", color);
        values.put("show_name", showName); // Додаємо значення видимості назви
        return db.insert("collections", null, values);
    }

    // 3. Додати гру в колекцію
    public void addGameToCollection(int collectionId, int gameId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("collection_id", collectionId);
        values.put("game_id", gameId);
        db.insertWithOnConflict("collection_games", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // 4. Отримати ігри певної колекції
    public List<Game> getGamesInCollection(int collectionId) {
        List<Game> games = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT g.* FROM games g " +
                "JOIN collection_games cg ON g.id = cg.game_id " +
                "WHERE cg.collection_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(collectionId)});
        if (cursor.moveToFirst()) {
            do {
                games.add(cursorToGame(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return games;
    }

    // 5. Видалити колекцію
    public void deleteCollection(int collectionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("collections", "id=?", new String[]{String.valueOf(collectionId)});
        db.delete("collection_games", "collection_id=?", new String[]{String.valueOf(collectionId)});
        // db.close(); <-- ВИДАЛИ ЦЕЙ РЯДОК ВСЮДИ В ХЕЛПЕРІ
    }

    public void updateCollectionOrder(int id, int newIndex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("position_index", newIndex);
        db.update("collections", values, "id=?", new String[]{String.valueOf(id)});
    }

    // Оновити дані колекції (редагування)
    public void updateCollection(int id, String name, String imagePath, int color, int showName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("image_path", imagePath);
        values.put("color", color);
        values.put("show_name", showName);
        db.update("collections", values, "id = ?", new String[]{String.valueOf(id)});
    }

    // Видалити всі ігри з конкретної колекції
    public void removeAllGamesFromCollection(int collectionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Видаляємо всі рядки, де collection_id збігається
            db.delete("collection_games", "collection_id = ?", new String[]{String.valueOf(collectionId)});
            Log.d("DB_HELP", "All games removed from collection ID: " + collectionId);
        } catch (Exception e) {
            Log.e("DB_HELP", "Error while removing games from collection: " + e.getMessage());
        } finally {
            db.close(); // Обов'язково закриваємо базу
        }
    }
    public void removeGameFromCollection(int collectionId, int gameId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("collection_games",
                "collection_id = ? AND game_id = ?",
                new String[]{String.valueOf(collectionId), String.valueOf(gameId)});
        db.close();
    }
    public List<Integer> getCollectionIdsForGame(int gameId) {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT collection_id FROM collection_games WHERE game_id = ?",
                new String[]{String.valueOf(gameId)});
        if (cursor.moveToFirst()) {
            do { ids.add(cursor.getInt(0)); } while (cursor.moveToNext());
        }
        cursor.close();
        return ids;
    }
    public class WidgetStats {
        public int playing, planned, completed, totalTime;
    }

    public WidgetStats getStatsForWidget() {
        WidgetStats stats = new WidgetStats();
        SQLiteDatabase db = this.getReadableDatabase();

        Log.d("WIDGET_DEBUG", "--- Початок збору статистики для віджета ---");

        Cursor c = db.rawQuery("SELECT name, category, hltb FROM games", null);
        Log.d("WIDGET_DEBUG", "Знайдено ігор у базі: " + c.getCount());

        if (c.moveToFirst()) {
            do {
                String name = c.getString(0);
                String cat = c.getString(1);
                String hltb = c.getString(2);

                if ("playing".equalsIgnoreCase(cat)) stats.playing++;
                else if ("planned".equalsIgnoreCase(cat)) stats.planned++;
                else if ("completed".equalsIgnoreCase(cat)) {
                    stats.completed++;

                    // Логуємо парсинг часу для кожної пройденої гри
                    if (hltb != null && hltb.contains("|")) {
                        try {
                            String[] parts = hltb.split("\\|");
                            if (parts.length > 1) {
                                String extraTimeStr = parts[1].replaceAll("[^0-9]", "").trim();
                                if (!extraTimeStr.isEmpty()) {
                                    Float time = Float.parseFloat(extraTimeStr);
                                    stats.totalTime += time;
                                    Log.v("WIDGET_DEBUG", "Гра: " + name + " | Час (Extras): " + time + "h");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("WIDGET_DEBUG", "Помилка парсингу HLTB для " + name + ": " + hltb);
                        }
                    }
                }
            } while (c.moveToNext());
        }
        c.close();

        Log.i("WIDGET_DEBUG", "Фінальна статистика: Playing=" + stats.playing +
                ", Planned=" + stats.planned + ", Played=" + stats.completed +
                ", TotalTime=" + stats.totalTime);
        return stats;
    }

    // Метод витягує блок статистики з опису
    private String extractBlock(String text, String startMarker, String endMarker) {
        if (text == null) return "";
        if (text.contains(startMarker)) {
            int start = text.indexOf(startMarker);
            int end = text.indexOf(endMarker, start) + endMarker.length();
            if (start >= 0 && end > start) {
                return text.substring(start, end);
            }
        }
        return "";
    }

    // Метод видаляє блок статистики з опису
    private String removeBlock(String text, String startMarker, String endMarker) {
        if (text == null) return "";
        if (text.contains(startMarker)) {
            int start = text.indexOf(startMarker);
            int end = text.indexOf(endMarker, start) + endMarker.length();
            if (start >= 0 && end > start) {
                return text.substring(0, start) + text.substring(end);
            }
        }
        return text;
    }

    // ГОЛОВНИЙ МЕТОД МІГРАЦІЇ ДАНИХ (Парсить старі описи та переносить дані у нові колонки)
    public void migrateLegacyStatsToColumns(SQLiteDatabase db) {
        Log.d("DB_MIGRATION", "Початок міграції старих текстових даних у нові колонки...");
        Cursor cursor = null;
        try {
            cursor = db.query("games", null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                    if (description == null) description = "";

                    // Тимчасові змінні для збору даних
                    String dateStartCompleted = null;
                    String dateEndCompleted = null;
                    String dateAddedPlanned = null;
                    String dateStartPlaying = null;
                    String review = null;
                    Integer priority = null;
                    String type = null;
                    Integer plays = null;
                    Float time = null;

                    boolean needsUpdate = false;
                    String cleanDesc = description;
                    String statsMarkerEnd = "-----------------------------";

                    // 1. Парсимо Planned Stats (якщо є)
                    String plannedMarker = "🎯 --- PLANNED STATS --- 🎯";
                    if (description.contains(plannedMarker)) {
                        needsUpdate = true;
                        String block = extractBlock(description, plannedMarker, statsMarkerEnd);
                        cleanDesc = removeBlock(cleanDesc, plannedMarker, statsMarkerEnd);

                        for (String line : block.split("\n")) {
                            if (line.startsWith("Priority:")) {
                                try { priority = Integer.parseInt(line.replace("Priority:", "").trim()); } catch (Exception ignored) {}
                            } else if (line.startsWith("Date added on:")) {
                                dateAddedPlanned = line.replace("Date added on:", "").trim();
                            } else if (line.startsWith("Added to Planned:")) {
                                dateAddedPlanned = line.replace("Added to Planned:", "").trim();
                            }
                        }
                    }

                    // 2. Парсимо Playing Stats (якщо є)
                    String playingMarker = "🎮 --- PLAYING STATS --- 🎮";
                    if (description.contains(playingMarker)) {
                        needsUpdate = true;
                        String block = extractBlock(description, playingMarker, statsMarkerEnd);
                        cleanDesc = removeBlock(cleanDesc, playingMarker, statsMarkerEnd);

                        for (String line : block.split("\n")) {
                            if (line.startsWith("Started playing:")) {
                                dateStartPlaying = line.replace("Started playing:", "").trim();
                            }
                        }
                    }

                    // 3. Парсимо Completion Stats (якщо є)
                    String completionMarker = "🏆 --- COMPLETION STATS --- 🏆";
                    if (description.contains(completionMarker)) {
                        needsUpdate = true;
                        String block = extractBlock(description, completionMarker, statsMarkerEnd);
                        cleanDesc = removeBlock(cleanDesc, completionMarker, statsMarkerEnd);

                        boolean isReviewLine = false;
                        StringBuilder reviewBuilder = new StringBuilder();

                        for (String line : block.split("\n")) {
                            if (line.trim().startsWith(statsMarkerEnd)) break;

                            if (line.startsWith("Time Spent:")) {
                                try { time = Float.parseFloat(line.replace("Time Spent:", "").replace("hours", "").trim()); } catch (Exception ignored) {}
                            } else if (line.startsWith("Playthroughs:")) {
                                try { plays = Integer.parseInt(line.replace("Playthroughs:", "").trim()); } catch (Exception ignored) {}
                            } else if (line.startsWith("Start Date:")) {
                                dateStartCompleted = line.replace("Start Date:", "").trim();
                            } else if (line.startsWith("End Date:")) {
                                dateEndCompleted = line.replace("End Date:", "").trim();
                            } else if (line.startsWith("Date:")) {
                                dateEndCompleted = line.replace("Date:", "").trim();
                            } else if (line.startsWith("Type:")) {
                                type = line.replace("Type:", "").trim();
                            } else if (line.startsWith("Review:")) {
                                isReviewLine = true;
                                reviewBuilder.append(line.replace("Review:", "").trim());
                            } else if (isReviewLine) {
                                reviewBuilder.append("\n").append(line);
                            }
                        }
                        if (reviewBuilder.length() > 0) {
                            review = reviewBuilder.toString().trim();
                        }
                    }

                    // 4. Фолбек для старого формату "Added on: dd.MM.yyyy"
                    if (cleanDesc.contains("Added on: ")) {
                        needsUpdate = true;
                        int idx = cleanDesc.indexOf("Added on: ");
                        try {
                            String legacyDate = cleanDesc.substring(idx + 10, idx + 20).trim();
                            if (dateAddedPlanned == null || dateAddedPlanned.isEmpty()) {
                                dateAddedPlanned = legacyDate;
                            }
                        } catch (Exception ignored) {}
                        cleanDesc = cleanDesc.replaceAll("Added on: \\d{2}\\.\\d{2}\\.\\d{4}", "").trim();
                    }

                    // 5. Оновлюємо рядок в базі даних, якщо знайшли якісь старі дані
                    if (needsUpdate) {
                        ContentValues values = new ContentValues();
                        values.put("description", cleanDesc.trim());

                        if (dateStartCompleted != null) values.put("dateStartCompleted", dateStartCompleted);
                        if (dateEndCompleted != null) values.put("dateEndCompleted", dateEndCompleted);
                        if (dateAddedPlanned != null) values.put("dateAddedPlanned", dateAddedPlanned);
                        if (dateStartPlaying != null) values.put("dateStartedPlaying", dateStartPlaying);
                        if (review != null) values.put("review", review);
                        if (priority != null) values.put("priority", priority);
                        if (type != null) values.put("type", type);
                        if (plays != null) values.put("plays", plays);
                        if (time != null) values.put("time", time);

                        db.update("games", values, "id = ?", new String[]{String.valueOf(id)});
                        Log.d("DB_MIGRATION", "Гру з ID " + id + " успішно мігровано!");
                    }

                } while (cursor.moveToNext());
            }
            Log.d("DB_MIGRATION", "Міграцію бази даних успішно завершено!");
        } catch (Exception e) {
            Log.e("DB_MIGRATION", "Помилка під час міграції даних: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    private boolean isValidColumn(String col) {
        if (col == null) return false;
        switch (col.toLowerCase()) {
            case "id": case "name": case "released": case "rating":
            case "priority": case "date_added": case "date_started":
            case "time_spent": case "date_completed": case "playthroughs":
            case "comp_type": case "price": case "discount":
                return true;
            default:
                return false;
        }
    }

}