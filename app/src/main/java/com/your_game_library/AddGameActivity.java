package com.your_game_library;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddGameActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText etName, etDesc, etRating, etHltbMain, etHltbExtra, etHltb100,
            etStoryline, etReleased, etGenres, etPlatforms, etTags, etLangs;
    private Spinner spCategory;
    private ImageView imageViewGame;
    private String selectedImagePath = null;
    private int editingGameId = -1;
    private GameDatabaseHelper dbHelper;
    private Game originalGame;
    private static final int PICK_SCREENSHOTS_REQUEST = 2;
    private List<String> manualScreenshots = new ArrayList<>();
    private View sectionScreenshots;
    private Button btnPickScreenshots;
    private TextView tvScreenshotsStatus;

    // --- ЗМІННІ ДЛЯ ЗБЕРЕЖЕННЯ ІСТОРІЇ СТАТИСТИКИ ---
    private String extractedPlannedStats = "";
    private String extractedPlayingStats = "";
    private String extractedCompletedStats = "";
    private int previousSelectedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_game);

        dbHelper = GameDatabaseHelper.getInstance(this);
        initViews();
        setupToolbar();
        setupDateMask();

        editingGameId = getIntent().getIntExtra("gameId", -1);
        if (editingGameId != -1) {
            loadGameData(editingGameId);
        }

        findViewById(R.id.buttonSelectImage).setOnClickListener(v -> selectImage());
        findViewById(R.id.buttonAddGame).setOnClickListener(v -> saveGame());
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDesc = findViewById(R.id.etDescription);
        etRating = findViewById(R.id.etRating);
        etHltbMain = findViewById(R.id.etHltbMain);
        etHltbExtra = findViewById(R.id.etHltbExtra);
        etHltb100 = findViewById(R.id.etHltb100);
        etStoryline = findViewById(R.id.etStoryline);
        etReleased = findViewById(R.id.etReleased);
        etGenres = findViewById(R.id.etGenres);
        etPlatforms = findViewById(R.id.etPlatforms);
        etTags = findViewById(R.id.etTags);
        etLangs = findViewById(R.id.etLanguages);
        spCategory = findViewById(R.id.spCategory);
        imageViewGame = findViewById(R.id.imageViewGame);
        sectionScreenshots = findViewById(R.id.sectionScreenshots);
        btnPickScreenshots = findViewById(R.id.btnPickScreenshots);
        tvScreenshotsStatus = findViewById(R.id.tvScreenshotsStatus);

        btnPickScreenshots.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Screenshots"), PICK_SCREENSHOTS_REQUEST);
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"planned", "playing", "completed"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        // СЛУХАЧ СПІННЕРА: Викликаємо потрібний BottomSheet при зміні категорії
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCat = parent.getItemAtPosition(position).toString();
                if (previousSelectedPosition != position) {
                    if (selectedCat.equals("completed")) {
                        showCompletedBottomSheet(previousSelectedPosition);
                    } else if (selectedCat.equals("playing")) {
                        showPlayingBottomSheet(previousSelectedPosition);
                    } else if (selectedCat.equals("planned")) {
                        showPlannedBottomSheet(previousSelectedPosition);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDateMask() {
        etReleased.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", "");
                    String out = "";
                    if (clean.length() >= 4) {
                        out = clean.substring(0, 4) + "-";
                        if (clean.length() >= 6) {
                            out += clean.substring(4, 6) + "-";
                            if (clean.length() > 6) out += clean.substring(6, Math.min(clean.length(), 8));
                        } else {
                            out += clean.substring(4);
                        }
                    } else { out = clean; }
                    current = out;
                    etReleased.setText(out);
                    etReleased.setSelection(out.length());
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // --- БЕЗПЕЧНІ МЕТОДИ ДЛЯ ПАРСИНГУ ---
    private String extractBlock(String text, String startMarker, String endMarker) {
        if (text == null) return "";
        if (text.contains(startMarker)) {
            int start = text.indexOf(startMarker);
            int end = text.indexOf(endMarker, start) + endMarker.length();
            if (start >= 0 && end > start) {
                return text.substring(start, end) + "\n\n";
            }
        }
        return "";
    }

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

    private void loadGameData(int id) {
        originalGame = dbHelper.getGameByIdObject(id);
        if (originalGame == null) return;

        etName.setText(originalGame.getName());

        String fullDesc = originalGame.getDescription() != null ? originalGame.getDescription() : "";
        String endMarker = "-----------------------------";

        // 1. Витягуємо всі існуючі блоки (щоб не втратити історію)
        extractedCompletedStats = extractBlock(fullDesc, "🏆 --- COMPLETION STATS --- 🏆", endMarker);
        extractedPlayingStats = extractBlock(fullDesc, "🎮 --- PLAYING STATS --- 🎮", endMarker);
        extractedPlannedStats = extractBlock(fullDesc, "🎯 --- PLANNED STATS --- 🎯", endMarker);

        // 2. Очищаємо опис від блоків, щоб показати користувачу чистий текст
        String cleanDesc = fullDesc;
        cleanDesc = removeBlock(cleanDesc, "🏆 --- COMPLETION STATS --- 🏆", endMarker);
        cleanDesc = removeBlock(cleanDesc, "🎮 --- PLAYING STATS --- 🎮", endMarker);
        cleanDesc = removeBlock(cleanDesc, "🎯 --- PLANNED STATS --- 🎯", endMarker);
        cleanDesc = cleanDesc.replaceAll("Added on: \\d{2}\\.\\d{2}\\.\\d{4}", "").trim();

        etDesc.setText(cleanDesc);

        etRating.setText(String.valueOf(originalGame.getRating()));
        etStoryline.setText(originalGame.getStoryline());
        etReleased.setText(originalGame.getReleased());

        if (originalGame.getHltb() != null && originalGame.getHltb().contains("|")) {
            String[] parts = originalGame.getHltb().split("\\|");
            if (parts.length >= 1) etHltbMain.setText(parts[0].replace("h", "").trim());
            if (parts.length >= 2) etHltbExtra.setText(parts[1].replace("h", "").trim());
            if (parts.length >= 3) etHltb100.setText(parts[2].replace("h", "").trim());
        }

        etGenres.setText(TextUtils.join(", ", originalGame.getGenres()));
        etPlatforms.setText(TextUtils.join(", ", originalGame.getPlatforms()));
        etTags.setText(TextUtils.join(", ", originalGame.getTags()));
        etLangs.setText(TextUtils.join(", ", originalGame.getLanguages()));

        List<String> cats = Arrays.asList("planned", "playing", "completed");
        previousSelectedPosition = cats.indexOf(originalGame.getCategory());
        spCategory.setSelection(previousSelectedPosition);

        selectedImagePath = originalGame.getImagePath();
        if (selectedImagePath != null) {
            Bitmap bm = BitmapFactory.decodeFile(selectedImagePath);
            if (bm != null) imageViewGame.setImageBitmap(bm);
        }

        List<String> existingScreens = originalGame.getScreenshots();
        boolean hasUrlScreens = false;
        if (existingScreens != null && !existingScreens.isEmpty()) {
            for (String s : existingScreens) {
                if (s.startsWith("http")) { hasUrlScreens = true; break; }
            }
        }
        if (hasUrlScreens) {
            sectionScreenshots.setVisibility(View.GONE);
        } else {
            sectionScreenshots.setVisibility(View.VISIBLE);
            manualScreenshots.addAll(existingScreens);
            tvScreenshotsStatus.setText(manualScreenshots.size() + " screenshots kept");
        }
    }

    private String formatHltbValue(String val) {
        if (val.isEmpty() || val.equals("-")) return "-";
        return val + "h";
    }

    // --- BOTTOM SHEETS ---

    private void showPlannedBottomSheet(int fallbackPosition) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_planned_sheet, null);

        LinearLayout llRatingContainer = view.findViewById(R.id.PriorityContainer);
        Button btnSave = view.findViewById(R.id.btnSavePlannedGames);

        int oldPriority = 0;
        if (!extractedPlannedStats.isEmpty()) {
            String[] lines = extractedPlannedStats.split("\n");
            for (String line : lines) {
                if (line.startsWith("Priority:")) {
                    try { oldPriority = Integer.parseInt(line.replace("Priority:", "").trim()); } catch (Exception e) {}
                }
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
            selectedBg.setColor(Color.parseColor("#2D5E85"));

            if ((val.equals("None") && oldPriority == 0) || val.equals(String.valueOf(oldPriority))) {
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

        sheet.setOnCancelListener(dialog -> {
            spCategory.setSelection(fallbackPosition);
            previousSelectedPosition = fallbackPosition;
        });

        btnSave.setOnClickListener(v -> {
            int priority = Math.round(selectedRating[0]);
            String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

            StringBuilder stats = new StringBuilder("🎯 --- PLANNED STATS --- 🎯\n");
            if (priority > 0) stats.append("Priority: ").append(priority).append("\n");
            stats.append("Added to Planned: ").append(today).append("\n");
            stats.append("-----------------------------\n\n");

            extractedPlannedStats = stats.toString();
            previousSelectedPosition = spCategory.getSelectedItemPosition();
            sheet.dismiss();
        });

        sheet.setContentView(view);
        sheet.show();
    }

    private void showPlayingBottomSheet(int fallbackPosition) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_playing_sheet, null);

        EditText etStart = view.findViewById(R.id.etStartDate);
        Button btnSave = view.findViewById(R.id.btnSavePlayingGames);
        etStart.addTextChangedListener(new SimpleDateWatcher(etStart));

        String oldStart = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        if (!extractedPlayingStats.isEmpty()) {
            String[] lines = extractedPlayingStats.split("\n");
            for (String line : lines) {
                if (line.startsWith("Started playing:")) oldStart = line.replace("Started playing:", "").trim();
            }
        }
        etStart.setText(oldStart);

        sheet.setOnCancelListener(dialog -> {
            spCategory.setSelection(fallbackPosition);
            previousSelectedPosition = fallbackPosition;
        });

        String finalOldStart = oldStart;
        btnSave.setOnClickListener(v -> {
            String startDate = etStart.getText().toString().trim();
            if (startDate.length() > 0 && startDate.length() < 10) startDate = finalOldStart;

            StringBuilder stats = new StringBuilder("🎮 --- PLAYING STATS --- 🎮\n");
            stats.append("Started playing: ").append(startDate).append("\n");
            stats.append("-----------------------------\n\n");

            extractedPlayingStats = stats.toString();
            previousSelectedPosition = spCategory.getSelectedItemPosition();
            sheet.dismiss();
        });

        sheet.setContentView(view);
        sheet.show();
    }

    private void showCompletedBottomSheet(int fallbackPosition) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_completed_sheet, null);

        LinearLayout llRatingContainer = view.findViewById(R.id.llRatingContainer);
        EditText etTime = view.findViewById(R.id.etTimeSpent);
        EditText etPlays = view.findViewById(R.id.etPlayCount);
        EditText etStart = view.findViewById(R.id.etStartDate);
        EditText etEnd = view.findViewById(R.id.etEndDate);
        Spinner spinnerType = view.findViewById(R.id.spinnerCompletionType);
        Button btnSave = view.findViewById(R.id.btnSaveCompletedStats);

        etStart.addTextChangedListener(new SimpleDateWatcher(etStart));
        etEnd.addTextChangedListener(new SimpleDateWatcher(etEnd));

        String[] types = {"Main Story", "Main + Extras", "100% Completion"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        String oldTime = "", oldPlays = "1", oldStart = "", oldEnd = "";
        int oldTypePos = 0;

        if (!extractedCompletedStats.isEmpty()) {
            String[] lines = extractedCompletedStats.split("\n");
            for (String line : lines) {
                if (line.startsWith("Time Spent:")) oldTime = line.replace("Time Spent:", "").replace("hours", "").trim();
                else if (line.startsWith("Playthroughs:")) oldPlays = line.replace("Playthroughs:", "").trim();
                else if (line.startsWith("Start Date:")) oldStart = line.replace("Start Date:", "").trim();
                else if (line.startsWith("End Date:")) oldEnd = line.replace("End Date:", "").trim();
                else if (line.startsWith("Type:")) {
                    String tStr = line.replace("Type:", "").trim();
                    if (tStr.equals("Main + Extras")) oldTypePos = 1;
                    else if (tStr.equals("100% Completion")) oldTypePos = 2;
                }
            }
        }

        etTime.setText(oldTime);
        etPlays.setText(oldPlays);
        etStart.setText(oldStart);
        etEnd.setText(oldEnd.isEmpty() ? new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()) : oldEnd);
        spinnerType.setSelection(oldTypePos);

        String[] ratingValues = {"None", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        final float[] selectedRating = {(originalGame != null) ? originalGame.getRating() : 0f};
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

            float currentRate = selectedRating[0];
            if ((val.equals("None") && currentRate <= 0) || (currentRate > 0 && val.equals(String.valueOf((int)currentRate)))) {
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

        sheet.setOnCancelListener(dialog -> {
            spCategory.setSelection(fallbackPosition);
            previousSelectedPosition = fallbackPosition;
        });

        btnSave.setOnClickListener(v -> {
            String startD = etStart.getText().toString().trim();
            String endD = etEnd.getText().toString().trim();
            String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

            if (startD.length() > 0 && startD.length() < 10) startD = "";
            if (endD.length() > 0 && endD.length() < 10) endD = today;
            if (endD.isEmpty()) endD = today;

            if (!startD.isEmpty() && !endD.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                try {
                    Date startDate = sdf.parse(startD);
                    Date endDate = sdf.parse(endD);
                    if (startDate != null && endDate != null && startDate.after(endDate)) {
                        Toast.makeText(this, "Start date cannot be after end date!", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception e) { return; }
            }

            float userRating = selectedRating[0];
            if (userRating > 0f) etRating.setText(String.valueOf(userRating));

            String time = etTime.getText().toString().trim();
            String plays = etPlays.getText().toString().trim();
            String type = spinnerType.getSelectedItem().toString();

            StringBuilder stats = new StringBuilder("🏆 --- COMPLETION STATS --- 🏆\n");
            if (!startD.isEmpty()) stats.append("Start Date: ").append(startD).append("\n");
            stats.append("End Date: ").append(endD).append("\n");
            if (!time.isEmpty()) stats.append("Time Spent: ").append(time).append(" hours\n");
            if (!plays.isEmpty()) stats.append("Playthroughs: ").append(plays).append("\n");
            stats.append("Type: ").append(type).append("\n");
            stats.append("-----------------------------\n\n");

            extractedCompletedStats = stats.toString();
            previousSelectedPosition = spCategory.getSelectedItemPosition();
            sheet.dismiss();
        });

        sheet.setContentView(view);
        sheet.show();
    }

    // --- ЗБЕРЕЖЕННЯ ГРИ ---
    private void saveGame() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String hltb = formatHltbValue(etHltbMain.getText().toString().trim()) + " | " +
                formatHltbValue(etHltbExtra.getText().toString().trim()) + " | " +
                formatHltbValue(etHltb100.getText().toString().trim());

        List<String> finalScreenshots = (sectionScreenshots.getVisibility() == View.VISIBLE)
                ? manualScreenshots : ((originalGame != null) ? originalGame.getScreenshots() : new ArrayList<>());

        List<String> similars = (originalGame != null) ? originalGame.getSimilarGames() : new ArrayList<>();
        List<String> seriesGames = (originalGame != null) ? originalGame.getSeriesGames() : new ArrayList<>();
        float aggRating = (originalGame != null) ? originalGame.getAggregatedRating() : 0f;
        String steam = (originalGame != null) ? originalGame.getSteamUrl() : "";
        String ps = (originalGame != null) ? originalGame.getPsUrl() : "";
        String xbox = (originalGame != null) ? originalGame.getXboxUrl() : "";
        String nintendo = (originalGame != null) ? originalGame.getNintendoUrl() : "";
        String igdb = (originalGame != null) ? originalGame.getIgdbUrl() : "";
        String coll = (originalGame != null) ? originalGame.getCollection() : "";
        String gameCat = (originalGame != null) ? originalGame.getGameCategory() : "Main Game";
        String imageUrl = (originalGame != null) ? originalGame.getImageUrl() : "";

        int idToSend = (editingGameId == -1) ? 0 : editingGameId;

        // Збираємо весь опис до купи (Всі статки + текст користувача)
        String rawDesc = etDesc.getText().toString().trim();
        StringBuilder finalDescBuilder = new StringBuilder();

        if (!extractedPlannedStats.isEmpty()) finalDescBuilder.append(extractedPlannedStats);
        if (!extractedPlayingStats.isEmpty()) finalDescBuilder.append(extractedPlayingStats);
        if (!extractedCompletedStats.isEmpty()) finalDescBuilder.append(extractedCompletedStats);

        finalDescBuilder.append(rawDesc);

        Game gameToSave = new Game(
                idToSend, name, spCategory.getSelectedItem().toString(),
                finalDescBuilder.toString(), parseSafe(etRating.getText().toString()),
                selectedImagePath, stringToList(etTags.getText().toString()),
                stringToList(etGenres.getText().toString()), finalScreenshots,
                etReleased.getText().toString(), hltb, steam,
                stringToList(etLangs.getText().toString()), similars, coll,
                stringToList(etPlatforms.getText().toString()), aggRating,
                etStoryline.getText().toString(), igdb, gameCat, xbox, ps, nintendo,
                seriesGames, imageUrl, originalGame.getUserRating(),
                originalGame.getDateStartCompleted(),
                originalGame.getDateEndCompleted(),
                originalGame.getDateAddedPlanned(),
                originalGame.getDateStartPlaying(),
                originalGame.getReview(),
                originalGame.getPriority(),
                originalGame.getType(),
                originalGame.getPlays(),
                originalGame.getTime()
        );

        if (editingGameId == -1) {
            dbHelper.addGame(gameToSave);
        } else {
            dbHelper.updateGame(gameToSave);
        }

        Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private float parseSafe(String val) {
        try { return TextUtils.isEmpty(val) ? 0f : Float.parseFloat(val); } catch (Exception e) { return 0f; }
    }

    private List<String> stringToList(String val) {
        if (TextUtils.isEmpty(val)) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(val.split("\\s*,\\s*")));
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getWindow().setStatusBarColor(Color.parseColor("#121212"));
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SCREENSHOTS_REQUEST && resultCode == RESULT_OK && data != null) {
            manualScreenshots.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    saveScreenshot(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                saveScreenshot(data.getData());
            }
            tvScreenshotsStatus.setText(manualScreenshots.size() + " screenshots selected");
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                imageViewGame.setImageBitmap(bitmap);
                selectedImagePath = saveBitmap(bitmap);
            } catch (Exception e) {}
        }
    }

    private void saveScreenshot(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            File dir = new File(getFilesDir(), "game_images");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "scr_" + System.nanoTime() + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos);
            fos.close();
            manualScreenshots.add(file.getAbsolutePath());
        } catch (Exception e) {}
    }

    private String saveBitmap(Bitmap bm) {
        try {
            File dir = new File(getFilesDir(), "game_images");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "man_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            return f.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    private class SimpleDateWatcher implements android.text.TextWatcher {
        private String current = "";
        private android.widget.EditText input;

        public SimpleDateWatcher(android.widget.EditText input) { this.input = input; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().equals(current)) return;
            String clean = s.toString().replaceAll("[^\\d]", "");
            String cleanC = current.replaceAll("[^\\d]", "");
            int cl = clean.length();
            int sel = cl;
            for (int i = 2; i <= cl && i < 6; i += 2) sel++;
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
        @Override public void afterTextChanged(Editable s) {}
    }
}