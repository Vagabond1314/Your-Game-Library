package com.your_game_library;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NextCloudWorker extends Worker {

    public NextCloudWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("nextcloud_prefs", Context.MODE_PRIVATE);
        String server = prefs.getString("server", ""); // Напр: https://cloud.example.com
        String user = prefs.getString("user", "");
        String pass = prefs.getString("pass", "");

        if (server.isEmpty() || user.isEmpty() || pass.isEmpty()) return Result.failure();

        try {
            File dbFile = getApplicationContext().getDatabasePath("games.db");
            if (!dbFile.exists()) return Result.failure();

            // Формуємо URL для WebDAV NextCloud
            // Стандартний шлях: /remote.php/dav/files/USER/path/to/file
            if (!server.endsWith("/")) server += "/";
            String url = server + "remote.php/dav/files/" + user + "/GamesTrackerBackup" + "/games_backup.db";

            OkHttpClient client = new OkHttpClient();

            // Авторизація (Basic Auth)
            String credentials = user + ":" + pass;
            String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            RequestBody body = RequestBody.create(dbFile, MediaType.parse("application/octet-stream"));

            Request request = new Request.Builder()
                    .url(url)
                    .put(body) // PUT перезаписує існуючий файл
                    .addHeader("Authorization", auth)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d("NextCloudSync", "Success!");
                    return Result.success();
                } else {
                    Log.e("NextCloudSync", "Failed: " + response.code());
                    return Result.retry();
                }
            }
        } catch (Exception e) {
            Log.e("NextCloudSync", "Error", e);
            return Result.retry();
        }
    }
}