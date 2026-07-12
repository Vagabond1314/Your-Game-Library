package com.your_game_library;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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

public class MyGameDetailsActivity extends AppCompatActivity {
    private boolean isFetching = false; // Захист від повторних запитів
    private ImageView detailsImage;
    private TextView detailsTitle, detailsCategory, detailsRating, detailsDescription,
            detailsReleased, detailsHltb, detailsAggregatedRating, detailsStoryline, summaryHeader;
    private View storylineHeader;
    private ChipGroup genresChipGroup, tagsChipGroup, platformsChipGroup,
            languagesChipGroup, similarGamesChipGroup;
    private ViewPager2 screenshotsViewPager;
    private Button btnEditGame, btnOpenIgdb, btnOpenSteam;
    private TextView tvStorylineHint;

    private IgdbApiService igdbApi;
    String CLIENT_ID = Config.IGDB_CLIENT_ID;
    String CLIENT_SECRET = Config.IGDB_CLIENT_SECRET;
    private boolean isTagsExpanded = false; // Чи розгорнуті теги
    private boolean isSimilarExpanded = false;
    private boolean isSeriesExpanded = false;
    private boolean isPlatformsExpanded = false;
    private boolean isLanguagesExpanded = false;
    private boolean isStorylineExpanded = false;
    private boolean isSummaryLineExpanded = false;
    private Button btnChangeStatus, btnEditCompletionStats, btnEditPlannedStats, btnEditPlayingStats;
    private final int CHIPS_LIMIT = 5; // Ліміт для всіх груп
    private boolean isReviewExpanded = false;
    private GameDetailsActivity gameDetailsActivity;

    private Game game;
    private GameDatabaseHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_game_details);

        initViews();
        dbHelper = GameDatabaseHelper.getInstance(this);

        // Налаштування Retrofit
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("https://api.igdb.com/")
                .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        igdbApi = retrofit.create(IgdbApiService.class);

        int gameId = getIntent().getIntExtra("gameId", -1);
        if (gameId == -1) {
            Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        game = dbHelper.getGameByIdObject(gameId);
        if (game == null) {
            Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateDetails();
        setupClickListeners();
    }

    private void initViews() {
        detailsImage = findViewById(R.id.detailsImage);
        detailsTitle = findViewById(R.id.detailsTitle);
        detailsCategory = findViewById(R.id.detailsCategory);
        detailsRating = findViewById(R.id.detailsRating);
        detailsAggregatedRating = findViewById(R.id.detailsAggregatedRating);
        detailsDescription = findViewById(R.id.detailsDescription);
        summaryHeader = findViewById(R.id.summaryHeader);
        detailsReleased = findViewById(R.id.detailsReleased);
        //detailsHltb = findViewById(R.id.detailsHltb);
        detailsStoryline = findViewById(R.id.detailsStoryline);
        storylineHeader = findViewById(R.id.storylineHeader);
        tvStorylineHint = findViewById(R.id.tvStorylineClickHint);

        genresChipGroup = findViewById(R.id.genresChipGroup);
        tagsChipGroup = findViewById(R.id.tagsChipGroup);
        platformsChipGroup = findViewById(R.id.platformsChipGroup);
        languagesChipGroup = findViewById(R.id.languagesChipGroup);

        btnOpenIgdb = findViewById(R.id.btnOpenIgdb);
        btnOpenSteam = findViewById(R.id.btnOpenSteam);
        screenshotsViewPager = findViewById(R.id.screenshotsViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_edit) {
            // Логіка переходу на редагування (те, що було на btnEditGame)
            openEditActivity();
            return true;
        } else if (id == R.id.action_update_igdb) {
            // Твій існуючий метод для оновлення даних
            fetchMissingDataFromIgdb();
            Toast.makeText(this, "Updating data...", Toast.LENGTH_SHORT).show();
            return true;
        }else if (id == R.id.action_delete) {
            dbHelper.deleteGame(game.getId());
            Toast.makeText(this, "Game deleted", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Допоміжний метод (перенеси сюди код з btnEditGame.setOnClickListener)
    private void openEditActivity() {
        Intent intent = new Intent(this, AddGameActivity.class);
        intent.putExtra("gameId", game.getId());
        intent.putStringArrayListExtra("screenshots", new ArrayList<>(game.getScreenshots()));
        intent.putStringArrayListExtra("tags", new ArrayList<>(game.getTags()));
        intent.putStringArrayListExtra("genres", new ArrayList<>(game.getGenres()));
        startActivity(intent);
    }

    private void setupClickListeners() {
//        btnEditGame.setOnClickListener(v -> {
//            Intent intent = new Intent(this, AddGameActivity.class);
//            intent.putExtra("gameId", game.getId());
//            intent.putStringArrayListExtra("screenshots", new ArrayList<>(game.getScreenshots()));
//            intent.putStringArrayListExtra("tags", new ArrayList<>(game.getTags()));
//            intent.putStringArrayListExtra("genres", new ArrayList<>(game.getGenres()));
//            startActivity(intent);
//        });
        // Ініціалізація кнопки копіювання
//        View btnCopy = findViewById(R.id.btnCopyTitle);
//        if (btnCopy != null) {
//            btnCopy.setOnClickListener(v -> {
//                String gameName = detailsTitle.getText().toString();
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
        if (btnOpenSteam != null) {
            btnOpenSteam.setOnClickListener(v -> {
                if (game.getSteamUrl() != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(game.getSteamUrl())));
                }
            });
        }
        // Клік: Зміна статусу гри
        if (btnChangeStatus != null) {
            btnChangeStatus.setOnClickListener(v -> showChangeStatusDialog());
        }

// --- 1. Клік по статусу (відкриття вибору нової категорії) ---
        View llStatusClickable = findViewById(R.id.llStatusClickable);
        if (llStatusClickable != null) {
            // Звичайний клік (бо це просто текст, випадково не натиснеш)
            llStatusClickable.setOnClickListener(v -> showChangeStatusDialog());
        }

        // --- 2. Довгі натискання по плашках статистики (для редагування) ---
        View userStatsContainer = findViewById(R.id.userStatsContainer);
        View userStatsPlannedContainer = findViewById(R.id.userStatsPlannedContainer);
        View userStatsPlayingContainer = findViewById(R.id.userStatsPlayingContainer);

        // Редагування Completed Stats
        if (userStatsContainer != null) {
            userStatsContainer.setOnLongClickListener(v -> {
                showCompletedBottomSheet();
                // Легка вібрація для фідбеку
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                return true; // true означає, що ми обробили подію і вона не піде далі
            });
        }

        // Редагування Planned Stats
        if (userStatsPlannedContainer != null) {
            userStatsPlannedContainer.setOnLongClickListener(v -> {
                showPlannedBottomSheet();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                return true;
            });
        }

        // Редагування Playing Stats
        if (userStatsPlayingContainer != null) {
            userStatsPlayingContainer.setOnLongClickListener(v -> {
                showPlayingBottomSheet();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                return true;
            });
        }
    }
    // 1. ДІАЛОГ ЗМІНИ СТАТУСУ ГРИ
    private void showChangeStatusDialog() {
        String[] categories = {"planned", "playing", "completed"};
        int checkedItem = java.util.Arrays.asList(categories).indexOf(game.getCategory().toLowerCase());
        int colorGreen = android.graphics.Color.parseColor("#58A870");

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Change Game Status")
                .setSingleChoiceItems(new String[]{"Planned", "Playing", "Completed"}, checkedItem, (d, which) -> {
                    String newCat = categories[which];
                    d.dismiss();

                    // Якщо користувач вибрав ту саму категорію, що й була, нічого не робимо
                    if (newCat.equalsIgnoreCase(game.getCategory())) {
                        return;
                    }

                    // Викликаємо відповідний Bottom Sheet, який сам збереже все в БД
                    if (newCat.equals("completed")) {
                        showCompletedBottomSheet();
                    } else if (newCat.equals("playing")) {
                        showPlayingBottomSheet();
                    } else if (newCat.equals("planned")) {
                        showPlannedBottomSheet();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colorGreen);
    }
    // Метод для очищення опису від старих маркерів під час переходу на нові поля
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
    // 1. ФОРМА ЗАПОВНЕННЯ СТАТИСТИКИ ПРОХОДЖЕННЯ (Completed)
    private void showCompletedBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_completed_sheet, null);

        // UI Елементи
        LinearLayout llRatingContainer = view.findViewById(R.id.llRatingContainer);
        com.google.android.material.textfield.TextInputEditText etTime = view.findViewById(R.id.etTimeSpent);
        com.google.android.material.textfield.TextInputEditText etPlays = view.findViewById(R.id.etPlayCount);
        com.google.android.material.textfield.TextInputEditText etStart = view.findViewById(R.id.etStartDate);
        com.google.android.material.textfield.TextInputEditText etEnd = view.findViewById(R.id.etEndDate);
        com.google.android.material.textfield.TextInputEditText etReview = view.findViewById(R.id.etReview);
        Spinner spinnerType = view.findViewById(R.id.spinnerCompletionType);
        Button btnSave = view.findViewById(R.id.btnSaveCompletedStats);

        // ОТКЛЮЧАЄМО КЛАВІАТУРУ ДЛЯ ДАТ
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

        String oldTime = "", oldPlays = "1", oldStart = "", oldEnd = "", oldReview = "";
        int oldTypePos = 0;

        // --- 1. ЧИТАННЯ ДАНИХ COMPLETED (Нові поля -> Фолбек опис) ---
        String fullDesc = game.getDescription() != null ? game.getDescription() : "";
        String statsMarkerStart = "🏆 --- COMPLETION STATS --- 🏆";
        String statsMarkerEnd = "-----------------------------";

        String cleanDescBefore = fullDesc;

        if (game.getDateEndCompleted() != null && !game.getDateEndCompleted().isEmpty()) {
            Float time = game.getTime();
            time = (float) Math.round(time * 100) / 100;
            oldTime = game.getTime() != null ? String.valueOf(time) : "";
            oldPlays = game.getPlays() != null ? String.valueOf(game.getPlays()) : "1";
            oldStart = game.getDateStartCompleted() != null ? game.getDateStartCompleted() : "";
            oldEnd = game.getDateEndCompleted();
            oldReview = game.getReview() != null ? game.getReview() : "";
            String tStr = game.getType() != null ? game.getType() : "";
            if (tStr.equals("Main + Extras")) oldTypePos = 1;
            else if (tStr.equals("100% Completion")) oldTypePos = 2;
        } else if (fullDesc.contains(statsMarkerStart)) {
            try {
                int startIdx = fullDesc.indexOf(statsMarkerStart);
                int endIdx = fullDesc.indexOf(statsMarkerEnd, startIdx) + statsMarkerEnd.length();

                if (startIdx >= 0 && endIdx > startIdx) {
                    String statsBlock = fullDesc.substring(startIdx, endIdx);
                    cleanDescBefore = fullDesc.substring(0, startIdx) + fullDesc.substring(endIdx);

                    String[] lines = statsBlock.split("\n");
                    boolean isReviewLine = false;
                    for (String line : lines) {
                        if (line.trim().startsWith("-----------------------------")) break;

                        if (line.startsWith("Time Spent:")) oldTime = line.replace("Time Spent:", "").replace("hours", "").trim();
                        else if (line.startsWith("Playthroughs:")) oldPlays = line.replace("Playthroughs:", "").trim();
                        else if (line.startsWith("Start Date:")) oldStart = line.replace("Start Date:", "").trim();
                        else if (line.startsWith("End Date:")) oldEnd = line.replace("End Date:", "").trim();
                        else if (line.startsWith("Type:")) {
                            String tStr = line.replace("Type:", "").trim();
                            if (tStr.equals("Main + Extras")) oldTypePos = 1;
                            else if (tStr.equals("100% Completion")) oldTypePos = 2;
                        }
                        else if (line.startsWith("Review:")) {
                            isReviewLine = true;
                            oldReview = line.replace("Review:", "").trim();
                        } else if (isReviewLine) {
                            oldReview += "\n" + line;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // --- 2. МАГІЯ: Перехід з PLAYING в COMPLETED ---
        // Якщо дати початку Completed немає, копіюємо її з дати старту Playing!
            if (game.getDateStartPlaying() != null && !game.getDateStartPlaying().isEmpty()) {
                oldStart = game.getDateStartPlaying();
            } else {
                // Фолбек для старих збережень Playing
                String playingMarkerStart = "🎮 --- PLAYING STATS --- 🎮";
                if (fullDesc.contains(playingMarkerStart)) {
                    try {
                        int start = fullDesc.indexOf(playingMarkerStart);
                        int end = fullDesc.indexOf(statsMarkerEnd, start) + statsMarkerEnd.length();
                        String statsBlock = fullDesc.substring(start, end);
                        for (String line : statsBlock.split("\n")) {
                            if (line.startsWith("Started playing:")) {
                                oldStart = line.replace("Started playing:", "").trim();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

        // ПІДСТАНОВКА В UI
        etTime.setText(oldTime);
        etPlays.setText(oldPlays);
        etStart.setText(oldStart);
        etEnd.setText(oldEnd.isEmpty() ? today : oldEnd);
        etReview.setText(oldReview);
        spinnerType.setSelection(oldTypePos);

        // Календарі
        etStart.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            try {
                String currentStart = etStart.getText().toString();
                if (!currentStart.isEmpty()) c.setTime(sdf.parse(currentStart));
            } catch (Exception ignored) {}
            new android.app.DatePickerDialog(MyGameDetailsActivity.this, R.style.MyDialogTheme,
                    (view1, y, m, d) -> etStart.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        etEnd.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            try {
                String currentEnd = etEnd.getText().toString();
                if (!currentEnd.isEmpty()) c.setTime(sdf.parse(currentEnd));
            } catch (Exception ignored) {}
            new android.app.DatePickerDialog(MyGameDetailsActivity.this, R.style.MyDialogTheme,
                    (view12, y, m, d) -> etEnd.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // Рулетка оцінок
        String[] ratingValues = {"None", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        // Безпечна null-перевірка для оцінки користувача
        float initialRate = (game.getUserRating() != null && game.getUserRating() > 0) ? game.getUserRating().floatValue() : game.getRating();
        final float[] selectedRating = {initialRate};
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

            if ((val.equals("None") && selectedRating[0] <= 0) || (selectedRating[0] > 0 && val.equals(String.valueOf((int)selectedRating[0])))) {
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

        final String finalCleanDescBefore = cleanDescBefore;

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

            Integer userRatingInt = selectedRating[0] > 0 ? Math.round(selectedRating[0]) : null;
            Float timeInt = null;
            try { String t = etTime.getText().toString().trim(); if (!t.isEmpty()) timeInt = Float.parseFloat(t); } catch (Exception ignored) {}
            Integer playsInt = null;
            try { String p = etPlays.getText().toString().trim(); if (!p.isEmpty()) playsInt = Integer.parseInt(p); } catch (Exception ignored) {}
            String reviewText = etReview.getText().toString().trim();

            // Вирізаємо ЛИШЕ старий блок Completed з опису (Planned та Playing залишаються недоторканими!)
            String cleanDesc = game.getDescription() != null ? game.getDescription() : "";
            cleanDesc = removeBlock(cleanDesc, "🏆 --- COMPLETION STATS --- 🏆", "-----------------------------");

            // Оновлюємо помаранчеві поля Completed
            game.setCategory("completed");
            game.setRating(userRatingInt != null ? userRatingInt.floatValue() : game.getRating());
            game.setUserRating(userRatingInt);
            game.setDateStartCompleted(startD);
            game.setDateEndCompleted(endD);
            game.setTime(timeInt);
            game.setPlays(playsInt);
            game.setReview(reviewText.isEmpty() ? null : reviewText);
            game.setType(spinnerType.getSelectedItem().toString());
            game.setDescription(cleanDesc.trim());
            // (Нічого іншого не занулюємо, всі інші статки зберігаються!)

            sheet.dismiss();

            new Thread(() -> {
                dbHelper.updateGame(game);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Stats updated successfully!", Toast.LENGTH_SHORT).show();
                    populateDetails();
                });
            }).start();
        });

        sheet.setContentView(view);
        sheet.show();
    }
    // --- ДІАЛОГ ДЛЯ ЗАПЛАНОВАНИХ ІГОР (Priority) ---
    private void showPlannedBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_planned_sheet, null);

        LinearLayout llRatingContainer = view.findViewById(R.id.PriorityContainer);
        Button btnSave = view.findViewById(R.id.btnSavePlannedGames);

        int oldPriority = 0;
        String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        if(game.getDateAddedPlanned() != null){
            today = game.getDateAddedPlanned();
        }

        // Читаємо пріоритет (нові поля -> фолбек опис)
        if (game.getPriority() != null) {
            oldPriority = game.getPriority();
        } else {
            String fullDesc = game.getDescription() != null ? game.getDescription() : "";
            if (fullDesc.contains("🎯 --- PLANNED STATS --- 🎯")) {
                try {
                    int start = fullDesc.indexOf("🎯 --- PLANNED STATS --- 🎯");
                    int end = fullDesc.indexOf("-----------------------------", start) + "-----------------------------".length();
                    String statsBlock = fullDesc.substring(start, end);
                    for (String line : statsBlock.split("\n")) {
                        if (line.startsWith("Priority: ")) {
                            oldPriority = Integer.parseInt(line.replace("Priority: ", "").trim());
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        String[] ratingValues = {"None", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        final float[] selectedRating = {(float) oldPriority};
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

            if ((val.equals("None") && selectedRating[0] <= 0) || val.equals(String.valueOf((int)selectedRating[0]))) {
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

        final String finalCleanDescBefore = game.getDescription() != null ? game.getDescription() : "";

        String finalToday = today;
        btnSave.setOnClickListener(v -> {
            Integer priorityInt = selectedRating[0] > 0 ? Math.round(selectedRating[0]) : null;

            // Вирізаємо ЛИШЕ старий блок Planned з тексту
            String cleanDesc = removeBlock(finalCleanDescBefore, "🎯 --- PLANNED STATS --- 🎯", "-----------------------------");

            // Оновлюємо сині поля Planned
            game.setCategory("planned");
            game.setPriority(priorityInt);
            game.setDateAddedPlanned(finalToday);
            game.setDescription(cleanDesc.trim());
            // (Нічого іншого не занулюємо!)

            sheet.dismiss();

            new Thread(() -> {
                dbHelper.updateGame(game);
                runOnUiThread(() -> {
                    populateDetails();
                });
            }).start();
        });

        sheet.setContentView(view);
        sheet.show();
    }
    // --- ДІАЛОГ ДЛЯ ІГОР, В ЯКІ ГРАЄМО (Playing) ---
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

        String oldStart = today;

        // Читаємо дату початку (нові поля -> фолбек опис)
        if (game.getDateStartPlaying() != null && !game.getDateStartPlaying().isEmpty()) {
            oldStart = game.getDateStartPlaying();
        } else {
            String fullDesc = game.getDescription() != null ? game.getDescription() : "";
            if (fullDesc.contains("🎮 --- PLAYING STATS --- 🎮")) {
                try {
                    int start = fullDesc.indexOf("🎮 --- PLAYING STATS --- 🎮");
                    int end = fullDesc.indexOf("-----------------------------", start) + "-----------------------------".length();
                    String statsBlock = fullDesc.substring(start, end);
                    for (String line : statsBlock.split("\n")) {
                        if (line.startsWith("Started playing: ")) {
                            oldStart = line.replace("Started playing: ", "").trim();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        etStart.setText(oldStart);

        etStart.setOnClickListener(v -> {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            try {
                c.setTime(sdf.parse(etStart.getText().toString()));
            } catch (Exception ignored) {}
            new android.app.DatePickerDialog(MyGameDetailsActivity.this, R.style.MyDialogTheme,
                    (view1, y, m, d) -> etStart.setText(String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        final String finalCleanDescBefore = game.getDescription() != null ? game.getDescription() : "";

        btnSave.setOnClickListener(v -> {
            String startDate = etStart.getText().toString().trim();
            if (startDate.isEmpty()) startDate = today;

            // Вирізаємо ЛИШЕ старий блок Playing з тексту
            String cleanDesc = removeBlock(finalCleanDescBefore, "🎮 --- PLAYING STATS --- 🎮", "-----------------------------");

            // Оновлюємо зелені поля Playing
            game.setCategory("playing");
            game.setDateStartPlaying(startDate);
            game.setDescription(cleanDesc.trim());
            // (Нічого іншого не занулюємо!)

            sheet.dismiss();

            new Thread(() -> {
                dbHelper.updateGame(game);
                runOnUiThread(() -> {
                    populateDetails();
                });
            }).start();
        });

        sheet.setContentView(view);
        sheet.show();
    }

    // 3. КЛАС-МАСКА ДЛЯ ДАТИ (dd.MM.yyyy)
    private class SimpleDateWatcher implements android.text.TextWatcher {
        private String current = "";
        private android.widget.EditText input;

        public SimpleDateWatcher(android.widget.EditText input) {
            this.input = input;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().equals(current)) return;

            String clean = s.toString().replaceAll("[^\\d]", "");
            String cleanC = current.replaceAll("[^\\d]", "");

            int cl = clean.length();
            int sel = cl;
            for (int i = 2; i <= cl && i < 6; i += 2) {
                sel++;
            }
            if (clean.equals(cleanC)) sel--;

            if (clean.length() < 8) {
                String formatted = "";
                for (int i = 0; i < clean.length(); i++) {
                    if (i == 2 || i == 4) formatted += ".";
                    formatted += clean.charAt(i);
                }
                clean = formatted;
            } else {
                int day = Integer.parseInt(clean.substring(0, 2));
                int mon = Integer.parseInt(clean.substring(2, 4));
                int year = Integer.parseInt(clean.substring(4, 8));

                mon = mon < 1 ? 1 : Math.min(mon, 12);
                year = (year < 1900) ? 1900 : Math.min(year, 2100);

                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.MONTH, mon - 1);
                cal.set(java.util.Calendar.YEAR, year);
                day = Math.min(day, cal.getActualMaximum(java.util.Calendar.DATE));

                clean = String.format(Locale.getDefault(), "%02d.%02d.%04d", day, mon, year);
            }

            current = clean;
            input.setText(current);
            input.setSelection(Math.min(sel, current.length()));
        }

        @Override public void afterTextChanged(android.text.Editable s) {}
    }
    private void populateDetails() {
        if (isDestroyed() || isFinishing()) return;

        // --- 1. БАЗОВА ІНФОРМАЦІЯ ---
        detailsTitle.setText(game.getName());
        detailsCategory.setText(game.getCategory());
        if(game.getCategory().equals("completed")) detailsCategory.setText("BEATEN");
        if(game.getCategory().equals("planned")) detailsCategory.setTextColor(Color.parseColor("#2D5E85"));
        if(game.getCategory().equals("playing")) detailsCategory.setTextColor(Color.parseColor("#58A870"));
        if(game.getCategory().equals("completed")) detailsCategory.setTextColor(Color.parseColor("#fc6f03"));
        //detailsCategory.setTextColor(Color.parseColor("#2D5E85"));
        detailsReleased.setText(game.getReleased());

        Button btnManageCols = findViewById(R.id.btnManageCollections);
        btnManageCols.setVisibility(View.VISIBLE);
        btnManageCols.setOnClickListener(v -> showCollectionSelector());

        detailsRating.setText(game.getRating() > 0 ? String.format(Locale.ROOT, "%.1f", game.getRating()) : "N/A");
        detailsAggregatedRating.setText(game.getAggregatedRating() > 0 ? String.format(Locale.ROOT, "%.1f", game.getAggregatedRating()) : "N/A");

        // --- 2. ПАРСИНГ ПЕРСОНАЛЬНОЇ СТАТИСТИКИ З ОПИСУ ТА HLTB ---
        String fullDesc = game.getDescription() != null ? game.getDescription() : "";
        String cleanDescription = fullDesc;

        // Маркери
        String statsMarkerEnd = "-----------------------------";
        String plannedMarkerStart = "🎯 --- PLANNED STATS --- 🎯";
        String playingMarkerStart = "🎮 --- PLAYING STATS --- 🎮";
        String compMarkerStart = "🏆 --- COMPLETION STATS --- 🏆";

        // Блоки для зберігання знайденого тексту
        String plannedBlock = "";
        String playingBlock = "";
        String compBlock = "";

        // 2.1 ВИРІЗАЄМО ВСІ БЛОКИ З ОПИСУ (щоб сховати їх від користувача)
        if (cleanDescription.contains(plannedMarkerStart)) {
            int start = cleanDescription.indexOf(plannedMarkerStart);
            int end = cleanDescription.indexOf(statsMarkerEnd, start) + statsMarkerEnd.length();
            if (start >= 0 && end > start) {
                plannedBlock = cleanDescription.substring(start, end);
                cleanDescription = cleanDescription.substring(0, start) + cleanDescription.substring(end);
            }
        }
        if (cleanDescription.contains(playingMarkerStart)) {
            int start = cleanDescription.indexOf(playingMarkerStart);
            int end = cleanDescription.indexOf(statsMarkerEnd, start) + statsMarkerEnd.length();
            if (start >= 0 && end > start) {
                playingBlock = cleanDescription.substring(start, end);
                cleanDescription = cleanDescription.substring(0, start) + cleanDescription.substring(end);
            }
        }
        if (cleanDescription.contains(compMarkerStart)) {
            int start = cleanDescription.indexOf(compMarkerStart);
            int end = cleanDescription.indexOf(statsMarkerEnd, start) + statsMarkerEnd.length();
            if (start >= 0 && end > start) {
                compBlock = cleanDescription.substring(start, end);
                cleanDescription = cleanDescription.substring(0, start) + cleanDescription.substring(end);
            }
        }

        // Знаходимо UI контейнери
        View userStatsPlannedContainer = findViewById(R.id.userStatsPlannedContainer);
        View userStatsPlayingContainer = findViewById(R.id.userStatsPlayingContainer);
        View userStatsContainer = findViewById(R.id.userStatsContainer); // Це для Completed
        View hltbContainer = findViewById(R.id.hltbContainer);

        // ХОВАЄМО ВСІ ПЛАШКИ ТА КНОПКИ ЗА ЗАМОВЧУВАННЯМ
        if(userStatsPlannedContainer != null) userStatsPlannedContainer.setVisibility(View.GONE);
        if(userStatsPlayingContainer != null) userStatsPlayingContainer.setVisibility(View.GONE);
        if(userStatsContainer != null) userStatsContainer.setVisibility(View.GONE);

        // ХОВАЄМО ВСІ КНОПКИ РЕДАГУВАННЯ СТАТИСТИКИ
        if(btnEditPlannedStats != null) btnEditPlannedStats.setVisibility(View.GONE);
        if(btnEditPlayingStats != null) btnEditPlayingStats.setVisibility(View.GONE); // Якщо є така кнопка
        if(btnEditCompletionStats != null) btnEditCompletionStats.setVisibility(View.GONE);
        // 2.2 ВІДОБРАЖЕННЯ ПЛАШОК ЗАЛЕЖНО ВІД ПОТОЧНОЇ КАТЕГОРІЇ
        // --- 2.2 ВІДОБРАЖЕННЯ ПЛАШОК ЗАЛЕЖНО ВІД ПОТОЧНОЇ КАТЕГОРІЇ ---

        // --- ЛОГІКА ДЛЯ PLANNED ---
        if ("planned".equalsIgnoreCase(game.getCategory())) {
            if (btnEditPlannedStats != null) btnEditPlannedStats.setVisibility(View.VISIBLE);

            String uPrio = "-";
            String uAdded = "?";

            // Перевіряємо, чи є дані в нових типізованих полях
            if (game.getDateAddedPlanned() != null && !game.getDateAddedPlanned().isEmpty()) {
                if (userStatsPlannedContainer != null) userStatsPlannedContainer.setVisibility(View.VISIBLE);
                uPrio = game.getPriority() != null ? String.valueOf(game.getPriority()) : "-";
                uAdded = game.getDateAddedPlanned();
            }
            // Фолбек: якщо нові поля порожні, читаємо зі старого текстового блоку в описі
            else if (!plannedBlock.isEmpty()) {
                if (userStatsPlannedContainer != null) userStatsPlannedContainer.setVisibility(View.VISIBLE);
                for (String line : plannedBlock.split("\n")) {
                    if (line.startsWith("Priority:")) uPrio = line.replace("Priority:", "").trim();
                    else if (line.startsWith("Date added on:")) uAdded = line.replace("Date added on:", "").trim();
                    else if (line.startsWith("Added to Planned:")) uAdded = line.replace("Added to Planned:", "").trim();
                }

                if (uAdded.equals("?") && cleanDescription.contains("Added on: ")) {
                    int addIdx = cleanDescription.indexOf("Added on: ");
                    uAdded = cleanDescription.substring(addIdx + 10, addIdx + 20).trim();
                }
            } else {
                // Старі збереження взагалі без блоків
                uAdded = "?";
                if (cleanDescription.contains("Added on: ")) {
                    int addIdx = cleanDescription.indexOf("Added on: ");
                    uAdded = cleanDescription.substring(addIdx + 10, addIdx + 20).trim();
                }
                if (!uAdded.equals("?")) {
                    if (userStatsPlannedContainer != null) userStatsPlannedContainer.setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.userPriority)).setText("-");
                    ((TextView) findViewById(R.id.tvUserAdded)).setText(uAdded);
                } else {
                    if (userStatsPlannedContainer != null) userStatsPlannedContainer.setVisibility(View.GONE);
                }
            }

            if (userStatsPlannedContainer != null && userStatsPlannedContainer.getVisibility() == View.VISIBLE) {
                ((TextView) findViewById(R.id.userPriority)).setText(uPrio);
                ((TextView) findViewById(R.id.tvUserAdded)).setText(uAdded);
            }
        }

        // --- ЛОГІКА ДЛЯ PLAYING ---
        else if ("playing".equalsIgnoreCase(game.getCategory())) {
            if (btnEditPlayingStats != null) btnEditPlayingStats.setVisibility(View.VISIBLE);

            String uStarted = "?";

            // Перевіряємо нові поля
            if (game.getDateStartPlaying() != null && !game.getDateStartPlaying().isEmpty()) {
                if (userStatsPlayingContainer != null) userStatsPlayingContainer.setVisibility(View.VISIBLE);
                uStarted = game.getDateStartPlaying();
            }
            // Фолбек на текстовий блок
            else if (!playingBlock.isEmpty()) {
                if (userStatsPlayingContainer != null) userStatsPlayingContainer.setVisibility(View.VISIBLE);
                for (String line : playingBlock.split("\n")) {
                    if (line.startsWith("Started playing:")) uStarted = line.replace("Started playing:", "").trim();
                }
            } else {
                if (userStatsPlayingContainer != null) userStatsPlayingContainer.setVisibility(View.GONE);
            }

            if (userStatsPlayingContainer != null && userStatsPlayingContainer.getVisibility() == View.VISIBLE) {
                ((TextView) findViewById(R.id.tvUserStarted)).setText(uStarted);
            }
        }

        // --- ЛОГІКА ДЛЯ COMPLETED ---
        else if ("completed".equalsIgnoreCase(game.getCategory())) {
            if (btnEditCompletionStats != null) btnEditCompletionStats.setVisibility(View.VISIBLE);

            String uTime = "-";
            String uPlays = "-";
            String uType = "-";
            String uStart = "?";
            String uEnd = "?";
            String uReview = "";

            // Перевіряємо нові поля
            if (game.getDateEndCompleted() != null && !game.getDateEndCompleted().isEmpty()) {
                if (userStatsContainer != null) userStatsContainer.setVisibility(View.VISIBLE);
                Float time = game.getTime();
                time = (float) Math.round(time * 100) / 100;
                uTime = game.getTime() != null ? String.valueOf(time) + "h" : "-";
                uPlays = game.getPlays() != null ? String.valueOf(game.getPlays()) : "-";
                uType = game.getType() != null ? game.getType() : "-";
                uStart = game.getDateStartCompleted() != null ? game.getDateStartCompleted() : "?";
                uEnd = game.getDateEndCompleted();
                uReview = game.getReview() != null ? game.getReview() : "";
            }
            // Фолбек на текстовий блок
            else if (!compBlock.isEmpty()) {
                if (userStatsContainer != null) userStatsContainer.setVisibility(View.VISIBLE);
                boolean isReviewLine = false;

                for (String line : compBlock.split("\n")) {
                    if (line.trim().startsWith("-----------------------------")) {
                        break;
                    }
                    if (line.startsWith("Time Spent:")) uTime = line.replace("Time Spent:", "").trim();
                    else if (line.startsWith("Playthroughs:")) uPlays = line.replace("Playthroughs:", "").trim();
                    else if (line.startsWith("Start Date:")) uStart = line.replace("Start Date:", "").trim();
                    else if (line.startsWith("End Date:")) uEnd = line.replace("End Date:", "").trim();
                    else if (line.startsWith("Date:")) uEnd = line.replace("Date:", "").trim();
                    else if (line.startsWith("Type:")) uType = line.replace("Type:", "").trim();
                    else if (line.startsWith("Review:")) {
                        isReviewLine = true;
                        uReview = line.replace("Review:", "").trim();
                    } else if (isReviewLine) {
                        uReview += "\n" + line;
                    }
                }
            } else {
                if (userStatsContainer != null) userStatsContainer.setVisibility(View.GONE);
            }

            // Заповнюємо UI Completed даними
            if (userStatsContainer != null && userStatsContainer.getVisibility() == View.VISIBLE) {
                ((TextView) findViewById(R.id.tvUserDates)).setText(uStart + "\n↓\n" + uEnd);

                // Якщо є години проходження, дописуємо в кінець "h"
                ((TextView) findViewById(R.id.tvUserTime)).setText(uTime.equals("-") ? "-" : uTime);
                ((TextView) findViewById(R.id.tvUserPlays)).setText(uPlays);
                ((TextView) findViewById(R.id.tvUserType)).setText(uType);

                // --- ЛОГІКА ВІДГУКУ (Show More) ---
                View reviewContainer = findViewById(R.id.reviewContainer);
                TextView tvUserReview = findViewById(R.id.tvUserReview);
                TextView tvReviewHint = findViewById(R.id.tvReviewClickHint);

                if (uReview.isEmpty()) {
                    if (reviewContainer != null) reviewContainer.setVisibility(View.GONE);
                } else {
                    if (reviewContainer != null) reviewContainer.setVisibility(View.VISIBLE);
                    tvUserReview.setText("\"" + uReview + "\"");

                    tvUserReview.post(() -> {
                        if (tvUserReview.getLineCount() >= 3) {
                            tvReviewHint.setVisibility(View.VISIBLE);
                            tvReviewHint.setText(isReviewExpanded ? "Show less" : "Show more...");
                        } else {
                            tvReviewHint.setVisibility(View.GONE);
                        }
                    });

                    View.OnClickListener toggleReview = v -> {
                        isReviewExpanded = !isReviewExpanded;
                        tvUserReview.setMaxLines(isReviewExpanded ? Integer.MAX_VALUE : 3);
                        tvReviewHint.setText(isReviewExpanded ? "Show less" : "Show more...");
                    };

                    tvUserReview.setOnClickListener(toggleReview);
                    tvReviewHint.setOnClickListener(toggleReview);
                }
            }
        }

        // --- 2.3 ЛОГІКА ОФІЦІЙНОГО HLTB ---
        // Показуємо HLTB завжди, ОКРІМ випадку, коли гра Completed і має заповнену плашку
        if ("completed".equalsIgnoreCase(game.getCategory()) && (game.getDateEndCompleted() != null || !compBlock.isEmpty())) {
            if (hltbContainer != null) hltbContainer.setVisibility(View.GONE);
        } else {
            if (game.getHltb() != null && game.getHltb().contains("|")) {
                String[] parts = game.getHltb().split("\\|");
                if (hltbContainer != null && parts.length == 3) {
                    hltbContainer.setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.tvHltbMain)).setText(parts[0]);
                    ((TextView)findViewById(R.id.tvHltbExtras)).setText(parts[1]);
                    ((TextView)findViewById(R.id.tvHltbComplete)).setText(parts[2]);
                }
            } else if (hltbContainer != null) {
                hltbContainer.setVisibility(View.GONE);
            }
        }

        // --- 3. ВІДОБРАЖЕННЯ ОЧИЩЕНОГО ОПИСУ ---
        TextView tvSummaryClickHint = findViewById(R.id.tvSummaryClickHint);

        // Видаляємо рядок "Added on: дата" за допомогою регулярного виразу
        cleanDescription = cleanDescription.replaceAll("Added on: \\d{2}\\.\\d{2}\\.\\d{4}", "").trim();

        if (!cleanDescription.isEmpty()) {
            summaryHeader.setVisibility(View.VISIBLE);
            detailsDescription.setVisibility(View.VISIBLE);
            detailsDescription.setText(cleanDescription);

            detailsDescription.setMaxLines(isSummaryLineExpanded ? Integer.MAX_VALUE : 4);
            detailsDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);

            detailsDescription.post(() -> {
                if (detailsDescription.getLineCount() >= 4) {
                    tvSummaryClickHint.setVisibility(View.VISIBLE);
                    tvSummaryClickHint.setText(isSummaryLineExpanded ? "Show less" : "Read more...");
                } else {
                    tvSummaryClickHint.setVisibility(View.GONE);
                }
            });

            View.OnClickListener toggleSummary = v -> {
                isSummaryLineExpanded = !isSummaryLineExpanded;
                detailsDescription.setMaxLines(isSummaryLineExpanded ? Integer.MAX_VALUE : 4);
                tvSummaryClickHint.setText(isSummaryLineExpanded ? "Show less" : "Read more...");
            };

            detailsDescription.setOnClickListener(toggleSummary);
            tvSummaryClickHint.setOnClickListener(toggleSummary);
        } else {
            summaryHeader.setVisibility(View.GONE);
            detailsDescription.setVisibility(View.GONE);
            tvSummaryClickHint.setVisibility(View.GONE);
        }

        // --- 4. СЮЖЕТ (STORYLINE) ---
        if (game.getStoryline() != null && !game.getStoryline().isEmpty()) {
            storylineHeader.setVisibility(View.VISIBLE);
            detailsStoryline.setVisibility(View.VISIBLE);
            detailsStoryline.setText(game.getStoryline());

            detailsStoryline.setMaxLines(isStorylineExpanded ? Integer.MAX_VALUE : 4);
            detailsStoryline.setEllipsize(android.text.TextUtils.TruncateAt.END);

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

        // --- 5. ЧІПИ (ЖАНРИ, ТЕГИ, ПЛАТФОРМИ, МОВИ) ---
        addChipsToGroup(genresChipGroup, game.getGenres());
        if (game.getTags() != null && !game.getTags().isEmpty()) {
            addExpandableChips(tagsChipGroup, game.getTags(), 0);
        }
        if (game.getPlatforms() != null && !game.getPlatforms().isEmpty()) {
            addExpandableChips(platformsChipGroup, game.getPlatforms(), 4);
        }
        if (game.getLanguages() != null && !game.getLanguages().isEmpty()) {
            addExpandableChips(languagesChipGroup, game.getLanguages(), 3);
        }

        // --- 6. МАГАЗИНИ ---
        setupStoreButtons(game.getSteamUrl(), game.getPsUrl(), game.getXboxUrl(), game.getNintendoUrl());

        // --- 7. ЗОБРАЖЕННЯ ТА СКРІНШОТИ ---
        loadMainImage();
        if (game.getScreenshots() != null && !game.getScreenshots().isEmpty()) {
            ScreenshotsAdapter adapter = new ScreenshotsAdapter(this, game.getScreenshots());
            screenshotsViewPager.setAdapter(adapter);
            screenshotsViewPager.setOffscreenPageLimit(3);
        }

        // --- 8. СХОЖІ ТА СЕРІЙНІ ІГРИ ---
        View btnSimilarGames = findViewById(R.id.btnSimilarGames);
        if (btnSimilarGames != null) {
            if (game.getSimilarGames() != null && !game.getSimilarGames().isEmpty()) {
                btnSimilarGames.setVisibility(View.VISIBLE);
                btnSimilarGames.setOnClickListener(v -> {
                    Intent intent = new Intent(this, SimilarGamesActivity.class);
                    intent.putExtra("GAME_ID", game.getId());
                    intent.putExtra("GAME_NAME", game.getName());
                    startActivity(intent);
                });
            } else {
                btnSimilarGames.setVisibility(View.GONE);
            }
        }

        View btnSeriesGames = findViewById(R.id.btnSeries);
        TextView tvSeriesTitle = findViewById(R.id.tvSeriesTitle);
        if (btnSeriesGames != null) {
            if (game.getSeriesGames() != null && !game.getSeriesGames().isEmpty()) {
                btnSeriesGames.setVisibility(View.VISIBLE);
                tvSeriesTitle.setText(game.getCollection() + " Series");
                btnSeriesGames.setOnClickListener(v -> {
                    Intent intent = new Intent(this, SeriesActivity.class);
                    intent.putExtra("GAME_ID", game.getId());
                    intent.putExtra("GAME_NAME", game.getName());
                    startActivity(intent);
                });
            } else {
                btnSeriesGames.setVisibility(View.GONE);
            }
        }
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
    private void fetchSeriesData(String token, int igdbId) {
        String query = "fields name, games.name, games.id; where games = (" + igdbId + ");";

        igdbApi.getCollections(CLIENT_ID, token, query).enqueue(new Callback<List<IgdbGame.IgdbSeries>>() {
            @Override
            public void onResponse(Call<List<IgdbGame.IgdbSeries>> call, Response<List<IgdbGame.IgdbSeries>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    IgdbGame.IgdbSeries foundSeries = response.body().get(0);

                    List<String> sGames = new ArrayList<>();
                    if (foundSeries.games != null) {
                        for (IgdbGame.GameInCollection gc : foundSeries.games) {
                            if (gc.id != igdbId) { // Виключаємо саму себе
                                sGames.add(gc.name + "|" + gc.id);
                            }
                        }
                    }

                    game.setCollection(foundSeries.name);
                    game.setSeriesGames(sGames);

                    new Thread(() -> dbHelper.updateGame(game)).start();

                    runOnUiThread(() -> {
                        View btnSeries = findViewById(R.id.btnSeries);
                        TextView tvSeriesTitle = findViewById(R.id.tvSeriesTitle);
                        if (btnSeries != null) {
                            btnSeries.setVisibility(View.VISIBLE);
                            if (tvSeriesTitle != null) tvSeriesTitle.setText(foundSeries.name + " Series");

                            btnSeries.setOnClickListener(v -> {
                                Intent intent = new Intent(MyGameDetailsActivity.this, SeriesActivity.class);
                                // Тут передаємо ЛОКАЛЬНИЙ id для переходу
                                intent.putExtra("GAME_ID", game.getId());
                                intent.putExtra("GAME_NAME", game.getName());
                                startActivity(intent);
                            });
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<List<IgdbGame.IgdbSeries>> call, Throwable t) {
                Log.e("IGDB_SERIES", "Error: " + t.getMessage());
            }
        });
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
    private void fetchMissingDataFromIgdb() {
        if (isFetching) return;
        isFetching = true;

        igdbApi.getToken(CLIENT_ID, CLIENT_SECRET, "client_credentials").enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = "Bearer " + response.body().access_token;

                    // Формуємо список полів для запиту
                    String fields = "fields id, name, summary, storyline, url, category, game_type, first_release_date, " +
                            "total_rating, aggregated_rating, cover.url, genres.name, themes.name, " +
                            "keywords.name, platforms.name, similar_games.name, similar_games.id, " +
                            "websites.url, websites.category, external_games.url, external_games.category, " +
                            "language_supports.language.name, screenshots.url, slug;";

                    String query = "";

                    // 1. ЗАХИСТ №1: Якщо ми вже маємо точне посилання (igdbUrl), беремо slug з нього
                    String igdbUrl = game.getIgdbUrl();
                    if (igdbUrl != null && igdbUrl.contains("/games/")) {
                        String exactSlug = igdbUrl.substring(igdbUrl.lastIndexOf("/") + 1);
                        query = fields + " where slug = \"" + exactSlug + "\"; limit 1;";
                    }
                    // 2. Якщо посилання немає, робимо пошук за назвою (але беремо до 10 результатів)
                    else {
                        String cleanName = game.getName().replaceAll("[^a-zA-Z0-9 ]", "");
                        query = fields + " search \"" + cleanName + "\"; limit 10;";
                    }

                    igdbApi.getGames(CLIENT_ID, token, query).enqueue(new Callback<List<IgdbGame>>() {
                        @Override
                        public void onResponse(Call<List<IgdbGame>> call, Response<List<IgdbGame>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                                // Пропускаємо результати через наш фільтр-захисник
                                IgdbGame bestMatch = findBestMatch(response.body(), game.getName());

                                if (bestMatch != null) {
                                    // Знайшли правильну гру, оновлюємо!
                                    new Thread(() -> updateGameObjectAndDb(bestMatch, token)).start();
                                } else {
                                    // Захист спрацював: ігри схожі, але точної немає. Відхиляємо оновлення.
                                    runOnUiThread(() -> Toast.makeText(MyGameDetailsActivity.this, "Exact match not found. Update aborted to prevent wrong data.", Toast.LENGTH_LONG).show());
                                    isFetching = false;
                                }
                            } else {
                                runOnUiThread(() -> Toast.makeText(MyGameDetailsActivity.this, "Game not found on IGDB", Toast.LENGTH_SHORT).show());
                                isFetching = false;
                            }
                        }

                        @Override
                        public void onFailure(Call<List<IgdbGame>> call, Throwable t) {
                            runOnUiThread(() -> Toast.makeText(MyGameDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show());
                            isFetching = false;
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MyGameDetailsActivity.this, "Auth error", Toast.LENGTH_SHORT).show());
                    isFetching = false;
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(MyGameDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show());
                isFetching = false;
            }
        });
    }
    private void updateGameObjectAndDb(IgdbGame igdb, String token) {

        if (igdb.url != null) game.setIgdbUrl(igdb.url);

        if (igdb.aggregated_rating > 0) game.setAggregatedRating((float) igdb.aggregated_rating / 10f);
        if (igdb.total_rating > 0) game.setRating((float) igdb.total_rating / 10f);

        if (igdb.slug != null) game.setRawgSlug(igdb.slug);

        // --- 2. ДАТА РЕЛІЗУ ---
        if (igdb.first_release_date > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(new Date(igdb.first_release_date * 1000L));
            game.setReleased(date);
        }

        // --- 3. ЖАНРИ, ТЕГИ (Теми + Ключові слова) ---
        if (igdb.genres != null) {
            List<String> genres = new ArrayList<>();
            for (IgdbGame.Genre g : igdb.genres) genres.add(g.name);
            game.setGenres(genres);
        }

        List<String> combinedTags = new ArrayList<>();
        if (igdb.themes != null) {
            for (IgdbGame.Theme t : igdb.themes) combinedTags.add(t.name);
        }
        if (igdb.keywords != null) {
            for (IgdbGame.Keyword k : igdb.keywords) {
                if (!combinedTags.contains(k.name)) combinedTags.add(k.name);
            }
        }
        if (!combinedTags.isEmpty()) game.setTags(combinedTags);

        // --- 4. ПЛАТФОРМИ ТА МОВИ ---
        if (igdb.platforms != null) {
            List<String> plats = new ArrayList<>();
            for (IgdbGame.Platform p : igdb.platforms) plats.add(p.name);
            game.setPlatforms(plats);
        }

        if (igdb.language_supports != null) {
            List<String> langs = new ArrayList<>();
            for (IgdbGame.LanguageSupport ls : igdb.language_supports) {
                if (ls.language != null && !langs.contains(ls.language.name)) langs.add(ls.language.name);
            }
            game.setLanguages(langs);
        }

        // --- 5. СПОСІБНІ ІГРИ ---
        if (igdb.similar_games != null) {
            List<String> similar = new ArrayList<>();
            for (IgdbGame.SimilarGame sg : igdb.similar_games) similar.add(sg.name + "|" + sg.id);
            game.setSimilarGames(similar);
        }

        // --- 6. МАГАЗИНИ ---
        if (igdb.websites != null) {
            for (IgdbGame.Website w : igdb.websites) {
                String url = (w.url != null) ? w.url.toLowerCase() : "";
                if (w.category == 13 || url.contains("steampowered.com") || url.contains("steamcommunity.com")) {
                    game.setSteamUrl(w.url);
                } else if (url.contains("playstation.com")) {
                    game.setPsUrl(w.url);
                } else if (url.contains("xbox.com") || url.contains("microsoft.com")) {
                    if (url.contains("/p/") || url.contains("/games/") || url.contains("store")) game.setXboxUrl(w.url);
                } else if (url.contains("nintendo.com") || url.contains("nintendo.co")) {
                    game.setNintendoUrl(w.url);
                }
            }
        }

        // --- 7. ГОЛОВНЕ ФОТО ТА СКРІНШОТИ ---
        if (igdb.cover != null && igdb.cover.url != null) {
            String newRemoteUrl = getHighResUrl(igdb.cover.url, "t_1080p");
            String currentPath = game.getImagePath();

            game.setImageUrl(newRemoteUrl);

            boolean isEmpty = (currentPath == null || currentPath.isEmpty());
            boolean isRemote = (currentPath != null && currentPath.startsWith("http"));
            boolean fileMissing = (currentPath != null && !currentPath.startsWith("http") && !new File(currentPath).exists());

            if (isEmpty || isRemote || fileMissing) {
                game.setImagePath(newRemoteUrl); // Запускає авто-завантаження у loadMainImage()
            }
        }

        if (igdb.screenshots != null && !igdb.screenshots.isEmpty()) {
            List<String> newScreenshots = new ArrayList<>();
            for (IgdbGame.Screenshot s : igdb.screenshots) {
                newScreenshots.add(getHighResUrl(s.url, "t_720p"));
            }
            game.setScreenshots(newScreenshots);
        }

        // --- 8. ЗБЕРІГАЄМО В БАЗУ ДАНИХ! ---
        dbHelper.updateGame(game);

        // --- 9. HLTB (Час) ТА SERIES (Колекція) ---
        // Ці два методи самі роблять мережевий запит і самі ще раз викликають updateGame()
        // для своїх специфічних полів (hltb, collection, seriesGames).
        syncHltbWithIgdb(token, igdb.id);
        fetchSeriesData(token, igdb.id);

        // --- 10. ОНОВЛЮЄМО UI ---
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                populateDetails();
                Toast.makeText(MyGameDetailsActivity.this, "Game data successfully updated!", Toast.LENGTH_SHORT).show();
            }
            isFetching = false; // Дозволяємо кнопці працювати знову
        });
    }

    // Метод для безпечного пошуку правильної гри серед результатів
    private IgdbGame findBestMatch(List<IgdbGame> results, String targetName) {
        if (results == null || results.isEmpty()) return null;

        String targetClean = targetName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        // 1. Пріоритет №1: Точний збіг назви (незалежно від регістру)
        for (IgdbGame g : results) {
            if (g.name != null && g.name.equalsIgnoreCase(targetName)) {
                return g; // Знайшли ідеальний збіг
            }
        }

        // 2. Пріоритет №2: Точний збіг без розділових знаків
        // (Наприклад "Spider-Man" == "Spider Man")
        for (IgdbGame g : results) {
            if (g.name != null) {
                String gClean = g.name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (gClean.equals(targetClean)) {
                    return g;
                }
            }
        }

        // 3. Пріоритет №3: Частковий збіг (назва міститься одна в одній)
        // Беремо найперший такий збіг, бо IGDB сортує за релевантністю
        for (IgdbGame g : results) {
            if (g.name != null) {
                String gLower = g.name.toLowerCase();
                String tLower = targetName.toLowerCase();
                if (gLower.contains(tLower) || tLower.contains(gLower)) {
                    return g;
                }
            }
        }

        // Якщо нічого не підійшло - повертаємо null (захист від перезапису іншою грою)
        return null;
    }
    private String getHighResUrl(String url, String sizeTag) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("//")) url = "https:" + url;
        return url.replaceAll("t_\\w+", sizeTag);
    }
    private void syncHltbWithIgdb(String token, int igdbId) {
        if (game == null) return;

        // Використовуємо реальний ID сервера (igdbId)
        String query = "fields hastily, normally, completely; where game_id = " + igdbId + ";";

        igdbApi.getTimeToBeat(CLIENT_ID, token, query).enqueue(new Callback<List<IgdbTimeToBeat>>() {
            @Override
            public void onResponse(Call<List<IgdbTimeToBeat>> call, Response<List<IgdbTimeToBeat>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    processHltbResponse(response.body().get(0));
                }
            }
            @Override
            public void onFailure(Call<List<IgdbTimeToBeat>> call, Throwable t) {
                Log.e("HLTB_SYNC", "Network error");
            }
        });
    }

    private void processHltbResponse(IgdbTimeToBeat t) {
        String mainTime = (t.hastily / 3600 > 0) ? (t.hastily / 3600) + "h" : "-";
        String extrasTime = (t.normally / 3600 > 0) ? (t.normally / 3600) + "h" : "-";
        String completeTime = (t.completely / 3600 > 0) ? (t.completely / 3600) + "h" : "-";

        String fullHltb = mainTime + "|" + extrasTime + "|" + completeTime;
        game.setHltb(fullHltb);

        // ОБОВ'ЯЗКОВО ЗБЕРІГАЄМО ОНОВЛЕНИЙ ЧАС У БАЗУ!
        new Thread(() -> dbHelper.updateGame(game)).start();

        runOnUiThread(() -> {
            View container = findViewById(R.id.hltbContainer);
            TextView tvMain = findViewById(R.id.tvHltbMain);
            TextView tvExtras = findViewById(R.id.tvHltbExtras);
            TextView tvComplete = findViewById(R.id.tvHltbComplete);

            if (container != null) {
                container.setVisibility(View.VISIBLE);
                tvMain.setText(mainTime);
                tvExtras.setText(extrasTime);
                tvComplete.setText(completeTime);
            }
        });
    }

    // Метод для виправлення ID, якщо він був зламаний пунктуацією
//    private void findCorrectIdAndRetryHltb(String token) {
//        // Очищуємо назву від символів, які заважають пошуку
//        String cleanName = game.getName().replaceAll("[^a-zA-Z0-9 ]", "");
//        String query = "fields id, name, slug; search \"" + cleanName + "\"; limit 1;";
//
//        igdbApi.getGames(CLIENT_ID, token, query).enqueue(new Callback<List<IgdbGame>>() {
//            @Override
//            public void onResponse(Call<List<IgdbGame>> call, Response<List<IgdbGame>> response) {
//                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
//                    IgdbGame correctGame = response.body().get(0);
//
//                    // Якщо знайдений ID відрізняється від того, що в базі - виправляємо базу
//                    if (correctGame.id != game.getId()) {
//                        int oldId = game.getId();
//                        Log.d("HLTB_FIX", "Fixing ID for " + game.getName() + ": " + oldId + " -> " + correctGame.id);
//
//                        // Оновлюємо об'єкт
//                        game.setId(correctGame.id);
//                        // Оновлюємо БД (використовуємо старий ID для пошуку рядка, щоб замінити його на новий)
//                        new Thread(() -> dbHelper.updateGameIdAndSlug(game.getName(), correctGame.id, correctGame.slug)).start();
//
//                        // Тепер, маючи правильний ID, знову пробуємо взяти час
//                        syncHltbWithIgdb(token);
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<IgdbGame>> call, Throwable t) {}
//        });
//    }
    private void loadMainImage() {
        if (isFinishing() || isDestroyed()) return;

        String localPath = game.getImagePath();
        String remoteUrl = game.getImageUrl();
        String urlToLoad = null;

        // 1. ПЕРЕВІРКА ФІЗИЧНОГО ФАЙЛУ
        // Якщо шлях не є посиланням, перевіряємо чи файл реально існує на диску
        if (localPath != null && !localPath.startsWith("http")) {
            File file = new File(localPath);
            if (file.exists()) {
                // Файл знайдено, вантажимо з пам'яті телефону
                Glide.with(this)
                        .load(file)
                        .centerCrop()
                        .into(detailsImage);
                return;
            } else {
                Log.d("IMAGE_LOAD", "Local file missing at path: " + localPath);
            }
        }

        // 2. ФОЛБЕК: Якщо файлу немає, визначаємо робоче посилання (URL)
        // Пріоритет віддаємо remoteUrl, бо в localPath може бути записаний шлях до вже видаленого файлу
        if (remoteUrl != null && remoteUrl.startsWith("http")) {
            urlToLoad = remoteUrl;
        } else if (localPath != null && localPath.startsWith("http")) {
            urlToLoad = localPath;
        }

        // 3. ЗАВАНТАЖЕННЯ ЗА ПОСИЛАННЯМ
        if (urlToLoad != null) {
            // Перетворюємо в високу якість
            String highRes = urlToLoad.replaceAll("t_\\w+", "t_1080p");

            Log.d("IMAGE_LOAD", "Restoring image from URL: " + highRes);

            Glide.with(this)
                    .load(highRes)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .centerCrop()
                    .into(detailsImage);

            // 4. АВТОВІДНОВЛЕННЯ ЛОКАЛЬНОГО ФАЙЛУ
            // Якщо ми в деталях збереженої гри, запускаємо скачування заново,
            // щоб наступного разу файл був на диску
            if (this instanceof MyGameDetailsActivity) {
                downloadImageToInternalStorage(highRes);
            }
        } else {
            // Якщо немає ні файлу, ні посилання в базі
            Log.e("IMAGE_LOAD", "No local file and no remote URL found in DB");
            detailsImage.setImageResource(R.drawable.placeholder);
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
                dbHelper.updateGame(game);

                Log.d("OFFLINE_SAVE", "Головне фото успішно збережено локально: " + localPath);
            }
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

    // Версія з 2 параметрами (вона автоматично викличе версію з 3 параметрами)
    private void addChipsToGroup(ChipGroup group, List<String> items) {
        addChipsToGroup(group, items, false);
    }

    // Версія з 3 параметрами (основна логіка)
    private void addChipsToGroup(ChipGroup group, List<String> items, boolean clickable) {
        if (group == null) return;
        group.removeAllViews();
        if (items == null || items.isEmpty()) return;

        for (String item : items) {
            Chip chip = new Chip(this);
            chip.setText(item);

            // Стилізація
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#58A870")));
            chip.setChipStrokeWidth(2f);
            chip.setTextColor(Color.WHITE);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setClickable(clickable); // Використовуємо третій параметр

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
    private void showCollectionSelector() {
        List<GameCollection> allCollections = dbHelper.getAllCollections(); // або dbHelper для MyGameDetails
        if (allCollections.isEmpty()) {
            Toast.makeText(this, "Create a collection first!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> currentIds = dbHelper.getCollectionIdsForGame(game.getId());
        String[] names = new String[allCollections.size()];
        boolean[] checked = new boolean[allCollections.size()];

        for (int i = 0; i < allCollections.size(); i++) {
            names[i] = allCollections.get(i).getName();
            if (currentIds.contains(allCollections.get(i).getId())) {
                checked[i] = true;
            }
        }
        int Color = android.graphics.Color.parseColor("#58A870");
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Add to Collections")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> {
                    int colId = allCollections.get(which).getId();
                    if (isChecked) dbHelper.addGameToCollection(colId, game.getId());
                    else dbHelper.removeGameFromCollection(colId, game.getId());
                })
                .setPositiveButton("Done", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color);
    }
}