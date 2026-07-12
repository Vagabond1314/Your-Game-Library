package com.your_game_library;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private GameDatabaseHelper dbHelper;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = GameDatabaseHelper.getInstance(context);
    }

    @Test
    public void testInsertAndRetrieveGame() {
        // 1. Створюємо тестову гру
        Game testGame = new Game(9999, "Test Game", "playing", "Summary", 8.5f,
                null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                "2024", "10h", "", new ArrayList<>(), new ArrayList<>(),
                "", new ArrayList<>(), 0f, "", "", "", "", "", "", new ArrayList<>(),"");

        // 2. Додаємо в базу
        dbHelper.addGame(testGame);

        // 3. Пробуємо дістати її за ID
        Game retrievedGame = dbHelper.getGameByIdObject(9999);

        // 4. Перевіряємо, чи назва збігається
        assertNotNull("Гра має бути знайдена в БД", retrievedGame);
        assertEquals("Назва гри в БД має бути Test Game", "Test Game", retrievedGame.getName());

        // Очищуємо після тесту
        dbHelper.deleteGame(9999);
    }
}