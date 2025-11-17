package com.example.gamestracker;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    GameAdapter adapter;
    FloatingActionButton fabAdd;
    GameDatabaseHelper dbHelper;

    Button buttonPlanned, buttonPlaying, buttonCompleted;
    String currentCategory = "planned"; // за замовчуванням

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewGames);
        fabAdd = findViewById(R.id.fabAdd);
        buttonPlanned = findViewById(R.id.buttonPlanned);
        buttonPlaying = findViewById(R.id.buttonPlaying);
        buttonCompleted = findViewById(R.id.buttonCompleted);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new GameDatabaseHelper(this);

        // Кнопки перемикання категорій
        buttonPlanned.setOnClickListener(v -> {
            currentCategory = "planned";
            loadGames();
        });
        buttonPlaying.setOnClickListener(v -> {
            currentCategory = "playing";
            loadGames();
        });
        buttonCompleted.setOnClickListener(v -> {
            currentCategory = "completed";
            loadGames();
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddGameActivity.class);
            startActivity(intent);
        });

        loadGames(); // завантажити початкову категорію
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGames(); // оновлення після повернення з AddGameActivity
    }

    private void loadGames() {
        Cursor cursor = dbHelper.getGamesByCategory(currentCategory);
        List<Game> games = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                Float rating = cursor.isNull(cursor.getColumnIndexOrThrow("rating")) ? null :
                        cursor.getFloat(cursor.getColumnIndexOrThrow("rating"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));

                games.add(new Game(id, name, cat, desc, rating, imagePath));
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Передаємо dbHelper у конструктор
        adapter = new GameAdapter(this, games, dbHelper);
        recyclerView.setAdapter(adapter);
    }
}
