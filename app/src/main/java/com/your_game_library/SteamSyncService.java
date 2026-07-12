package com.your_game_library;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class SteamSyncService extends Service {

    private static final String CHANNEL_ID = "SteamSyncChannel";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private IgdbApiService igdbApi;
    private GameDatabaseHelper dbHelper;
    private String igdbToken = null;

    private final String IGDB_CLIENT_ID = Config.IGDB_CLIENT_ID;
    private final String IGDB_CLIENT_SECRET = Config.IGDB_CLIENT_SECRET;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        dbHelper = GameDatabaseHelper.getInstance(this);

        Retrofit igdbRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.igdb.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        igdbApi = igdbRetrofit.create(IgdbApiService.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra("GAMES_JSON")) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String gamesJson = intent.getStringExtra("GAMES_JSON");
        Type listType = new TypeToken<List<SteamResponse.SteamGame>>(){}.getType();
        List<SteamResponse.SteamGame> selectedGames = new Gson().fromJson(gamesJson, listType);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification("Connecting to IGDB...", 0, selectedGames.size()));

        // Починаємо фонову роботу
        new Thread(() -> {
            try {
                // 1. Отримуємо токен
                retrofit2.Call<TokenResponse> call = igdbApi.getToken(IGDB_CLIENT_ID, IGDB_CLIENT_SECRET, "client_credentials");
                retrofit2.Response<TokenResponse> response = call.execute(); // Синхронний виклик (бо ми вже у фоні)

                if (response.isSuccessful() && response.body() != null) {
                    igdbToken = "Bearer " + response.body().access_token;
                    // 2. Починаємо обробку ігор
                    processGames(selectedGames);
                } else {
                    updateNotification("Error: IGDB Auth Failed", 0, 0);
                    stopSelf();
                }
            } catch (Exception e) {
                updateNotification("Network Error", 0, 0);
                stopSelf();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void processGames(List<SteamResponse.SteamGame> games) {
        int currentProgress = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        List<Game> allDbGames = dbHelper.getAllGames();

        for (SteamResponse.SteamGame sg : games) {
            currentProgress++;
            updateNotification("Processing: " + sg.name, currentProgress, games.size());

            float hoursPlayed = sg.playtime_forever / 60.0f;
            String lastPlayedDate = null;
            if (sg.rtime_last_played > 0) {
                lastPlayedDate = sdf.format(new Date(sg.rtime_last_played * 1000L));
            }

            // Перевірка на дублікат (Локально)
            Game existingGame = null;
            for (Game dbG : allDbGames) {
                if (dbG.getName() != null && dbG.getName().equalsIgnoreCase(sg.name)) {
                    existingGame = dbG;
                    break;
                }
            }

            if (existingGame != null) {
                // ГРА ВЖЕ Є: Оновлюємо години і дати, категорію НЕ МІНЯЄМО
                if (hoursPlayed > 0) existingGame.setTime(hoursPlayed);

                // Якщо є дата, пишемо її в ту категорію, де зараз знаходиться гра
                if (lastPlayedDate != null) {
                    if (existingGame.getCategory().equalsIgnoreCase("completed")) {
                        existingGame.setDateEndCompleted(lastPlayedDate);
                    } else if (existingGame.getCategory().equalsIgnoreCase("playing")) {
                        existingGame.setDateStartPlaying(lastPlayedDate);
                    }
                }
                dbHelper.updateGame(existingGame);
            } else {
                // ГРИ НЕМАЄ: Йдемо на IGDB
                fetchAndSaveNewGame(sg.name, hoursPlayed, lastPlayedDate, today);
            }

            // Затримка для обходу лімітів IGDB (4 запити на секунду макс)
            try { Thread.sleep(300); } catch (InterruptedException e) {}
        }

        // Завершення: Міняємо текст і ховаємо прогрес
        notificationBuilder.setContentTitle("Steam Sync Complete!")
                .setContentText("Added/Updated " + games.size() + " games.")
                .setProgress(0, 0, false);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        // Зупиняємо сервіс
        stopForeground(false);
        stopSelf();
    }

    private void fetchAndSaveNewGame(String gameName, Float hoursPlayed, String lastPlayedDate, String today) {
        // Замінюємо спецсимволи на ПРОБІЛИ, а потім видаляємо зайві пробіли
        String cleanName = gameName.replaceAll("[^a-zA-Z0-9 ]", " ").trim().replaceAll(" +", " ");
        Log.d("STEAM_SYNC_DEBUG", "--------------------------------------------------");
        Log.d("STEAM_SYNC_DEBUG", "1. Спроба знайти нову гру на IGDB: [" + gameName + "]");

        String query = "fields name, summary, cover.url, genres.name, platforms.name, aggregated_rating; search \"" + cleanName + "\"; limit 5;";
        Log.d("STEAM_SYNC_DEBUG", "2. Запит до IGDB: " + query);

        try {
            Response<List<IgdbGame>> response = igdbApi.getGames(IGDB_CLIENT_ID, igdbToken, query).execute();

            if (response.isSuccessful() && response.body() != null) {
                Log.d("STEAM_SYNC_DEBUG", "3. Відповідь IGDB успішна. Знайдено результатів: " + response.body().size());

                IgdbGame igdb = findBestMatch(response.body(), gameName);

                if (igdb != null) {
                    Log.d("STEAM_SYNC_DEBUG", "4. Найкращий збіг знайдено! Назва з IGDB: [" + igdb.name + "]");

                    // Перевіряємо дублікат ще раз (можливо під іншою назвою IGDB)
                    Game doubleCheckGame = dbHelper.getGameByName(igdb.name);
                    if (doubleCheckGame != null) {
                        Log.d("STEAM_SYNC_DEBUG", "5. СТОП! Гра [" + igdb.name + "] вже є в базі. Оновлюємо години.");
                        if (hoursPlayed > 0) doubleCheckGame.setTime(hoursPlayed);
                        dbHelper.updateGame(doubleCheckGame);
                        return;
                    }

                    // --- БЕЗПЕЧНЕ ЧИТАННЯ ДАНИХ З IGDB ---
                    String coverUrl = "";
                    if (igdb.cover != null && igdb.cover.url != null) {
                        coverUrl = igdb.cover.url.replace("t_thumb", "t_1080p").replace("//", "https://");
                    }

                    List<String> genres = new ArrayList<>();
                    if (igdb.genres != null) {
                        for (IgdbGame.Genre g : igdb.genres) if (g.name != null) genres.add(g.name);
                    }

                    List<String> platforms = new ArrayList<>();
                    if (igdb.platforms != null) {
                        for (IgdbGame.Platform p : igdb.platforms) if (p.name != null) platforms.add(p.name);
                    }

                    Float rating = (float) (igdb.aggregated_rating / 10.0);
                    String summary = (igdb.summary != null) ? igdb.summary : "";

                    // Якщо 0 годин -> Planned, якщо грав -> Completed
                    String targetCategory = (hoursPlayed > 0) ? "completed" : "planned";
                    Float finalTime = (hoursPlayed > 0) ? hoursPlayed : null;
                    String finalEndDate = (hoursPlayed > 0) ? lastPlayedDate : null;
                    String finalAddedDate = (hoursPlayed == 0) ? today : null;

                    Log.d("STEAM_SYNC_DEBUG", "6. Формуємо об'єкт для БД. Category: " + targetCategory + " | Hours: " + finalTime);

                    Game newGame = new Game(
                            0, igdb.name, targetCategory, summary, rating, coverUrl,
                            new ArrayList<>(), genres, new ArrayList<>(), "", "", "",
                            new ArrayList<>(), new ArrayList<>(), "", platforms, rating,
                            "", "", "Main Game", "", "", "", new ArrayList<>(), coverUrl,
                            null, null, finalEndDate, finalAddedDate, null, null, null, "Steam Import", 1, finalTime
                    );

                    long dbId = dbHelper.addGame(newGame);
                    if (dbId != -1) {
                        Log.d("STEAM_SYNC_DEBUG", "7. УСПІХ! Гра [" + igdb.name + "] додана в БД з ID: " + dbId);
                    } else {
                        Log.e("STEAM_SYNC_DEBUG", "7. ПОМИЛКА SQLITE! dbHelper.addGame повернув -1 для [" + igdb.name + "]");
                    }
                } else {
                    Log.w("STEAM_SYNC_DEBUG", "4. IGDB не знайшов точного збігу для [" + gameName + "]. Запускаємо ФОЛБЕК.");
                    saveGameAsFallback(gameName, hoursPlayed, lastPlayedDate, today);
                }
            } else {
                Log.e("STEAM_SYNC_DEBUG", "3. Помилка відповіді IGDB: " + response.code() + " | Звертаємось до фолбеку.");
                saveGameAsFallback(gameName, hoursPlayed, lastPlayedDate, today);
            }
        } catch (Exception e) {
            Log.e("STEAM_SYNC_DEBUG", "КРИТИЧНА ПОМИЛКА в fetchAndSaveNewGame для [" + gameName + "]: " + e.getMessage(), e);
            saveGameAsFallback(gameName, hoursPlayed, lastPlayedDate, today);
        }
    }

    // --- ФОЛБЕК МЕТОД: ЗБЕРІГАЄ ГРУ НАВІТЬ ЯКЩО ЇЇ НЕМАЄ НА IGDB ---
    private void saveGameAsFallback(String gameName, Float hoursPlayed, String lastPlayedDate, String today) {
        Log.d("STEAM_SYNC_DEBUG", "-> Фолбек старт для [" + gameName + "]");

        Game checkGame = dbHelper.getGameByName(gameName);
        if (checkGame != null) {
            Log.d("STEAM_SYNC_DEBUG", "-> Фолбек стоп: Гра вже є в БД.");
            return;
        }

        String targetCategory = (hoursPlayed > 0) ? "completed" : "planned";
        Float finalTime = (hoursPlayed > 0) ? hoursPlayed : null;
        String finalEndDate = (hoursPlayed > 0) ? lastPlayedDate : null;
        String finalAddedDate = (hoursPlayed == 0) ? today : null;

        Game fallbackGame = new Game(
                0, gameName, targetCategory, "", 0f, "",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", "", "",
                new ArrayList<>(), new ArrayList<>(), "", new ArrayList<>(), 0f,
                "", "", "Main Game", "", "", "", new ArrayList<>(), "",
                null, null, finalEndDate, finalAddedDate, null, null, null, "Steam Import (Fallback)", 1, finalTime
        );

        long dbId = dbHelper.addGame(fallbackGame);
        if (dbId != -1) {
            Log.d("STEAM_SYNC_DEBUG", "-> ФОЛБЕК УСПІХ! [" + gameName + "] додана в БД без даних IGDB.");
        } else {
            Log.e("STEAM_SYNC_DEBUG", "-> ФОЛБЕК ПОМИЛКА! Не вдалося записати [" + gameName + "] в БД.");
        }
    }

    private IgdbGame findBestMatch(List<IgdbGame> results, String targetName) {
        if (results == null || results.isEmpty()) return null;
        String targetClean = targetName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        for (IgdbGame g : results) if (g.name != null && g.name.equalsIgnoreCase(targetName)) return g;
        for (IgdbGame g : results) if (g.name != null && g.name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase().equals(targetClean)) return g;
        for (IgdbGame g : results) if (g.name != null && (g.name.toLowerCase().contains(targetName.toLowerCase()) || targetName.toLowerCase().contains(g.name.toLowerCase()))) return g;
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Steam Sync Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows background sync progress");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String contentText, int current, int max) {
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Steam Syncing...")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_steam) // Обов'язково своя іконка
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setProgress(max, current, false);
        return notificationBuilder.build();
    }

    private void updateNotification(String text, int current, int max) {
        notificationBuilder.setContentText(text).setProgress(max, current, false);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Ми не прив'язуємо цей сервіс до UI
    }
}