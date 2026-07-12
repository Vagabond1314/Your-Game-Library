package com.your_game_library;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class GameDetailsActivity extends AppCompatActivity {

    // UI Елементи
    private ImageView mainImage;
    private TextView title, released, rating, description, category, detailsHltb;
    private TextView detailsAggregatedRating, detailsStoryline, detailsGameCategory, collectionHeader;
    private View storylineHeader, collectionScroll, hltbDivider;
    private ChipGroup platformsChipGroup, languagesChipGroup, similarGamesChipGroup, collectionChipGroup;
    private ChipGroup genresChipGroup, tagsChipGroup;
    private ViewPager2 screenshotsViewPager;
    private Button btnPlanned, btnPlaying, btnCompleted, btnOpenIgdb, btnOpenSteam;

    // IGDB Credentials
    String CLIENT_ID = Config.IGDB_CLIENT_ID;
    String CLIENT_SECRET = Config.IGDB_CLIENT_SECRET;

    private Game game;
    private int gameId = -1;
    private String gameNameFromIntent;
    private GameDatabaseHelper db;
    private IgdbApiService igdbApi;
    private boolean isTagsExpanded = false;
    private boolean isSimilarExpanded = false;
    private boolean isSeriesExpanded = false;
    private boolean isPlatformsExpanded = false;
    private boolean isLanguagesExpanded = false;
    private boolean isStorylineExpanded = false;
    private boolean isSummaryLineExpanded = false;

    private final int CHIPS_LIMIT = 5; // Ліміт для всіх груп
    private String mainImageUrl = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = GameDatabaseHelper.getInstance(this);

        // Отримання даних з Intent
        gameId = getIntent().getIntExtra("game_id", -1);
        gameNameFromIntent = getIntent().getStringExtra("game_name");

        if (gameId == -1 && (gameNameFromIntent == null || gameNameFromIntent.isEmpty())) {
            Toast.makeText(this, "Game data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- НОВА ЛОГІКА: ПЕРЕВІРКА В БД ---
        Game existingGame = null;
        if (gameId != -1) {
            existingGame = db.getGameByIdObject(gameId); // Перевіряємо за ID
        }

        // Якщо за ID не знайшло, або ID не було, перевіряємо за назвою (для надійності)
        if (existingGame == null && gameNameFromIntent != null) {
            if (db.isGameExists(gameNameFromIntent)) {
                // Якщо метод getGameByNameObject не існує у твоєму хелпері, просто отримай категорію,
                // або витягни ID через базу, щоб передати його далі.
                // Припустимо, що у нас є спосіб знайти гру:
                List<Game> allGames = db.getAllGames();
                for(Game g : allGames) {
                    if(g.getName().equalsIgnoreCase(gameNameFromIntent)) {
                        existingGame = g;
                        break;
                    }
                }
            }
        }

        // ЯКЩО ГРА ВЖЕ Є В БАЗІ -> Відкриваємо MyGameDetailsActivity і ЗАКРИВАЄМО цей екран
        if (existingGame != null) {
            Intent intent = new Intent(this, MyGameDetailsActivity.class);
            intent.putExtra("gameId", existingGame.getId());
            startActivity(intent);
            finish(); // Важливо: закриваємо GameDetailsActivity, щоб користувач не міг повернутися сюди кнопкою "Назад"
            return;   // Припиняємо виконання onCreate
        }

        // --- СТАРА ЛОГІКА (якщо гри немає в БД) ---
        setContentView(R.layout.activity_game_details);
        initViews();
        initRetrofit();
        setupButtons();
        loadGameDetails();
    }
    private void initViews() {
        mainImage = findViewById(R.id.detailsImage);
        title = findViewById(R.id.detailsTitle);
        released = findViewById(R.id.detailsReleased);
        rating = findViewById(R.id.detailsRating);
        description = findViewById(R.id.detailsDescription);
        category = findViewById(R.id.detailsCategory);
        //detailsHltb = findViewById(R.id.detailsHltb);

        genresChipGroup = findViewById(R.id.genresChipGroup);
        tagsChipGroup = findViewById(R.id.tagsChipGroup);
        platformsChipGroup = findViewById(R.id.platformsChipGroup);
        languagesChipGroup = findViewById(R.id.languagesChipGroup);

        screenshotsViewPager = findViewById(R.id.screenshotsViewPager);

        btnPlanned = findViewById(R.id.btnAddPlanned);
        btnPlaying = findViewById(R.id.btnAddPlaying);
        btnCompleted = findViewById(R.id.btnAddCompleted);
        Button btnManageCols = findViewById(R.id.btnManageCollections);
        btnOpenIgdb = findViewById(R.id.btnOpenIgdb); // Використовуємо існуючий ID кнопки
        detailsAggregatedRating = findViewById(R.id.detailsAggregatedRating);
        detailsStoryline = findViewById(R.id.detailsStoryline);
        storylineHeader = findViewById(R.id.storylineHeader);
        detailsGameCategory = findViewById(R.id.detailsGameCategory);
    }
    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.igdb.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        igdbApi = retrofit.create(IgdbApiService.class);
    }
    private void setupButtons() {
        btnPlanned.setOnClickListener(v -> showPlannedBottomSheet());
        btnPlaying.setOnClickListener(v -> showPlayingBottomSheet());
        btnCompleted.setOnClickListener(v -> showCompletedBottomSheet());

        // Ініціалізація кнопки копіювання
//        View btnCopy = findViewById(R.id.btnCopyTitle);
//        if (btnCopy != null) {
//            btnCopy.setOnClickListener(v -> {
//                String gameName = title.getText().toString();
//                if (!gameName.isEmpty()) {
//                    // Копіювання в буфер обміну
//                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//                    android.content.ClipData clip = android.content.ClipData.newPlainText("Game Title", gameName);
//                    clipboard.setPrimaryClip(clip);
//
//                    // Візуальне підтвердження для користувача
//                    Toast.makeText(this, "Title copied to clipboard", Toast.LENGTH_SHORT).show();
//
//                    // Легка вібрація (опціонально)
//                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
//                }
//            });
//        }

        btnOpenIgdb.setOnClickListener(v -> {
            if (game != null) {
                String url = "";
                String savedUrl = game.getIgdbUrl(); // Повне посилання з бази
                String slug = game.getRawgSlug();     // Slug (текстовий ID)

                // 1. Пріоритет №1: Якщо в базі вже лежить повне посилання
                if (savedUrl != null && savedUrl.startsWith("http")) {
                    url = savedUrl;
                }
                // 2. Пріоритет №2: Якщо є slug, будуємо пряме посилання
                else if (slug != null && !slug.isEmpty()) {
                    url = "https://www.igdb.com/games/" + slug;
                }
                // 3. Фолбек: Розумний пошук, якщо нічого не знайдено
                else {
                    String cleanQuery = game.getName().replaceAll("[^a-zA-Z0-9 ]", " ");
                    url = "https://www.igdb.com/search?q=" + Uri.encode(cleanQuery);
                }

                // Відкриваємо зовнішній браузер
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    // На випадок, якщо на пристрої немає жодного браузера (малоймовірно)
                    Toast.makeText(this, "Неможливо відкрити посилання", Toast.LENGTH_SHORT).show();
                    Log.e("BROWSER_ERROR", "Error opening browser: " + e.getMessage());
                }
            }
        });
        if (btnOpenSteam != null) {
            btnOpenSteam.setOnClickListener(v -> {
                if (game != null && game.getSteamUrl() != null && !game.getSteamUrl().isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(game.getSteamUrl()));
                    startActivity(intent);
                }
            });
        }
        // 1. Встановлюємо СУЦІЛЬНИЙ колір статус-бара (як фон додатка)
        getWindow().setStatusBarColor(Color.parseColor("#121212"));

        // Якщо хочеш, щоб іконки годинника були білими (на темному фоні)
        getWindow().getDecorView().setSystemUiVisibility(0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
            if (Math.abs(verticalOffset) - appBarLayout1.getTotalScrollRange() == 0) {
                // Повністю згорнуто
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                toolbar.setTitle(game.getName());
            } else {
                // Розгорнуто
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        });
    }
    private void loadGameDetails() {
        // 1. Спочатку перевіряємо, чи є ця гра вже в кеші
        Game cachedGame = GameCache.getInstance().getGame(gameId);

        if (cachedGame != null) {
            // ГРА ЗНАЙДЕНА В КЕШІ!
            // Відразу показуємо її, не роблячи жодного запиту в інтернет
            Log.d("CACHE_DEBUG", "Завантажено гру з кешу: " + cachedGame.getName());
            this.game = cachedGame;

            // Запускаємо відображення
            displayGame(game);
            setupStoreButtons(game.getSteamUrl(), game.getPsUrl(), game.getXboxUrl(), game.getNintendoUrl());
            // Визначаємо ID категорії для відображення
            Integer catId = 0; // Для відображення значка (0 = Main Game)
            if (game.getGameCategory() != null && game.getGameCategory().contains("DLC")) catId = 1;
            updateCategoryBadge(catId, game.getName());

        } else {
            // ГРИ В КЕШІ НЕМАЄ. Йдемо в IGDB
            Log.d("CACHE_DEBUG", "Гри немає в кеші. Завантажуємо з IGDB...");
            fetchIgdbTokenAndData();
        }
    }
    private void fetchIgdbTokenAndData() {
        igdbApi.getToken(CLIENT_ID, CLIENT_SECRET, "client_credentials")
                .enqueue(new retrofit2.Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            String token = "Bearer " + response.body().access_token;
                            fetchGameFromIgdb(token);
                        } else {
                            showToast("Auth failed: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        showToast("Network error during auth");
                    }
                });
    }
    private void fetchGameFromIgdb(String token) {
        // 1. Поля запиту
        String fields = "fields name, summary, storyline, url, category, game_type, first_release_date, " +
                "total_rating, aggregated_rating, cover.url, genres.name, themes.name, " +
                "keywords.name, platforms.name, similar_games.name, similar_games.id, " +
                "websites.url, websites.category, external_games.url, external_games.category, " +
                "language_supports.language.name, screenshots.url, slug; ";

        if (gameId != -1) {
            String query = fields + "where id = " + gameId + ";";
            Log.d("IGDB_STEP", "Запуск Deep Fetch за ID: " + gameId);
            executeDeepFetch(token, query);
        } else {
            String cleanName = gameNameFromIntent.replace("\"", "");
            String searchIdQuery = "fields id; search \"" + cleanName + "\"; limit 1;";

            Log.d("IGDB_STEP", "Запуск Пошуку ID для: " + cleanName);

            igdbApi.getGames(CLIENT_ID, token, searchIdQuery).enqueue(new Callback<List<IgdbGame>>() {
                @Override
                public void onResponse(Call<List<IgdbGame>> call, Response<List<IgdbGame>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        int realId = response.body().get(0).id;
                        Log.d("IGDB_STEP", "ID знайдено: " + realId + ". Тепер вантажимо деталі.");
                        executeDeepFetch(token, fields + "where id = " + realId + ";");
                    } else {
                        Log.e("IGDB_STEP", "ID не знайдено через пошук.");
                    }
                }
                @Override
                public void onFailure(Call<List<IgdbGame>> call, Throwable t) {
                    Log.e("IGDB_STEP", "Помилка мережі при пошуку ID.");
                }
            });
        }
    }
    private void fetchSeriesData(String token, int gameId) {
        // Прямий запит до таблиці колекцій
        String query = "fields name, games.name, games.id; where games = (" + gameId + ");";

        igdbApi.getCollections(CLIENT_ID, token, query).enqueue(new Callback<List<IgdbGame.IgdbSeries>>() {
            @Override
            public void onResponse(Call<List<IgdbGame.IgdbSeries>> call, Response<List<IgdbGame.IgdbSeries>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    IgdbGame.IgdbSeries foundSeries = response.body().get(0);

                    // 1. Формуємо список ігор серії у форматі "назва|id"
                    List<String> sGames = new ArrayList<>();
                    if (foundSeries.games != null) {
                        for (IgdbGame.GameInCollection gc : foundSeries.games) {
                            // Не додаємо саму цю гру в список її ж серії
                            if (gc.id != gameId) {
                                sGames.add(gc.name + "|" + gc.id);
                            }
                        }
                    }

                    // 2. Сетимо дані в об'єкт game
                    game.setCollection(foundSeries.name);
                    game.setSeriesGames(sGames);

                    if (game.getCategory() != null && !game.getCategory().isEmpty()) {
                        new Thread(() -> {
                            db.updateGame(game);
                        }).start();
                    }
                    // 4. Оновлюємо UI
                    runOnUiThread(() -> {
                        View btnSeries = findViewById(R.id.btnSeries);
                        TextView tvSeriesTitle = findViewById(R.id.tvSeriesTitle);

                        if (btnSeries != null) {
                            btnSeries.setVisibility(View.VISIBLE);
                            if (tvSeriesTitle != null) {
                                tvSeriesTitle.setText(foundSeries.name + " Series");
                            }

                            btnSeries.setOnClickListener(v -> {
                                Intent intent = new Intent(GameDetailsActivity.this, SeriesActivity.class);
                                intent.putExtra("GAME_ID", gameId);
                                intent.putExtra("GAME_NAME", game.getName());
                                startActivity(intent);
                            });
                        }
                    });
                } else {
                    Log.d("IGDB_SERIES", "Серію не знайдено через /collections");
                    // Якщо серію не знайдено, можна спробувати аналогічний запит до /franchises
                }
            }

            @Override
            public void onFailure(Call<List<IgdbGame.IgdbSeries>> call, Throwable t) {
                Log.e("IGDB_SERIES", "Error: " + t.getMessage());
            }
        });
    }
    // Допоміжний метод для завантаження ПОВНИХ даних
    private void executeDeepFetch(String token, String query) {
        Log.d("IGDB_QUERY", "Final Body: " + query);

        // 1. Отримуємо СИРИЙ ТЕКСТ відповіді (String)
        igdbApi.getGamesRaw(CLIENT_ID, token, query).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ДИВИСЬ ЦЕ В LOGCAT!
                    Log.d("IGDB_RAW_JSON", "RAW RESPONSE: " + response.body());

                    String raw = response.body();
                    if (raw.contains("\"collection\":")) {
                        Log.i("IGDB_DIAGNOSE", "УСПІХ: Поле collection є в JSON!");
                    } else {
                        Log.e("IGDB_DIAGNOSE", "ПОМИЛКА: Поле collection ВІДСУТНЄ в JSON. Перевір API або назву гри.");
                    }

                    if (raw.contains("\"franchises\":")) {
                        Log.i("IGDB_DIAGNOSE", "УСПІХ: Поле franchises є в JSON!");
                    }
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {}
        });

        // 2. Основний запит для об'єктів
        igdbApi.getGames(CLIENT_ID, token, query).enqueue(new retrofit2.Callback<List<IgdbGame>>() {
            @Override
            public void onResponse(Call<List<IgdbGame>> call, Response<List<IgdbGame>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    mapIgdbToGame(response.body().get(0), token);
                }
            }
            @Override
            public void onFailure(Call<List<IgdbGame>> call, Throwable t) {
                Log.e("IGDB_ERROR", "Deep fetch failed: " + t.getMessage());
            }
        });
    }
    private void fetchTimeToBeatData(String token, int igdbId) {
        String query = "fields hastily, normally, completely; where game_id = " + igdbId + ";";

        igdbApi.getTimeToBeat(CLIENT_ID, token, query).enqueue(new retrofit2.Callback<List<IgdbTimeToBeat>>() {
            @Override
            public void onResponse(Call<List<IgdbTimeToBeat>> call, Response<List<IgdbTimeToBeat>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    IgdbTimeToBeat ttb = response.body().get(0);

                    // 1. Формуємо значення для кожного типу проходження
                    // Якщо годин 0 — ставимо "-", якщо більше — число + "h"
                    String mainT = (ttb.hastily / 3600 > 0) ? (ttb.hastily / 3600) + "h" : "-";
                    String extraT = (ttb.normally / 3600 > 0) ? (ttb.normally / 3600) + "h" : "-";
                    String complT = (ttb.completely / 3600 > 0) ? (ttb.completely / 3600) + "h" : "-";

                    // 2. Створюємо рядок для бази даних з розділювачем
                    String hltbResult = mainT + "|" + extraT + "|" + complT;

                    if (game != null) game.setHltb(hltbResult);

                    runOnUiThread(() -> {
                        // 3. Знаходимо контейнер та окремі TextView
                        View container = findViewById(R.id.hltbContainer);
                        TextView tvMain = findViewById(R.id.tvHltbMain);
                        TextView tvExtras = findViewById(R.id.tvHltbExtras);
                        TextView tvComplete = findViewById(R.id.tvHltbComplete);

                        if (container != null) {
                            container.setVisibility(View.VISIBLE);
                            if (tvMain != null) tvMain.setText(mainT);
                            if (tvExtras != null) tvExtras.setText(extraT);
                            if (tvComplete != null) tvComplete.setText(complT);
                        }

                        // Якщо ти все ще використовуєш старий одиночний TextView для тестів:
                        if (detailsHltb != null) {
                            detailsHltb.setText(mainT + " / " + extraT + " / " + complT);
                            detailsHltb.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<IgdbTimeToBeat>> call, Throwable t) {
                Log.e("IGDB_TTB", "Error: " + t.getMessage());
            }
        });
    }
    private void mapIgdbToGame(IgdbGame igdb, String token) {
        final int safeCategoryId = (igdb.category != null) ? igdb.category : 0;
        Game existingGame = db.getGameByIdObject(igdb.id);

        String gameTypeName = getCategoryName(igdb.category, igdb.name);

        String currentCat = "";
        String currentLocalPath = null;
        if (existingGame != null) {
            currentCat = existingGame.getCategory();
            currentLocalPath = existingGame.getImagePath();
        }

        String finalDescription = igdb.summary;
        String finalName = igdb.name;
        String finalStoryline = (igdb.storyline != null ? igdb.storyline : "");

        // --- БЕЗПЕЧНИЙ розрахунок рейтингу користувачів ---
        Float finalRating = (float) (igdb.total_rating / 10.0f);

        if (existingGame != null) {
            currentCat = existingGame.getCategory();
            currentLocalPath = existingGame.getImagePath();

            if (existingGame.getDescription() != null && !existingGame.getDescription().isEmpty()) {
                finalDescription = existingGame.getDescription();
            }
            if (existingGame.getName() != null && !existingGame.getName().isEmpty()) {
                finalName = existingGame.getName();
            }
            if (existingGame.getStoryline() != null && !existingGame.getStoryline().isEmpty()) {
                finalStoryline = existingGame.getStoryline();
            }
            if (existingGame.getRating() != null && existingGame.getRating() > 0) {
                finalRating = existingGame.getRating();
            }
        }

        // 1. Жанри
        List<String> genres = new ArrayList<>();
        if (igdb.genres != null) for (IgdbGame.Genre g : igdb.genres) genres.add(g.name);

        // 2. Теги (Themes + Keywords)
        List<String> combinedTags = new ArrayList<>();
        if (igdb.themes != null) for (IgdbGame.Theme t : igdb.themes) combinedTags.add(t.name);
        if (igdb.keywords != null) for (IgdbGame.Keyword k : igdb.keywords) combinedTags.add(k.name);

        // 3. Скріншоти
        List<String> screenshotUrls = new ArrayList<>();
        if (igdb.screenshots != null) {
            for (IgdbGame.Screenshot s : igdb.screenshots) screenshotUrls.add(getHighResUrl(s.url, "t_720p"));
        }

        // 4. Платформи
        List<String> platforms = new ArrayList<>();
        if (igdb.platforms != null) for (IgdbGame.Platform p : igdb.platforms) platforms.add(p.name);

        // 5. Схожі ігри
        List<String> similarWithIds = new ArrayList<>();
        if (igdb.similar_games != null) {
            for (IgdbGame.SimilarGame sg : igdb.similar_games) similarWithIds.add(sg.name + "|" + sg.id);
        }

        // 6. Мови
        List<String> languages = new ArrayList<>();
        if (igdb.language_supports != null) {
            for (IgdbGame.LanguageSupport ls : igdb.language_supports) {
                if (ls.language != null && !languages.contains(ls.language.name)) languages.add(ls.language.name);
            }
        }

        // 7. Посилання на магазини
        String steamLink = "";
        String psLink = "";
        String xboxLink = "";
        String nintendoLink = "";

        if (igdb.websites != null) {
            for (IgdbGame.Website w : igdb.websites) {
                String url = (w.url != null) ? w.url.toLowerCase() : "";
                if (w.category == 13 || url.contains("steampowered.com") || url.contains("steamcommunity.com")) {
                    if (steamLink.isEmpty()) steamLink = w.url;
                } else if (url.contains("playstation.com")) {
                    if (psLink.isEmpty()) psLink = w.url;
                } else if (url.contains("xbox.com") || url.contains("microsoft.com")) {
                    if (url.contains("/p/") || url.contains("/games/") || url.contains("store")) {
                        if (xboxLink.isEmpty()) xboxLink = w.url;
                    }
                } else if (url.contains("nintendo.com") || url.contains("nintendo.co")) {
                    if (nintendoLink.isEmpty()) nintendoLink = w.url;
                }
            }
        }

        if (igdb.cover != null) {
            mainImageUrl = getHighResUrl(igdb.cover.url, "t_1080p");
        }

        // Безпечна дата релізу
        long safeReleaseDate = igdb.first_release_date;

        // 9. СТВОРЮЄМО ОБ'ЄКТ GAME (З безпечними null-перевірками)
        game = new Game(
                igdb.id,
                finalName,
                currentCat,
                finalDescription,
                finalRating,
                (currentLocalPath != null) ? currentLocalPath : mainImageUrl,
                combinedTags,
                genres,
                screenshotUrls,
                formatReleaseDate(safeReleaseDate),
                "",
                steamLink,
                languages,
                similarWithIds,
                "",
                platforms,
                // --- БЕЗПЕЧНИЙ розрахунок рейтингу критиків ---
                finalRating = (float) (igdb.total_rating / 10.0f),
                finalStoryline,
                igdb.url,
                gameTypeName,
                xboxLink,
                psLink,
                nintendoLink,
                new ArrayList<>(),
                mainImageUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        game.setRawgSlug(igdb.slug);

        // 10. ОНОВЛЕННЯ UI
        final String finalSteam = steamLink;
        final String finalPs = psLink;
        final String finalXbox = xboxLink;
        final String finalNintendo = nintendoLink;

        runOnUiThread(() -> {
            displayGame(game);
            setupStoreButtons(finalSteam, finalPs, finalXbox, finalNintendo);
            updateCategoryBadge(igdb.category, igdb.name);
        });

        View btnSeries = findViewById(R.id.btnSeries);
        if (btnSeries != null) btnSeries.setVisibility(View.GONE);

        fetchTimeToBeatData(token, igdb.id);
        fetchSeriesData(token, igdb.id);
        GameCache.getInstance().putGame(igdb.id, game);
    }
    private void setupStoreButtons(String steam, String ps, String xbox, String nintendo) {
        View btnSteam = findViewById(R.id.btnOpenSteam);
        View btnPs = findViewById(R.id.btnPlayStation);
        View btnXbox = findViewById(R.id.btnXbox);
        View btnNintendo = findViewById(R.id.btnNintendo);

        handleUrlButton(btnSteam, steam);
        handleUrlButton(btnPs, ps);
        handleUrlButton(btnXbox, xbox);
        handleUrlButton(btnNintendo, nintendo);
    }

    private void handleUrlButton(View button, String url) {
        if (url != null && !url.isEmpty()) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        } else {
            button.setVisibility(View.GONE);
        }
    }
    private void updateCategoryBadge(Integer categoryId, String gameName) {
        if (detailsGameCategory == null) return;

        String name = getCategoryName(categoryId, gameName);

        // Якщо назва категорії порожня (для Main Game) або null - ховаємо плашку
        if (name == null || name.isEmpty()) {
            detailsGameCategory.setVisibility(View.GONE);
        } else {
            detailsGameCategory.setText(name);
            detailsGameCategory.setVisibility(View.VISIBLE);

            // Якщо це бандл або спец. видання - синій колір, якщо DLC - золотий
            if (name.contains("Bundle") || name.contains("Edition")) {
                detailsGameCategory.setTextColor(Color.parseColor("#2D5E85")); // Синій
            } else {
                detailsGameCategory.setTextColor(Color.parseColor("#FFD700")); // Золотий
            }
        }
    }
    private String formatReleaseDate(long seconds) {
        if (seconds <= 0) {
            return "TBA"; // To Be Announced (якщо дати немає)
        }

        try {
            // Конвертуємо секунди в мілісекунди
            Date date = new Date(seconds * 1000L);

            // Встановлюємо формат (можна змінити на "dd MMM yyyy")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            return sdf.format(date);
        } catch (Exception e) {
            return "Unknown";
        }
    }
    // Допоміжний метод для категорій
    private String getCategoryName(Integer catId, String gameName) {
        if (catId == null) return "";

        // Якщо IGDB каже, що це "Main Game" (0), але в назві є "Bundle" або "Edition"
        if (catId == 0 && gameName != null) {
            String name = gameName.toLowerCase();
            if (name.contains("bundle") || name.contains("collection")) return "Bundle";
            if (name.contains("edition") || name.contains("version")) return "Special Edition";
            if (name.contains("expansion") || name.contains("dlc")) return "Expansion";

            // Якщо це просто основна гра - повертаємо порожньо, щоб сховати плашку
            return "";
        }

        switch (catId) {
            case 1: return "DLC / Addon";
            case 2: return "Expansion";
            case 3: return "Bundle";
            case 4: return "Standalone Expansion";
            case 5: return "Mod";
            case 6: return "Episode";
            case 7: return "Season Pass";
            case 8: return "Remake";
            case 9: return "Remaster";
            case 10: return "Expanded Game";
            case 11: return "Port";
            case 13: return "Pack";
            default: return "";
        }
    }
    private void displayGame(Game g) {
        if (g == null) return;

        // 1. Основна інформація
        title.setText(g.getName());
        rating.setText(String.format(Locale.getDefault(), "%.1f", g.getRating()));
        released.setText(g.getReleased());

        // 2. Статус (Playing/Planned/Played)
        String cat = db.getCategoryByGameName(g.getName());
        category.setText(cat != null ? cat : "Not tracked");

        if (detailsGameCategory != null) {
            String type = g.getGameCategory();
            if (type != null && !type.isEmpty()) {
                detailsGameCategory.setText(type);
                detailsGameCategory.setVisibility(View.VISIBLE);

                // Стиль: якщо це не основна гра, можна виділити іншим кольором
                if (!type.equals("Main Game")) {
                    detailsGameCategory.setTextColor(Color.parseColor("#FFD700")); // Золотий для DLC/Remake
                } else {
                    detailsGameCategory.setTextColor(Color.parseColor("#58A870")); // Зелений для Main
                }
            } else {
                detailsGameCategory.setVisibility(View.GONE);
            }
        }

        String currentStatus = db.getCategoryByGameName(g.getName());
        // 3. Рейтинг критиків
        TextView criticsRating = findViewById(R.id.detailsAggregatedRating);
        if (g.getAggregatedRating() > 0) {
            criticsRating.setText(String.format(Locale.getDefault(), "%.1f", g.getAggregatedRating()));
        } else {
            criticsRating.setText("N/A");
        }

        // 4. Жанри та Теги (Chips)
        addChipsToGroup(genresChipGroup, g.getGenres());
        if (g.getTags() != null && !g.getTags().isEmpty()) {
            addExpandableChips(tagsChipGroup, g.getTags(), 0);
        }

// --- ПЛАТФОРМИ ---
        if (g.getPlatforms() != null && !g.getPlatforms().isEmpty()) {
            // Використовуємо глобальну змінну platformsChipGroup і тип 4
            addExpandableChips(platformsChipGroup, g.getPlatforms(), 4);
        }

// --- МОВИ ---
        if (g.getLanguages() != null && !g.getLanguages().isEmpty()) {
            // Використовуємо глобальну змінну languagesChipGroup і тип 3
            addExpandableChips(languagesChipGroup, g.getLanguages(), 3);
        }

        TextView tvSummaryClickHint = findViewById(R.id.tvSummaryClickHint);
        if (g.getDescription() != null && !g.getDescription().isEmpty()) {
            description.setVisibility(View.VISIBLE);
            description.setText(g.getDescription());

            // Встановлюємо обмеження в 4 рядки
            description.setMaxLines(isSummaryLineExpanded ? Integer.MAX_VALUE : 4);
            description.setEllipsize(android.text.TextUtils.TruncateAt.END);

            // Логіка показу підказки "Read more"
            description.post(() -> {
                if (description.getLineCount() >= 4) {
                    tvSummaryClickHint.setVisibility(View.VISIBLE);
                    tvSummaryClickHint.setText(isSummaryLineExpanded ? "Show less" : "Read more...");
                } else {
                    tvSummaryClickHint.setVisibility(View.GONE);
                }
            });

            View.OnClickListener toggleStory = v -> {
                isSummaryLineExpanded = !isSummaryLineExpanded;
                description.setMaxLines(isSummaryLineExpanded ? Integer.MAX_VALUE : 4);
                tvSummaryClickHint.setText(isSummaryLineExpanded ? "Show less" : "Read more...");
            };

            description.setOnClickListener(toggleStory);
            tvSummaryClickHint.setOnClickListener(toggleStory);
        } else {
            description.setVisibility(View.GONE);
            tvSummaryClickHint.setVisibility(View.GONE);
        }
// --- STORYLINE (Сюжет) ---
        TextView tvStorylineHint = findViewById(R.id.tvStorylineClickHint);
        if (g.getStoryline() != null && !g.getStoryline().isEmpty()) {
            storylineHeader.setVisibility(View.VISIBLE);
            detailsStoryline.setVisibility(View.VISIBLE);
            detailsStoryline.setText(g.getStoryline());

            // Встановлюємо обмеження в 4 рядки
            detailsStoryline.setMaxLines(isStorylineExpanded ? Integer.MAX_VALUE : 4);
            detailsStoryline.setEllipsize(android.text.TextUtils.TruncateAt.END);

            // Логіка показу підказки "Read more"
            detailsStoryline.post(() -> {
                if (detailsStoryline.getLineCount() >= 4) {
                    tvStorylineHint.setVisibility(View.VISIBLE);
                    tvStorylineHint.setText(isStorylineExpanded ? "Show less" : "Read more...");
                } else {
                    tvStorylineHint.setVisibility(View.GONE);
                }
            });

            View.OnClickListener toggleStory = v -> {
                isStorylineExpanded = !isStorylineExpanded;
                detailsStoryline.setMaxLines(isStorylineExpanded ? Integer.MAX_VALUE : 4);
                tvStorylineHint.setText(isStorylineExpanded ? "Show less" : "Read more...");
            };

            detailsStoryline.setOnClickListener(toggleStory);
            tvStorylineHint.setOnClickListener(toggleStory);
        } else {
            storylineHeader.setVisibility(View.GONE);
            detailsStoryline.setVisibility(View.GONE);
            tvStorylineHint.setVisibility(View.GONE);
        }

        // 1. Знаходимо наш новий клікабельний рядок
        View btnSimilarGames = findViewById(R.id.btnSimilarGames);

        if (btnSimilarGames != null) {
            // Перевіряємо, чи є в базі/об'єкті схожі ігри
            if (g.getSimilarGames() != null && !g.getSimilarGames().isEmpty()) {

                // Показуємо кнопку
                btnSimilarGames.setVisibility(View.VISIBLE);

                // Налаштовуємо клік
                btnSimilarGames.setOnClickListener(v -> {
                    Intent intent = new Intent(this, SimilarGamesActivity.class);

                    // Передаємо ID поточної гри, щоб SimilarGamesActivity знала, що шукати
                    intent.putExtra("GAME_ID", g.getId());

                    // Передаємо назву для заголовка в тулбарі
                    intent.putExtra("GAME_NAME", g.getName());

                    startActivity(intent);
                });

            } else {
                // Ховаємо кнопку, якщо схожих ігор немає
                btnSimilarGames.setVisibility(View.GONE);
            }
        }

        loadMainImage();

        // Скріншоти (Гібридна логіка: тільки URL)
        if (g.getScreenshots() != null && !g.getScreenshots().isEmpty()) {
            ScreenshotsAdapter adapter = new ScreenshotsAdapter(this, g.getScreenshots());
            screenshotsViewPager.setAdapter(adapter);
            screenshotsViewPager.setOffscreenPageLimit(3);
        }

// 11. HLTB (Час проходження)
        if (g.getHltb() != null && !g.getHltb().isEmpty()) {
            // Перевіряємо, чи це новий формат з розділювачами (наприклад "10h|15h|20h")
            if (g.getHltb().contains("|")) {
                String[] parts = g.getHltb().split("\\|");
                View container = findViewById(R.id.hltbContainer);
                if (container != null && parts.length == 3) {
                    container.setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.tvHltbMain)).setText(parts[0]);
                    ((TextView) findViewById(R.id.tvHltbExtras)).setText(parts[1]);
                    ((TextView) findViewById(R.id.tvHltbComplete)).setText(parts[2]);
                }
            } else {
                // Якщо це старий формат (простий текст), а старий TextView все ще є
                if (detailsHltb != null) {
                    detailsHltb.setText(g.getHltb());
                    detailsHltb.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    private void showCollectionSelector() {
        // 1. Отримуємо всі створені користувачем колекції
        List<GameCollection> allCollections = db.getAllCollections();
        if (allCollections.isEmpty()) {
            Toast.makeText(this, "Create a collection in the Menu first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Отримуємо ID колекцій, де ця гра вже є
        List<Integer> currentIds = db.getCollectionIdsForGame(game.getId());

        String[] names = new String[allCollections.size()];
        boolean[] checked = new boolean[allCollections.size()];

        for (int i = 0; i < allCollections.size(); i++) {
            names[i] = allCollections.get(i).getName();
            if (currentIds.contains(allCollections.get(i).getId())) {
                checked[i] = true;
            }
        }
        int Color = android.graphics.Color.parseColor("#58A870");
        // 3. Показуємо діалог
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Add to Collections")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> {
                    int colId = allCollections.get(which).getId();
                    if (isChecked) {
                        db.addGameToCollection(colId, game.getId());
                    } else {
                        db.removeGameFromCollection(colId, game.getId());
                    }
                })
                .setPositiveButton("Done", null)
                .setNegativeButton("Cancel", null)
                .create(); // Використовуємо .create() замість .show() відразу

        dialog.show();

        // Встановлюємо БІЛИЙ колір для кнопок після того, як діалог з'явився
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color);
    }
    private void loadMainImage() {
        if (isFinishing() || isDestroyed()) return;

        String localPath = game.getImagePath();
        String remoteUrl = game.getImageUrl(); // Наше нове поле

        // 1. Перевіряємо, чи є локальний шлях і чи існує файл фізично
        if (localPath != null && !localPath.startsWith("http")) {
            File file = new File(localPath);
            if (file.exists()) {
                // ВСЕ ДОБРЕ: файл є, вантажимо з диска
                Glide.with(this).load(file).centerCrop().into(mainImage);
                return;
            }
        }

        // 2. Якщо ми тут, значить: або це нова гра (шлях http),
        // або файл був видалений (після Restore).
        // Вантажимо за посиланням (remoteUrl або localPath якщо він http)
        String urlToLoad = (remoteUrl != null && !remoteUrl.isEmpty()) ? remoteUrl : localPath;

        if (urlToLoad != null && urlToLoad.startsWith("http")) {
            String highRes = urlToLoad.replaceAll("t_\\w+", "t_1080p");
            Glide.with(this)
                    .load(highRes)
                    .placeholder(R.drawable.placeholder)
                    .centerCrop()
                    .into(mainImage);

            // 3. АВТОВІДНОВЛЕННЯ: якщо ми в MyGameDetails (гра вже в базі),
            // запускаємо фонове скачування, щоб наступного разу було офлайн
            if (this instanceof GameDetailsActivity) {
                downloadImageToInternalStorage(highRes);
            }
        } else {
            mainImage.setImageResource(R.drawable.placeholder);
        }
    }
    private void downloadImageToInternalStorage(String imageUrl) {
        // Робимо це в новому потоці, щоб не блокувати UI
        new Thread(() -> {
            Log.d("OFFLINE_SAVE", "Початок фонового завантаження головного фото...");

            // Використовуємо ваш існуючий метод стиснення (він поверне шлях до .webp файлу)
            String localPath = downloadAndCompressImageWithBackoff(imageUrl);

            if (localPath != null && game != null) {
                // Оновлюємо шлях в об'єкті гри
                game.setImagePath(localPath);

                // ЗАПИСУЄМО НОВИЙ ШЛЯХ В БАЗУ ДАНИХ
                // Тепер при наступному відкритті path.startsWith("http") буде false
                db.updateGame(game);

                Log.d("OFFLINE_SAVE", "Головне фото успішно збережено локально: " + localPath);
            }
        }).start();
    }
    private void addChipsToGroup(ChipGroup group, List<String> items) {
        if (group == null) return;
        group.removeAllViews();
        if (items == null || items.isEmpty()) return;

        for (String item : items) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#58A870")));
            chip.setChipStrokeWidth(2f);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipMinHeight(0f);
            chip.setTextColor(Color.WHITE);
            group.addView(chip);
        }
    }
    private void addExpandableChips(ChipGroup group, List<String> items, int type) {
        if (group == null) return;
        if (items == null || items.isEmpty()) {
            group.setVisibility(View.GONE);
            return;
        }
        group.setVisibility(View.VISIBLE);
        group.removeAllViews();

        // Визначаємо стан розгортання
        boolean isExpanded;
        if (type == 0) isExpanded = isTagsExpanded;
        else if (type == 1) isExpanded = isSimilarExpanded;
        else if (type == 2) isExpanded = isSeriesExpanded;
        else if (type == 3) isExpanded = isLanguagesExpanded;
        else isExpanded = isPlatformsExpanded;

        int displayCount = (isExpanded || items.size() <= CHIPS_LIMIT + 1) ? items.size() : CHIPS_LIMIT;

        for (int i = 0; i < displayCount; i++) {
            String rawItem = items.get(i);
            Chip chip = new Chip(this);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setTextColor(Color.WHITE);
            chip.setChipBackgroundColorResource(android.R.color.transparent);

            if (type == 1 || type == 2) {
                // Стиль для ігор (Зелений + Іконка)
                String[] parts = rawItem.split("\\|");
                if (parts.length < 2) continue;
                chip.setText(parts[0]);
                chip.setChipIconResource(android.R.drawable.ic_menu_send);
                chip.setChipIconTint(ColorStateList.valueOf(Color.parseColor("#58A870")));
                chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#58A870")));
                chip.setChipStrokeWidth(4f);
                chip.setOnClickListener(v -> {
                    Intent intent = new Intent(this, GameDetailsActivity.class);
                    intent.putExtra("game_id", Integer.parseInt(parts[1]));
                    intent.putExtra("game_name", parts[0]);
                    startActivity(intent);
                });
            } else if (type == 0) {
                // Стиль для тегів (Сірий)
                chip.setText(rawItem);
                chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#444444")));
                chip.setChipStrokeWidth(1f);
                chip.setTextColor(Color.LTGRAY);
            } else {
                // Стиль для Платформ (4) та Мов (3)
                chip.setText(rawItem);
                chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#58A870")));
                chip.setChipStrokeWidth(2f);
            }
            group.addView(chip);
        }

        // Кнопка перемикання
        if (items.size() > CHIPS_LIMIT + 1) {
            Chip toggleChip = new Chip(this);
            toggleChip.setText(isExpanded ? "Show less" : "+ " + (items.size() - CHIPS_LIMIT) + " more");
            toggleChip.setTextColor(Color.parseColor("#58A870"));
            toggleChip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#58A870")));
            toggleChip.setChipStrokeWidth(2f);
            toggleChip.setChipBackgroundColorResource(android.R.color.transparent);
            toggleChip.setEnsureMinTouchTargetSize(false);

            toggleChip.setOnClickListener(v -> {
                if (type == 0) isTagsExpanded = !isTagsExpanded;
                else if (type == 1) isSimilarExpanded = !isSimilarExpanded;
                else if (type == 2) isSeriesExpanded = !isSeriesExpanded;
                else if (type == 3) isLanguagesExpanded = !isLanguagesExpanded;
                else if (type == 4) isPlatformsExpanded = !isPlatformsExpanded;
                addExpandableChips(group, items, type); // Оновлюємо
            });
            group.addView(toggleChip);
        }
    }
    // Надійний і простий клас для розстановки крапок у даті (dd.MM.yyyy)
    private class SimpleDateWatcher implements android.text.TextWatcher {
        private String current = "";
        private android.widget.EditText input;

        public SimpleDateWatcher(android.widget.EditText input) {
            this.input = input;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().equals(current)) {
                return;
            }

            // Видаляємо все, крім цифр
            String clean = s.toString().replaceAll("[^\\d]", "");
            String cleanC = current.replaceAll("[^\\d]", "");

            int cl = clean.length();
            int sel = cl;
            for (int i = 2; i <= cl && i < 6; i += 2) {
                sel++;
            }
            // Виправлення курсору при видаленні
            if (clean.equals(cleanC)) sel--;

            // Якщо цифр менше 8 - просто форматуємо те, що є
            if (clean.length() < 8) {
                String formatted = "";
                for (int i = 0; i < clean.length(); i++) {
                    if (i == 2 || i == 4) {
                        formatted += ".";
                    }
                    formatted += clean.charAt(i);
                }
                clean = formatted;
            } else {
                // Якщо введено всі 8 цифр, формуємо повну дату і перевіряємо валідність
                int day = Integer.parseInt(clean.substring(0, 2));
                int mon = Integer.parseInt(clean.substring(2, 4));
                int year = Integer.parseInt(clean.substring(4, 8));

                mon = mon < 1 ? 1 : Math.min(mon, 12);
                year = (year < 1900) ? 1900 : Math.min(year, 2100);

                // Перевірка кількості днів у місяці
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.MONTH, mon - 1);
                cal.set(java.util.Calendar.YEAR, year);
                day = Math.min(day, cal.getActualMaximum(java.util.Calendar.DATE));

                clean = String.format(Locale.getDefault(), "%02d.%02d.%04d", day, mon, year);
            }

            current = clean;
            input.setText(current);
            input.setSelection(Math.min(sel, current.length())); // Курсор в кінець
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
    private void showCompletedBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_completed_sheet, null);

        LinearLayout llRatingContainer = view.findViewById(R.id.llRatingContainer);
        com.google.android.material.textfield.TextInputEditText etTime = view.findViewById(R.id.etTimeSpent);
        com.google.android.material.textfield.TextInputEditText etPlays = view.findViewById(R.id.etPlayCount);
        com.google.android.material.textfield.TextInputEditText etStart = view.findViewById(R.id.etStartDate);
        com.google.android.material.textfield.TextInputEditText etEnd = view.findViewById(R.id.etEndDate);
        com.google.android.material.textfield.TextInputEditText etReview = view.findViewById(R.id.etReview);
        Spinner spinnerType = view.findViewById(R.id.spinnerCompletionType);
        Button btnSave = view.findViewById(R.id.btnSaveCompletedStats);

        etStart.setFocusable(false);
        etStart.setFocusableInTouchMode(false);
        etEnd.setFocusable(false);
        etEnd.setFocusableInTouchMode(false);

        String[] types = {"Main Story", "Main + Extras", "100% Completion"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String today = sdf.format(new Date());
        etEnd.setText(today);

        // Календар для Start Date
        etStart.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            try {
                String currentStart = etStart.getText().toString();
                if (!currentStart.isEmpty()) c.setTime(sdf.parse(currentStart));
            } catch (Exception ignored) {}
            new android.app.DatePickerDialog(GameDetailsActivity.this, R.style.MyDialogTheme,
                    (view1, y, m, d) -> etStart.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // Календар для End Date
        etEnd.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            try {
                String currentEnd = etEnd.getText().toString();
                if (!currentEnd.isEmpty()) c.setTime(sdf.parse(currentEnd));
            } catch (Exception ignored) {}
            new android.app.DatePickerDialog(GameDetailsActivity.this, R.style.MyDialogTheme,
                    (view1, y, m, d) -> etEnd.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // Оцінки
        String[] ratingValues = {"None", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        final float[] selectedRating = {0f};
        List<TextView> ratingViews = new ArrayList<>();
        for (int i = 0; i < ratingValues.length; i++) {
            String val = ratingValues[i];
            TextView tv = new TextView(this);
            int sizePx = (int) (50 * getResources().getDisplayMetrics().density);
            int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(0, 0, marginPx, 0);
            tv.setLayoutParams(params);
            tv.setText(val);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(16f);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            android.graphics.drawable.GradientDrawable unselectedBg = new android.graphics.drawable.GradientDrawable();
            unselectedBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            unselectedBg.setColor(Color.parseColor("#262626"));
            android.graphics.drawable.GradientDrawable selectedBg = new android.graphics.drawable.GradientDrawable();
            selectedBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            selectedBg.setColor(Color.parseColor("#FFC107"));
            if (i == 0) {
                tv.setBackground(selectedBg);
                tv.setTextColor(Color.BLACK);
            } else {
                tv.setBackground(unselectedBg);
                tv.setTextColor(Color.WHITE);
            }
            tv.setOnClickListener(v -> {
                for (TextView otherTv : ratingViews) {
                    otherTv.setBackground(unselectedBg);
                    otherTv.setTextColor(Color.WHITE);
                }
                tv.setBackground(selectedBg);
                tv.setTextColor(Color.BLACK);
                selectedRating[0] = val.equals("None") ? 0f : Float.parseFloat(val);
            });
            ratingViews.add(tv);
            llRatingContainer.addView(tv);
        }

        btnSave.setOnClickListener(v -> {
            String startD = etStart.getText().toString().trim();
            String endD = etEnd.getText().toString().trim();

            if (endD.isEmpty()) endD = today;

            if (!startD.isEmpty() && !endD.isEmpty()) {
                try {
                    Date startDate = sdf.parse(startD);
                    Date endDate = sdf.parse(endD);
                    if (startDate != null && endDate != null && startDate.after(endDate)) {
                        Toast.makeText(this, "Start date cannot be after end date!", Toast.LENGTH_LONG).show();
                        etStart.setError("Invalid date");
                        return;
                    }
                } catch (Exception e) { return; }
            }

            // Перетворюємо дані у потрібні типи для бази
            Integer userRatingInt = selectedRating[0] > 0 ? Math.round(selectedRating[0]) : null;

            Float timeInt = null;
            try {
                String tStr = etTime.getText().toString().trim();
                if (!tStr.isEmpty()) timeInt = Float.parseFloat(tStr);
            } catch (Exception ignored) {}

            Integer playsInt = null;
            try {
                String pStr = etPlays.getText().toString().trim();
                if (!pStr.isEmpty()) playsInt = Integer.parseInt(pStr);
            } catch (Exception ignored) {}

            String reviewText = etReview.getText().toString().trim();
            String type = spinnerType.getSelectedItem().toString();

            sheet.dismiss();

            // Зберігаємо в Completed
            saveGameToCategory("completed", userRatingInt, startD, endD, null, null,
                    reviewText.isEmpty() ? null : reviewText, null, type, playsInt, timeInt);
        });

        sheet.setContentView(view);
        sheet.show();
    }
    private void showPlannedBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_planned_sheet, null);

        LinearLayout llRatingContainer = view.findViewById(R.id.PriorityContainer);
        Button btnSave = view.findViewById(R.id.btnSavePlannedGames);

        // Пріоритети (1-10)
        String[] ratingValues = {"None", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        final float[] selectedRating = {0f};
        List<TextView> ratingViews = new ArrayList<>();
        for (int i = 0; i < ratingValues.length; i++) {
            String val = ratingValues[i];
            TextView tv = new TextView(this);
            int sizePx = (int) (50 * getResources().getDisplayMetrics().density);
            int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(0, 0, marginPx, 0);
            tv.setLayoutParams(params);
            tv.setText(val);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(16f);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            android.graphics.drawable.GradientDrawable unselectedBg = new android.graphics.drawable.GradientDrawable();
            unselectedBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            unselectedBg.setColor(Color.parseColor("#262626"));
            android.graphics.drawable.GradientDrawable selectedBg = new android.graphics.drawable.GradientDrawable();
            selectedBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            selectedBg.setColor(Color.parseColor("#2D5E85")); // Синій
            if (i == 0) {
                tv.setBackground(selectedBg);
                tv.setTextColor(Color.BLACK);
            } else {
                tv.setBackground(unselectedBg);
                tv.setTextColor(Color.WHITE);
            }
            tv.setOnClickListener(v -> {
                for (TextView otherTv : ratingViews) {
                    otherTv.setBackground(unselectedBg);
                    otherTv.setTextColor(Color.WHITE);
                }
                tv.setBackground(selectedBg);
                tv.setTextColor(Color.BLACK);
                selectedRating[0] = val.equals("None") ? 0f : Float.parseFloat(val);
            });
            ratingViews.add(tv);
            llRatingContainer.addView(tv);
        }

        String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        btnSave.setOnClickListener(v -> {
            Integer priorityInt = selectedRating[0] > 0 ? Math.round(selectedRating[0]) : null;

            sheet.dismiss();

            // Зберігаємо в Planned
            saveGameToCategory("planned", null, null, null, today, null,
                    null, priorityInt, null, null, null);
        });

        sheet.setContentView(view);
        sheet.show();
    }
    private void showPlayingBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_playing_sheet, null);

        com.google.android.material.textfield.TextInputEditText etStart = view.findViewById(R.id.etStartDate);
        Button btnSave = view.findViewById(R.id.btnSavePlayingGames);

        etStart.setFocusable(false);
        etStart.setFocusableInTouchMode(false);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String today = sdf.format(new Date());
        etStart.setText(today);

        etStart.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            try {
                c.setTime(sdf.parse(etStart.getText().toString()));
            } catch (Exception ignored) {}
            new android.app.DatePickerDialog(GameDetailsActivity.this, R.style.MyDialogTheme,
                    (view1, y, m, d) -> etStart.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            String startDate = etStart.getText().toString().trim();
            if (startDate.isEmpty()) startDate = today;

            sheet.dismiss();

            // Зберігаємо в Playing
            saveGameToCategory("playing", null, null, null, null, startDate,
                    null, null, null, null, null);
        });

        sheet.setContentView(view);
        sheet.show();
    }
    private void saveGameToCategory(
            String catName,
            Integer userRating,
            String dateStartCompleted,
            String dateEndCompleted,
            String dateAddedPlanned,
            String dateStartPlaying,
            String review,
            Integer priority,
            String type,
            Integer plays,
            Float time
    ) {
        if (game == null) return;

        btnPlanned.setEnabled(false);
        btnPlaying.setEnabled(false);
        btnCompleted.setEnabled(false);

        // Опис тепер залишається чистим і оригінальним!
        String finalDesc = game.getDescription() != null ? game.getDescription() : "";

        // Встановлюємо оцінку користувача як основний рейтинг, якщо вона вказана
        final float finalRating = (userRating != null && userRating > 0) ? (float) userRating : game.getRating();

        new Thread(() -> {
            String localImagePath = game.getImagePath();
            if (localImagePath != null && localImagePath.startsWith("http")) {
                localImagePath = downloadAndCompressImageWithBackoff(localImagePath);
            }

            // Виклик твого нового конструктора
            Game finalGame = new Game(
                    game.getId(),
                    game.getName(),
                    catName,
                    finalDesc,
                    finalRating,
                    localImagePath,
                    game.getTags(),
                    game.getGenres(),
                    game.getScreenshots(),
                    game.getReleased(),
                    game.getHltb(),
                    game.getSteamUrl(),
                    game.getLanguages(),
                    game.getSimilarGames(),
                    game.getCollection(),
                    game.getPlatforms(),
                    game.getAggregatedRating(),
                    game.getStoryline(),
                    game.getIgdbUrl(),
                    game.getGameCategory(),
                    game.getXboxUrl(),
                    game.getPsUrl(),
                    game.getNintendoUrl(),
                    game.getSeriesGames(),
                    mainImageUrl,
                    // --- ТВОЇ НОВІ ПОЛЯ ---
                    userRating,
                    dateStartCompleted,
                    dateEndCompleted,
                    dateAddedPlanned,
                    dateStartPlaying,
                    review,
                    priority,
                    type,
                    plays,
                    time
            );

            finalGame.setRawgSlug(game.getRawgSlug());

            long newDbId = db.addGame(finalGame);
            boolean success = newDbId != -1;

            runOnUiThread(() -> {
                if (success) {
                    Intent intent = new Intent(GameDetailsActivity.this, MyGameDetailsActivity.class);
                    intent.putExtra("gameId", game.getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Error saving game", Toast.LENGTH_SHORT).show();
                    btnPlanned.setEnabled(true);
                    btnPlaying.setEnabled(true);
                    btnCompleted.setEnabled(true);
                }
            });
        }).start();
    }
    private String downloadAndCompressImageWithBackoff(String imageUrl) {
        File dir = new File(getFilesDir(), "game_images");
        if (!dir.exists()) dir.mkdirs();

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Bitmap bitmap = Glide.with(this).asBitmap().load(imageUrl).submit().get();
                if (bitmap == null) continue;

                File file = new File(dir, "img_" + System.currentTimeMillis() + ".webp");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 70, out);
                }
                return file.getAbsolutePath();
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }
    private String getHighResUrl(String url, String sizeTag) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("//")) url = "https:" + url;
        return url.replaceAll("t_\\w+", sizeTag);
    }
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}