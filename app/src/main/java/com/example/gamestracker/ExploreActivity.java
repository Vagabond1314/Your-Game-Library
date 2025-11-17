package com.example.gamestracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Spinner spinnerFilter;
    private RawgClient client;
    private RawgAdapter adapter;
    private GameDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        recyclerView = findViewById(R.id.recyclerRawg);
        progressBar = findViewById(R.id.progressRawg);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        client = new RawgClient();
        dbHelper = new GameDatabaseHelper(this);

        String[] options = new String[] { "Popular", "Top Rated", "Upcoming", "New Releases" };
        spinnerFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                fetchForPosition(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // initial
        fetchForPosition(0);
    }

    private void fetchForPosition(int pos) {
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(android.view.View.VISIBLE);

        String params;
        switch (pos) {
            case 0: // Popular
                params = "ordering=-added&page_size=30";
                break;
            case 1: // Top rated
                params = "ordering=-rating&page_size=30";
                break;
            case 2: // Upcoming
                // dates from tomorrow to next year
                params = "dates=2025-11-17,2026-11-17&ordering=-added&page_size=30";
                break;
            case 3: // New releases
                params = "ordering=-released&page_size=30";
                break;
            default:
                params = "page_size=30";
        }

        client.fetchGames(params, new RawgClient.CallbackGames() {
            @Override
            public void onSuccess(List<RawgGame> games) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    adapter = new RawgAdapter(ExploreActivity.this, games, rawgGame -> addRawgGameToMyList(rawgGame));
                    recyclerView.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(ExploreActivity.this, "RAWG error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Завантажити картинку з URL у файл і додати гру в локальну БД
    private void addRawgGameToMyList(RawgGame rawgGame) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        new Thread(() -> {
            String savedPath = null;
            try {
                String imageUrl = rawgGame.getBackgroundImage();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    savedPath = downloadImageToInternal(imageUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Game g = new Game(-1,
                    rawgGame.getName(),
                    "planned",
                    "Imported from RAWG",
                    rawgGame.getRating() == 0.0f ? null : rawgGame.getRating(),
                    savedPath);

            boolean ok = dbHelper.addGame(g) != -1;

            final boolean success = ok;
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(ExploreActivity.this, success ? "Added to My List" : "Add failed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    // Просте скачування картинки і збереження у внутрішню папку files/
    private String downloadImageToInternal(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            conn.setInstanceFollowRedirects(true);
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();

            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return null;

            String fileName = "rawg_" + System.currentTimeMillis() + ".png";
            File file = new File(getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            is.close();
            conn.disconnect();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
