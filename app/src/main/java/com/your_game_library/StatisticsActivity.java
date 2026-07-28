package com.your_game_library;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class StatisticsActivity extends AppCompatActivity {

    private com.your_game_library.GameDatabaseHelper dbHelper;
    private TextView tvTotalTime, tvAvgRating, tvBacklogTime;
    private PieChart pieChartStatus;
    private LinearLayout llMostPlayedContainer, llTopGenresContainer;
    private View cardMostPlayed;
    private LinearLayout llAchievementsContainer;
    private Queue<Achievement> newAchievementsQueue = new LinkedList<>();
    private boolean isPopupShowing = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = com.your_game_library.GameDatabaseHelper.getInstance(this);

        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        tvBacklogTime = findViewById(R.id.tvBacklogTime);
        pieChartStatus = findViewById(R.id.pieChartStatus);
        llMostPlayedContainer = findViewById(R.id.llMostPlayedContainer);
        llTopGenresContainer = findViewById(R.id.llTopGenresContainer);
        cardMostPlayed = findViewById(R.id.cardMostPlayed);
        llAchievementsContainer = findViewById(R.id.llAchievementsContainer);

        renderAchievements(dbHelper.getAllGames());
        setupToolbar();
        calculateAndShowStatistics();
    }
    private void renderAchievements(List<com.your_game_library.Game> allGames) {
        llAchievementsContainer.removeAllViews();

        // Отримуємо розраховані досягнення з менеджера
        List<Achievement> achievements = AchievementManager.calculateAchievements(allGames);

        for (Achievement a : achievements) {
            View item = getLayoutInflater().inflate(R.layout.item_achievement, llAchievementsContainer, false);

            androidx.cardview.widget.CardView cvIcon = item.findViewById(R.id.cvIcon);
            ImageView ivIcon = item.findViewById(R.id.ivIcon);
            TextView tvTitle = item.findViewById(R.id.tvTitle);
            TextView tvDesc = item.findViewById(R.id.tvDesc);
            TextView tvProgress = item.findViewById(R.id.tvProgress);

            ivIcon.setImageResource(a.iconResId);
            tvTitle.setText(a.title);
            tvDesc.setText(a.description);
            tvProgress.setText(a.progressText);

            if (a.isVectorIcon) {
                ivIcon.setPadding(30, 30, 30, 30); // Відступи для векторних іконок
            } else {
                ivIcon.setPadding(0, 0, 0, 0); // Немає відступів для повноцінних фото
            }
            if (a.isUnlocked) {
                // РОЗБЛОКОВАНО
                cvIcon.setCardBackgroundColor(Color.parseColor("#1Afc6f03")); // Легкий помаранчевий фон
                tvTitle.setTextColor(Color.WHITE);
                tvProgress.setTextColor(Color.parseColor("#fc6f03"));
                tvProgress.setText("UNLOCKED");

                // Якщо це векторна іконка - фарбуємо в помаранчевий. Якщо фото - повертаємо 100% видимість.
                if (a.isVectorIcon) {
                    ivIcon.setColorFilter(Color.parseColor("#fc6f03"));
                    ivIcon.setAlpha(1.0f);
                } else {
                    ivIcon.clearColorFilter();
                    ivIcon.setAlpha(1.0f);
                }
            } else {
                // ЗАБЛОКОВАНО
                cvIcon.setCardBackgroundColor(Color.parseColor("#262626")); // Темний фон
                tvTitle.setTextColor(Color.parseColor("#888888"));
                tvProgress.setTextColor(Color.parseColor("#888888"));

                // Якщо це векторна іконка - фарбуємо в сірий. Якщо фото - робимо його напівпрозорим і темнішим.
                if (a.isVectorIcon) {
                    ivIcon.setColorFilter(Color.parseColor("#666666"));
                    ivIcon.setAlpha(1.0f);
                } else {
                    ivIcon.clearColorFilter();
                    ivIcon.setColorFilter(Color.parseColor("#88000000")); // Накладаємо легку темну тінь
                    ivIcon.setAlpha(0.5f); // Робимо напівпрозорим
                }
            }

            llAchievementsContainer.addView(item);
        }
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        getWindow().setStatusBarColor(Color.parseColor("#121212"));
    }

    private void calculateAndShowStatistics() {
        List<Game> allGames = dbHelper.getAllGames();
        if (allGames.isEmpty()) return;

        int countCompleted = 0, countPlaying = 0, countPlanned = 0;
        int totalTimeHours = 0;
        int backlogTimeHours = 0;
        float totalRatingSum = 0;
        int ratedGamesCount = 0;

        Map<String, Integer> genreCounts = new HashMap<>();
        List<Game> topTimeGames = new ArrayList<>();

        for (Game game : allGames) {
            String cat = game.getCategory() != null ? game.getCategory().toLowerCase() : "";

            if (cat.equals("completed")) {
                countCompleted++;
                if (game.getTime() != null && game.getTime() > 0) {
                    totalTimeHours += game.getTime();
                    topTimeGames.add(game);
                }
            }
            else if (cat.equals("playing")) countPlaying++;
            else if (cat.equals("planned")) {
                countPlanned++;
                if (game.getHltb() != null && game.getHltb().contains("|")) {
                    String[] parts = game.getHltb().split("\\|");
                    int mainTime = parseHltbTime(parts[0]);
                    int extraTime = (parts.length > 1) ? parseHltbTime(parts[1]) : mainTime;
                    if (mainTime > 0 && extraTime > 0) backlogTimeHours += (mainTime + extraTime) / 2;
                    else if (mainTime > 0) backlogTimeHours += mainTime;
                }
            }

            if (game.getUserRating() != null && game.getUserRating() > 0) {
                totalRatingSum += game.getUserRating();
                ratedGamesCount++;
            }

            if (game.getGenres() != null) {
                for (String genre : game.getGenres()) {
                    genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
                }
            }
        }

        // 1. Огляд
        tvTotalTime.setText(totalTimeHours + "h");
        tvBacklogTime.setText("~" + backlogTimeHours + "h");
        if (ratedGamesCount > 0) tvAvgRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", (totalRatingSum / ratedGamesCount)));
        else tvAvgRating.setText("N/A");

        // 2. Рендеринг मोस्ट Played
        renderMostPlayed(topTimeGames);

        // 3. Рендеринг Жанрів
        renderGenres(genreCounts);

        // 4. Бублик Статусів
        setupPieChart(countCompleted, countPlaying, countPlanned);
        checkForNewUnlocks(AchievementManager.calculateAchievements(allGames));
    }
    // Перевіряє всі ачівки і додає нові в чергу
    private void checkForNewUnlocks(List<Achievement> achievements) {
        SharedPreferences prefs = getSharedPreferences("achievements_prefs", MODE_PRIVATE);

        for (Achievement a : achievements) {
            if (a.isUnlocked) {
                // Перевіряємо, чи ми вже показували цю ачівку
                boolean alreadyShown = prefs.getBoolean(a.getPrefsKey(), false);
                if (!alreadyShown) {
                    newAchievementsQueue.add(a); // Додаємо в чергу на показ
                }
            }
        }

        // Якщо є щось у черзі і зараз нічого не показується - запускаємо показ
        if (!newAchievementsQueue.isEmpty() && !isPopupShowing) {
            showNextAchievementPopup();
        }
    }

    // Показує наступне вікно з черги
    private void showNextAchievementPopup() {
        if (newAchievementsQueue.isEmpty()) {
            isPopupShowing = false;
            return;
        }

        isPopupShowing = true;
        Achievement currentAchiev = newAchievementsQueue.poll(); // Беремо першу з черги

        // Будуємо діалог
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_achievement, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false); // Забороняємо закривати кліком повз, щоб точно зберегти статус

        // Налаштовуємо UI
        ImageView ivIcon = view.findViewById(R.id.ivAchievementIcon);
        TextView tvTitle = view.findViewById(R.id.tvAchievementTitle);
        TextView tvDesc = view.findViewById(R.id.tvAchievementDesc);
        Button btnOk = view.findViewById(R.id.btnAchievementOk);

        ivIcon.setImageResource(currentAchiev.iconResId);
        tvTitle.setText(currentAchiev.title);
        tvDesc.setText(currentAchiev.description);

        if (currentAchiev.isVectorIcon) {
            ivIcon.setColorFilter(Color.parseColor("#fc6f03"));
            ivIcon.setPadding(40, 40, 40, 40); // Відступи для вектора (бо вікно велике)
        } else {
            ivIcon.clearColorFilter();
            ivIcon.setPadding(0, 0, 0, 0);
        }

        // Обробка кліку "OK"
        btnOk.setOnClickListener(v -> {
            // Зберігаємо інформацію, що ми її вже показали
            SharedPreferences prefs = getSharedPreferences("achievements_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean(currentAchiev.getPrefsKey(), true).apply();

            dialog.dismiss();

            // Відкриваємо наступне вікно з черги (якщо воно є)
            showNextAchievementPopup();
        });

        dialog.show();
    }
    private int parseHltbTime(String hltbString) {
        try {
            String clean = hltbString.replaceAll("[^0-9]", "");
            if (!clean.isEmpty()) return Integer.parseInt(clean);
        } catch (Exception ignored) {}
        return 0;
    }

    private void renderMostPlayed(List<Game> games) {
        if (games.isEmpty()) {
            cardMostPlayed.setVisibility(View.GONE);
            return;
        }

        llMostPlayedContainer.removeAllViews();
        games.sort((g1, g2) -> g2.getTime().compareTo(g1.getTime())); // За спаданням

        int limit = Math.min(5, games.size());
        for (int i = 0; i < limit; i++) {
            Game g = games.get(i);
            View item = getLayoutInflater().inflate(R.layout.item_stat_most_played, llMostPlayedContainer, false);

            TextView tvRank = item.findViewById(R.id.tvRank);
            ImageView ivCover = item.findViewById(R.id.ivGameCover);
            TextView tvTitle = item.findViewById(R.id.tvGameTitle);
            TextView tvTime = item.findViewById(R.id.tvGameTime);

            tvRank.setText(String.valueOf(i + 1));
            tvTitle.setText(g.getName());
            String formattedTime = String.format(java.util.Locale.US, "%.1f", g.getTime());
            tvTime.setText(formattedTime + "h");

            // Завантаження картинки
            String imagePath = g.getImagePath() != null ? g.getImagePath() : g.getImageUrl();
            if (imagePath != null) {
                Object source = imagePath.startsWith("http") ? imagePath : new File(imagePath);
                Glide.with(this).load(source).centerCrop().into(ivCover);
            }

            llMostPlayedContainer.addView(item);
        }
    }

    private void renderGenres(Map<String, Integer> genreCounts) {
        llTopGenresContainer.removeAllViews();
        if (genreCounts.isEmpty()) return;

        List<Map.Entry<String, Integer>> list = new ArrayList<>(genreCounts.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        int limit = Math.min(8, list.size());
        int maxCount = list.get(0).getValue(); // Найбільше значення для ProgressBar.max

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = list.get(i);
            View item = getLayoutInflater().inflate(R.layout.item_stat_progress, llTopGenresContainer, false);

            TextView tvName = item.findViewById(R.id.tvStatName);
            ProgressBar pbProgress = item.findViewById(R.id.pbStatProgress);
            TextView tvCount = item.findViewById(R.id.tvStatCount);

            tvName.setText(entry.getKey());
            tvCount.setText(String.valueOf(entry.getValue()));

            pbProgress.setMax(maxCount);
            pbProgress.setProgress(entry.getValue());

            llTopGenresContainer.addView(item);
        }
    }

    private void setupPieChart(int completed, int playing, int planned) {
        int total = completed + playing + planned;
        if (total == 0) return; // Захист від ділення на нуль

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        // Формуємо красивий текст легенди з числом та відсотком
        if (completed > 0) {
            String label = String.format(java.util.Locale.getDefault(), "Completed   %d  •  %d%%", completed, (completed * 100 / total));
            entries.add(new PieEntry(completed, label));
            colors.add(Color.parseColor("#fc6f03"));
        }
        if (playing > 0) {
            String label = String.format(java.util.Locale.getDefault(), "Playing   %d  •  %d%%", playing, (playing * 100 / total));
            entries.add(new PieEntry(playing, label));
            colors.add(Color.parseColor("#58A870"));
        }
        if (planned > 0) {
            String label = String.format(java.util.Locale.getDefault(), "Planned   %d  •  %d%%", planned, (planned * 100 / total));
            entries.add(new PieEntry(planned, label));
            colors.add(Color.parseColor("#2D5E85"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false); // Ховаємо цифри з самого бублика

        pieChartStatus.setData(new PieData(dataSet));
        pieChartStatus.getDescription().setEnabled(false);

        // --- НАЛАШТУВАННЯ БУБЛИКА ---
        pieChartStatus.setDrawHoleEnabled(true);
        pieChartStatus.setHoleColor(Color.TRANSPARENT);
        pieChartStatus.setTransparentCircleRadius(0f);
        pieChartStatus.setHoleRadius(65f); // Товщина кільця

        pieChartStatus.setDrawEntryLabels(false); // Ховаємо текст з графіку (залишаємо тільки легенду)

        // --- ЛЕГЕНДА СПРАВА ---
        Legend l = pieChartStatus.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setTextColor(Color.WHITE);
        l.setTextSize(13f);
        l.setForm(Legend.LegendForm.CIRCLE); // Круглі маркери
        l.setFormSize(10f);
        l.setXEntrySpace(15f); // Відступ легенди від графіка
        l.setYEntrySpace(8f);  // Відстань між рядками легенди

        pieChartStatus.animateY(1200, Easing.EaseInOutQuad);
        pieChartStatus.invalidate();
    }
}