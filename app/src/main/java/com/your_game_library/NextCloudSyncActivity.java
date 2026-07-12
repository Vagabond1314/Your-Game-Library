package com.your_game_library;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

// ВАЖЛИВО: Використовуємо лише ці імпорти для OkHttp
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NextCloudSyncActivity extends AppCompatActivity {

    private EditText etServer, etUser, etPassword;
    private SharedPreferences prefs;
    // ВИПРАВЛЕННЯ ПОМИЛКИ 1: Оголошуємо змінну
    private AlertDialog progressDialog;
    private RadioGroup radioGroupFrequency;
    private LinearLayout layoutCustomTime;
    private EditText etCustomHours;
    private Spinner spinnerFrequency;
    private final String[] freqOptions = {"Every Day", "Every Week", "Every Month", "Custom (hours)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_cloud_sync);

        Toolbar toolbar = findViewById(R.id.toolbarNextCloud);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etServer = findViewById(R.id.etNextCloudServer);
        etUser = findViewById(R.id.etNextCloudUser);
        etPassword = findViewById(R.id.etNextCloudPassword);
        Button btnSave = findViewById(R.id.btnSaveNextCloud);
        Button btnSyncNow = findViewById(R.id.btnSyncNow);
        Button btnDownload = findViewById(R.id.btnDownloadFromCloud);
        radioGroupFrequency = findViewById(R.id.radioGroupFrequency);

        prefs = getSharedPreferences("nextcloud_prefs", MODE_PRIVATE);

        etServer.setText(prefs.getString("server", ""));
        etUser.setText(prefs.getString("user", ""));
        etPassword.setText(prefs.getString("pass", ""));

        btnSave.setOnClickListener(v -> {
            saveCredentials();
            scheduleSync();
            Toast.makeText(this, "Settings saved & Sync scheduled", Toast.LENGTH_SHORT).show();
        });

        btnSyncNow.setOnClickListener(v -> {
            saveCredentials();
            androidx.work.OneTimeWorkRequest manualSync = new androidx.work.OneTimeWorkRequest.Builder(NextCloudWorker.class).build();
            WorkManager.getInstance(this).enqueue(manualSync);
            Toast.makeText(this, "Upload started in background...", Toast.LENGTH_SHORT).show();
        });

        btnDownload.setOnClickListener(v -> {
            new AlertDialog.Builder(this, R.style.MyDialogTheme)
                    .setTitle("Restore from Cloud")
                    .setMessage("Replace local database with cloud backup? Current data will be lost.")
                    .setPositiveButton("Download & Restart", (dialog, which) -> performCloudDownload())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        int savedCheckedId = prefs.getInt("checked_rb_id", R.id.rbDaily);
        radioGroupFrequency.check(savedCheckedId);

// Слухач перемикання
        radioGroupFrequency.setOnCheckedChangeListener((group, checkedId) -> {
        });
    }

    private void saveCredentials() {
        int checkedId = radioGroupFrequency.getCheckedRadioButtonId();
        int hours = 24;

        if (checkedId == R.id.rbDaily) {
            hours = 24;
        } else if (checkedId == R.id.rbWeekly) {
            hours = 168; // 24 * 7
        } else if (checkedId == R.id.rbMonthly) {
            hours = 720; // 24 * 30
        }

        prefs.edit()
                .putString("server", etServer.getText().toString().trim())
                .putString("user", etUser.getText().toString().trim())
                .putString("pass", etPassword.getText().toString().trim())
                .putInt("checked_rb_id", checkedId)      // зберігаємо ID кнопки
                .putInt("sync_hours", hours)            // години для WorkManager
                .apply();
    }

    // Метод scheduleSync залишається майже таким самим
    private void scheduleSync() {
        int hours = prefs.getInt("sync_hours", 24);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                NextCloudWorker.class, hours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "NextCloudSync",
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                syncRequest
        );
    }

    private void performCloudDownload() {
        String server = etServer.getText().toString().trim();
        String user = etUser.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setMessage("Downloading database...")
                .setCancelable(false)
                .show();

        new Thread(() -> {
            boolean success = downloadFileFromNextCloud(server, user, pass);
            runOnUiThread(() -> {
                if (progressDialog != null) progressDialog.dismiss();
                if (success) {
                    Toast.makeText(this, "Restore successful!", Toast.LENGTH_SHORT).show();
                    restartApp();
                } else {
                    Toast.makeText(this, "Download failed!", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private boolean downloadFileFromNextCloud(String server, String user, String pass) {
        try {
            if (!server.endsWith("/")) server += "/";
            String url = server + "remote.php/dav/files/" + user + "/GamesTrackerBackup" + "/games_backup.db";

            OkHttpClient client = new OkHttpClient();
            String credentials = user + ":" + pass;
            String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", auth)
                    .build();

            // ВИПРАВЛЕННЯ ПОМИЛКИ 2: Це має бути okhttp3.Response
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    // Закриваємо БД
                    GameDatabaseHelper.getInstance(this).close();

                    File dbFile = getDatabasePath("games.db");
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream os = new FileOutputStream(dbFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("NextCloud", "Download error", e);
        }
        return false;
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
        finish();
    }
}