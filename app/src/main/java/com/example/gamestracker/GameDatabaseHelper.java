package com.example.gamestracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GameDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "games.db";
    private static final int DATABASE_VERSION = 2; // оновлено до версії з image_path

    public GameDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE games (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE," +
                "category TEXT," +
                "description TEXT," +
                "rating REAL," +
                "image_path TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS games");
        onCreate(db);
    }

    // Додати гру
    public long addGame(Game game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", game.getName());
        values.put("category", game.getCategory());
        values.put("description", game.getDescription());
        values.put("rating", game.getRating());
        values.put("image_path", game.getImagePath());
        long id = db.insert("games", null, values);
        db.close();
        return id;
    }

    public Cursor getGameById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM games WHERE id=?", new String[]{String.valueOf(id)});
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
        int rows = db.update("games", values, "id=?", new String[]{String.valueOf(game.getId())});
        db.close();
        return rows > 0;
    }

    // Видалити гру
    public boolean deleteGame(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("games", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    // Перевірка на існування гри з таким ім'ям
    public boolean isGameExists(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM games WHERE name=?", new String[]{name});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Отримати всі ігри
    public Cursor getAllGames() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM games", null);
    }

    // Отримати ігри за категорією
    public Cursor getGamesByCategory(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM games WHERE category=?", new String[]{category});
    }
}
