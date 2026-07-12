package com.your_game_library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_MAIN_GRID = "is_main_grid";
    private static final String DB_NAME = "games.db";
    private IgdbApiService igdbApi;
    private GameDatabaseHelper dbHelper;
    private boolean isSyncCancelled = false; // Прапор для скасування
    private androidx.appcompat.app.AlertDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Налаштування Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        findViewById(R.id.btnBackup).setOnClickListener(v -> exportDatabase());
        findViewById(R.id.btnRestore).setOnClickListener(v -> importDatabase());
        findViewById(R.id.btnNextCloudSync).setOnClickListener(v -> {
            startActivity(new Intent(this, NextCloudSyncActivity.class));
        });
        RadioGroup radioGroup = findViewById(R.id.radioGroupTable);
        RadioButton radioList = findViewById(R.id.radioList);
        RadioButton radioGrid = findViewById(R.id.radioGrid);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isGridStored = prefs.getBoolean(KEY_MAIN_GRID, false);
// Ініціалізація та клік по кнопці Steam Sync
        findViewById(R.id.btnSteamSync).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SteamSyncActivity.class);
            startActivity(intent);
        });
        // Встановлюємо вибраний пункт згідно з базою
        if (isGridStored) {
            radioGrid.setChecked(true);
        } else {
            radioList.setChecked(true);
        }
        dbHelper = GameDatabaseHelper.getInstance(this);
        initRetrofit();

        // Слухаємо зміни
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isGridSelected = (checkedId == R.id.radioGrid);

            // Зберігаємо вибір (true для сітки, false для списку)
            prefs.edit().putBoolean(KEY_MAIN_GRID, isGridSelected).apply();
        });
    }
    private void initRetrofit() {
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("https://api.igdb.com/")
                .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        igdbApi = retrofit.create(IgdbApiService.class);
    }
    private void updateAllGameFields(Game game, IgdbGame igdb) {
        if (igdb.name != null) game.setName(igdb.name);
        if (igdb.summary != null) game.setDescription(igdb.summary);
        if (igdb.storyline != null) game.setStoryline(igdb.storyline);
        if (igdb.slug != null) game.setRawgSlug(igdb.slug);
        game.setIgdbUrl(igdb.url);

        if (igdb.total_rating > 0) game.setRating((float) igdb.total_rating / 10f);
        if (igdb.aggregated_rating > 0) game.setAggregatedRating((float) igdb.aggregated_rating / 10f);

        // Жанри
        List<String> genres = new ArrayList<>();
        if (igdb.genres != null) for (IgdbGame.Genre g : igdb.genres) genres.add(g.name);
        game.setGenres(genres);

        // Теги
        List<String> tags = new ArrayList<>();
        if (igdb.themes != null) for (IgdbGame.Theme t : igdb.themes) tags.add(t.name);
        if (igdb.keywords != null) for (IgdbGame.Keyword k : igdb.keywords) if (!tags.contains(k.name)) tags.add(k.name);
        game.setTags(tags);

        // Платформи, Мови, Схожі ігри
        List<String> plats = new ArrayList<>();
        if (igdb.platforms != null) for (IgdbGame.Platform p : igdb.platforms) plats.add(p.name);
        game.setPlatforms(plats);

        List<String> langs = new ArrayList<>();
        if (igdb.language_supports != null) {
            for (IgdbGame.LanguageSupport ls : igdb.language_supports) if (ls.language != null) langs.add(ls.language.name);
        }
        game.setLanguages(langs);

        List<String> similar = new ArrayList<>();
        if (igdb.similar_games != null) {
            for (IgdbGame.SimilarGame sg : igdb.similar_games) similar.add(sg.name + "|" + sg.id);
        }
        game.setSimilarGames(similar);

        // Обкладинка та Скріншоти (якщо локальні файли відсутні)
        if (igdb.cover != null) {
            String remote = getHighResUrl(igdb.cover.url, "t_1080p");
            if (game.getImagePath() == null || game.getImagePath().startsWith("http") || !new File(game.getImagePath()).exists()) {
                game.setImagePath(remote);
            }
        }
        if (igdb.screenshots != null) {
            List<String> sc = new ArrayList<>();
            for (IgdbGame.Screenshot s : igdb.screenshots) sc.add(getHighResUrl(s.url, "t_720p"));
            game.setScreenshots(sc);
        }

        // Магазини
        if (igdb.websites != null) {
            for (IgdbGame.Website w : igdb.websites) {
                String u = w.url.toLowerCase();
                if (w.category == 13 || u.contains("steampowered.com")) game.setSteamUrl(w.url);
                else if (u.contains("playstation.com")) game.setPsUrl(w.url);
                else if (u.contains("xbox.com") || u.contains("microsoft.com")) {
                    if (u.contains("/p/") || u.contains("/games/") || u.contains("store")) game.setXboxUrl(w.url);
                } else if (u.contains("nintendo.com") || u.contains("nintendo.co")) game.setNintendoUrl(w.url);
            }
        }
    }

    // Не забудьте додати метод для посилань, якщо його ще немає в цьому класі
    private String getHighResUrl(String url, String sizeTag) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("//")) url = "https:" + url;
        return url.replaceAll("t_\\w+", sizeTag);
    }
    private void showProgressDialog(int total) {
        isSyncCancelled = false; // Скидаємо прапор перед початком

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle("Global Sync");
        builder.setMessage("Preparing to sync " + total + " games...");
        builder.setCancelable(false); // Користувач не може закрити вікно просто клікнувши поруч

        // Додаємо кнопку скасування
        builder.setNegativeButton("Stop Sync", (dialog, which) -> {
            isSyncCancelled = true; // Встановлюємо прапор скасування
        });

        progressDialog = builder.create();
        progressDialog.show();

        // Робимо текст кнопки чорним
        progressDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.RED);
    }
    private void exportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Важливі зміни:
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Більш універсальний тип, який File Manager не заблокує
        intent.putExtra(Intent.EXTRA_TITLE, "gamestracker_backup.db");

        // Додаємо дозволи на запис (для нових версій Android це іноді критично)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            backupLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening File Manager", Toast.LENGTH_SHORT).show();
            Log.e("BACKUP", "Failed to launch document creator: " + e.getMessage());
        }
    }
    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    copyDbToFile(result.getData().getData());
                }
            }
    );

    private void copyDbToFile(Uri targetUri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);

            // Якщо БД відкрита, бажано переконатися, що всі дані скинуто на диск
            GameDatabaseHelper.getInstance(this).getReadableDatabase();

            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = getContentResolver().openOutputStream(targetUri)) {

                if (out == null) throw new Exception("OutputStream is null");

                byte[] buf = new byte[8192]; // Збільшений буфер (8KB)
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush(); // Обов'язкове скидання на диск
            }

            Toast.makeText(this, "Backup created successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("BACKUP", "Backup error", e);
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // --- ІМПОРТ (RESTORE) ---
    private void importDatabase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Дозволяємо вибрати будь-який файл
        restoreLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    confirmRestore(result.getData().getData());
                }
            }
    );

    private void confirmRestore(Uri sourceUri) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Restore Database")
                .setMessage("This will overwrite your current collection. Are you sure?")
                .setPositiveButton("Restore & Restart", (d, which) -> {
                    startRestore(sourceUri);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // ПРИМУСОВЕ фарбування кнопок (для гарантії)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#fc6f03")); // Помаранчевий для небезпечної дії
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#FFFFFF")); // Білий для скасування
    }

    private void startRestore(Uri sourceUri) {
        // Показуємо ProgressDialog або просто логуємо
        Log.d("DB_RESTORE", "Moving to background thread...");

        // Створюємо фоновий потік
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            // ВИКОНУЄТЬСЯ У ФОНІ
            boolean success = doRestore(sourceUri);

            handler.post(() -> {
                // ПОВЕРТАЄМОСЬ У ГОЛОВНИЙ ПОТІК ДЛЯ UI
                if (success) {
                    Toast.makeText(this, "Restore successful! Restarting app...", Toast.LENGTH_LONG).show();
                    // Рекомендується перезавантажити додаток після заміни БД
                    restartApp();
                } else {
                    Toast.makeText(this, "Restore failed. Check logs.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private boolean doRestore(Uri sourceUri) {
        try {
            Log.d("DB_RESTORE", "Background copy started...");

            // Закриваємо БД
            GameDatabaseHelper.getInstance(this).close();

            File dbFile = getDatabasePath(DB_NAME);

            // Тепер openInputStream не видасть помилку, бо ми не в Main Thread
            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(dbFile)) {

                if (in == null) return false;

                byte[] buf = new byte[8192]; // Збільшив буфер для швидкості
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }

            Log.d("DB_RESTORE", "File copied successfully in background.");
            return true;
        } catch (Exception e) {
            Log.e("DB_RESTORE", "Error during background restore", e);
            return false;
        }
    }

    private void restartApp() {
        // Простий спосіб перезавантажити додаток, щоб підхопити нову БД
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}