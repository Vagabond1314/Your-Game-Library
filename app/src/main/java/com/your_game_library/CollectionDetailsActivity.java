
package com.your_game_library;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionDetailsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private GameDatabaseHelper dbHelper;
    private int collectionId;
    private String collectionName;
    private TextView tvEmpty, tvSort;
    private ImageButton btnSortDirection;
    private SearchView searchView;
    private ConstraintLayout topBarLayout;

    private static final String PREF_NAME = "app_settings";
    private static final String KEY_SORT_COLUMN = "sort_column_col"; // Окремий ключ для колекцій
    private static final String KEY_IS_ASCENDING = "is_ascending_col";

    Map<String, List<Game>> cachedLists = new HashMap<>();
    private String currentSortCriteria = "id DESC";
    private String currentSortColumn = "id";
    private boolean isAscending = false;
    int accentColor;

    // Фільтри (якщо захочеш додати пізніше)
    private String selectedGenreFilter = "", selectedTagFilter = "", selectedPlatformFilter = "", selectedLanguageFilter = "";
    private List<String> selectedGenresList = new ArrayList<>(), selectedTagsList = new ArrayList<>(), selectedPlatformsList = new ArrayList<>(), selectedLanguagesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_details);

        collectionId = getIntent().getIntExtra("COLLECTION_ID", -1);
        collectionName = getIntent().getStringExtra("COLLECTION_NAME");
        accentColor = getIntent().getIntExtra("COLLECTION_COLOR", Color.parseColor("#58A870"));

        dbHelper = GameDatabaseHelper.getInstance(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarCollection);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());
        initSettings();
        initViews();
        setupGlowEffect();
        setupFilterButton();
        setupSearchLogic();

        // Завантажуємо дані з відновленням скролу (false для першого разу)
        loadCategorySorted(collectionId, false);

        findViewById(R.id.fabAddGameToCol).setOnClickListener(v -> showAddGamesDialog());
    }
    private void setupFilterButton() {
        View btnFilters = findViewById(R.id.btnFilters);
        TextView tvSortLabel = findViewById(R.id.tvCurrentSort);
        ImageView ivTune = findViewById(R.id.ivTune);

        // Фарбуємо іконку фільтра в колір колекції
        ivTune.setImageTintList(ColorStateList.valueOf(accentColor));

        btnFilters.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet(
                    currentSortColumn,
                    isAscending,
                    selectedGenresList,
                    selectedTagsList,
                    selectedPlatformsList,
                    selectedLanguagesList,
                    new FilterBottomSheet.FilterListener() {
                        @Override
                        public void onApplyFilters(String sortColumn, boolean ascending, boolean reset) {
                            if (reset) {
                                currentSortColumn = "id";
                                isAscending = false;
                                clearFilters();
                            } else {
                                currentSortColumn = sortColumn;
                                isAscending = ascending;
                            }

                            tvSortLabel.setText(getSortLabelByColumn(currentSortColumn));
                            applyAndLoad();
                        }

                        // Передаємо 'sheet', щоб мітки оновлювалися миттєво
                        @Override public void onOpenGenreDialog(com.your_game_library.FilterBottomSheet sheet) { showGenreFilterDialog(sheet); }
                        @Override public void onOpenTagDialog(com.your_game_library.FilterBottomSheet sheet) { showTagFilterDialog(sheet); }
                        @Override public void onOpenPlatformDialog(com.your_game_library.FilterBottomSheet sheet) { showPlatformFilterDialog(sheet); }
                        @Override public void onOpenLanguageDialog(com.your_game_library.FilterBottomSheet sheet) { showLanguageFilterDialog(sheet); }
                    }
            );
            sheet.show(getSupportFragmentManager(), "filter_sheet");
        });
    }
    private void showGenreFilterDialog(com.your_game_library.FilterBottomSheet sheet) {
        List<String> allGenres = dbHelper.getAllUniqueGenres();
        String[] genresArray = allGenres.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allGenres.size()];

        for (int i = 0; i < allGenres.size(); i++) {
            if (selectedGenresList.contains(allGenres.get(i))) checkedItems[i] = true;
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Select Genres")
                .setMultiChoiceItems(genresArray, checkedItems, (d, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("Select", (d, which) -> {
                    selectedGenresList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedGenresList.add(genresArray[i]);
                    }
                    selectedGenreFilter = selectedGenresList.isEmpty() ? "" : String.join("|", selectedGenresList);
                    if (sheet != null) sheet.updateSummaryLabels(); // Оновлюємо текст у меню
                })
                .setNeutralButton("Clear All", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
                dialog.getListView().setItemChecked(i, false);
            }
            selectedGenresList.clear();
            selectedGenreFilter = "";
            if (sheet != null) sheet.updateSummaryLabels();
        });
    }

    private void showTagFilterDialog(com.your_game_library.FilterBottomSheet sheet) {
        List<String> allTags = dbHelper.getAllUniqueTags();
        List<String> tempSelectedTags = new ArrayList<>(selectedTagsList);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_list, null);
        androidx.appcompat.widget.SearchView sv = dialogView.findViewById(R.id.searchViewTags);
        RecyclerView rv = dialogView.findViewById(R.id.rvTags);

        MainActivity.TagSelectionAdapter tagAdapter = new MainActivity.TagSelectionAdapter(allTags, tempSelectedTags);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(tagAdapter);

        sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                tagAdapter.filter(newText);
                return true;
            }
        });

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Filter by Tags")
                .setView(dialogView)
                .setPositiveButton("Select", (d, which) -> {
                    selectedTagsList.clear();
                    selectedTagsList.addAll(tempSelectedTags);
                    selectedTagFilter = selectedTagsList.isEmpty() ? "" : String.join("|", selectedTagsList);
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNeutralButton("Clear All", (d, which) -> {
                    selectedTagsList.clear();
                    selectedTagFilter = "";
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();
    }

    private void showPlatformFilterDialog(FilterBottomSheet sheet) {
        List<String> allPlatforms = dbHelper.getAllUniquePlatforms();
        String[] platformsArray = allPlatforms.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allPlatforms.size()];

        for (int i = 0; i < allPlatforms.size(); i++) {
            if (selectedPlatformsList.contains(allPlatforms.get(i))) checkedItems[i] = true;
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Select Platforms")
                .setMultiChoiceItems(platformsArray, checkedItems, (d, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("Select", (d, which) -> {
                    selectedPlatformsList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedPlatformsList.add(platformsArray[i]);
                    }
                    selectedPlatformFilter = selectedPlatformsList.isEmpty() ? "" : String.join("|", selectedPlatformsList);
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear All", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
                dialog.getListView().setItemChecked(i, false);
            }
            selectedPlatformsList.clear();
            selectedPlatformFilter = "";
            if (sheet != null) sheet.updateSummaryLabels();
        });
    }

    private void showLanguageFilterDialog(FilterBottomSheet sheet) {
        List<String> allLanguages = dbHelper.getAllUniqueLanguages();
        String[] languagesArray = allLanguages.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allLanguages.size()];

        for (int i = 0; i < allLanguages.size(); i++) {
            if (selectedLanguagesList.contains(allLanguages.get(i))) checkedItems[i] = true;
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Select Languages")
                .setMultiChoiceItems(languagesArray, checkedItems, (d, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("Select", (d, which) -> {
                    selectedLanguagesList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedLanguagesList.add(languagesArray[i]);
                    }
                    selectedLanguageFilter = selectedLanguagesList.isEmpty() ? "" : String.join("|", selectedLanguagesList);
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear All", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
                dialog.getListView().setItemChecked(i, false);
            }
            selectedLanguagesList.clear();
            selectedLanguageFilter = "";
            if (sheet != null) sheet.updateSummaryLabels();
        });
    }
    private void initSettings() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentSortColumn = prefs.getString(KEY_SORT_COLUMN, "id");
        isAscending = prefs.getBoolean(KEY_IS_ASCENDING, false);
        updateSortCriteria();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rvCollectionGames);
        tvEmpty = findViewById(R.id.tvEmptyCollection);
        searchView = findViewById(R.id.searchView);
        topBarLayout = findViewById(R.id.topBarConstraintLayout);
        Toolbar toolbar = findViewById(R.id.toolbarCollection);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(collectionName);
        }

        updateLayoutManager();
    }

    private void setupGlowEffect() {
        // 1. Фарбуємо заголовок Toolbar

        // 2. Фарбуємо рамку картки пошуку (потрібен MaterialCardView)
        androidx.cardview.widget.CardView card = findViewById(R.id.topBarCard);
        // Якщо змінити CardView на MaterialCardView в XML:
        // ((com.google.android.material.card.MaterialCardView) card).setStrokeColor(ColorStateList.valueOf(accentColor));
        // ((com.google.android.material.card.MaterialCardView) card).setStrokeWidth(3);

        // 3. Фарбуємо лінію-розділювач (робимо її товщою)
        View divider = findViewById(R.id.colorDivider);
        divider.setBackgroundColor(accentColor);
        ViewGroup.LayoutParams params = divider.getLayoutParams();
        params.height = 16; // Робимо лінію чіткішою
        divider.setLayoutParams(params);

        FloatingActionButton fab = findViewById(R.id.fabAddGameToCol);
        fab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
    }

    private void updateLayoutManager() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isGrid = prefs.getBoolean("is_main_grid", false);
        recyclerView.setLayoutManager(isGrid ? new GridLayoutManager(this, 2) : new LinearLayoutManager(this));

        // Якщо адаптер вже є, оновлюємо йому режим відображення
        if (adapter != null) {
            adapter.setGrid(isGrid);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ВАЖЛИВО: Оновлюємо LayoutManager та режим адаптера при поверненні з налаштувань
        updateLayoutManager();
        loadCategorySorted(collectionId, true);
    }

    private void handleSortSelection(String option) {
        // Скидання фільтрів для стандартних типів
        if (!option.equals("Genre") && !option.equals("Tag") && !option.equals("Platform") && !option.equals("Language")) {
            clearFilters();
        }

        switch (option) {
            case "Rating": currentSortColumn = "rating"; break;
            case "Year": currentSortColumn = "released"; break;
            case "Name": currentSortColumn = "name"; break;
            case "Genre": showGenreFilterDialog(); return;
            case "Tag": showTagFilterDialog(); return;
            case "Platform": showPlatformFilterDialog(); return;
            case "Language": showLanguageFilterDialog(); return;
            default: currentSortColumn = "id"; break;
        }
        applyAndLoad();
    }

    private void applyAndLoad() {
        updateSortCriteria();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_SORT_COLUMN, currentSortColumn).putBoolean(KEY_IS_ASCENDING, isAscending).apply();
        cachedLists.clear();
        loadCategorySorted(collectionId, false);
    }

    private void updateSortCriteria() {
        String direction = isAscending ? " ASC" : " DESC";
        currentSortCriteria = currentSortColumn + direction;
    }

    private String getSortLabelByColumn(String column) {
        switch (column) {
            case "name": return "Name";
            case "released": return "Year";
            case "rating": return "Rating";
            default: return "Default";
        }
    }
    TransitionSet createTransition() {
        return new TransitionSet()
                .addTransition(new ChangeBounds()) // Плавна зміна ширини
                .addTransition(new Fade())         // Плавне зникнення/поява
                .setDuration(400)                  // Трохи збільшимо час для м'якості
                .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
    }
    private void setupSearchLogic() {
        final View filtersBtn = findViewById(R.id.btnFilters);
        final ConstraintLayout topBarLayout = findViewById(R.id.topBarConstraintLayout);

        final EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        final ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        final ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);

        // 1. ПЛАВНА КОМБІНОВАНА АНІМАЦІЯ
        // Створюємо набір: ChangeBounds (для розтягування бару) + Fade (для зникнення кнопок)


        // 2. АКТИВАЦІЯ ПО ВСІЙ ПОВЕРХНІ
        View.OnClickListener onSearchAreaClick = v -> {
            searchEditText.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        };

        searchView.setOnClickListener(onSearchAreaClick);
        searchEditText.setOnClickListener(onSearchAreaClick);
        if (searchIcon != null) searchIcon.setOnClickListener(onSearchAreaClick);

        // 3. ЛОГІКА ФОКУСУ ТА РОЗГОРТАННЯ
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(topBarLayout, createTransition());

            if (hasFocus) {
                // ХОВАЄМО ВСЕ ЗАЙВЕ
                filtersBtn.setVisibility(View.GONE);

                if (closeButton != null) {
                    closeButton.post(() -> closeButton.setVisibility(View.VISIBLE));
                }
            } else {
                // ПОВЕРТАЄМО, ЯКЩО ПОШУК ПОРОЖНІЙ
                if (searchView.getQuery().length() == 0) {
                    filtersBtn.setVisibility(View.VISIBLE);
                    if (closeButton != null) closeButton.setVisibility(View.GONE);
                }
            }
        });

        // 4. ЛОГІКА ХРЕСТИКА (ЗАКРИТТЯ)
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                if (searchView.getQuery().length() > 0) {
                    searchView.setQuery("", false);
                    closeButton.post(() -> closeButton.setVisibility(View.VISIBLE));
                } else {
                    // ПОВНЕ ЗАКРИТТЯ
                    searchEditText.clearFocus();

                    TransitionManager.beginDelayedTransition(topBarLayout, createTransition());
                    filtersBtn.setVisibility(View.VISIBLE);
                    closeButton.setVisibility(View.GONE);

                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    filterGames("");
                }
            });
        }

        // 5. СЛУХАЧ ТЕКСТУ
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterGames(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (closeButton != null && searchEditText.hasFocus()) {
                    closeButton.setVisibility(View.VISIBLE);
                }
                filterGames(newText);
                return true;
            }
        });
    }
    private void setupSortLogic() {
        String[] sortOptions = {"Default", "Name", "Year", "Rating", "Genre", "Tag", "Platform", "Language"};

        tvSort.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(this);

            // 1. Адаптер з твоїм дизайном елементів
            popup.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, sortOptions));

            // 2. Якір — TextView
            popup.setAnchorView(tvSort);

            // 3. Дизайн фону
            popup.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.dialog_bg));

            // 4. Розміри
            int popupWidth = (int) (150 * getResources().getDisplayMetrics().density);
            popup.setWidth(popupWidth);
            popup.setHeight(ListPopupWindow.WRAP_CONTENT);
            popup.setModal(true);

            // 5. ІДЕАЛЬНЕ ЦЕНТРУВАННЯ ТА ПОЗИЦІЯ (як у MainActivity)
            tvSort.post(() -> {
                int tvWidth = tvSort.getWidth();
                int horizontalOffset = (tvWidth - popupWidth) / 2;
                popup.setHorizontalOffset(horizontalOffset);
                popup.setVerticalOffset(0); // Рівно під кнопкою
                popup.show();
            });

            // 6. Клік
            popup.setOnItemClickListener((parent, view, position, id) -> {
                String selected = sortOptions[position];
                tvSort.setText(selected);
                handleSortSelection(selected); // Твій метод обробки фільтрів
                popup.dismiss();
            });
        });

        // Кнопка напрямку (ASC/DESC) з анімацією повороту
        btnSortDirection.setOnClickListener(v -> {
            isAscending = !isAscending;
            btnSortDirection.animate()
                    .rotation(isAscending ? 180 : 0)
                    .setDuration(300)
                    .start();
            applyAndLoad();
        });
    }

    private void loadCategorySorted(int colId, boolean restoreScroll) {
        // Отримуємо актуальне значення isGrid
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isGrid = prefs.getBoolean("is_main_grid", false);

        List<Game> gamesToShow = dbHelper.getGamesByCollectionSorted(colId, currentSortCriteria,
                selectedGenreFilter, selectedTagFilter, selectedPlatformFilter, selectedLanguageFilter);

        tvEmpty.setVisibility(gamesToShow.isEmpty() ? View.VISIBLE : View.GONE);

        if (adapter == null) {
            adapter = new GameAdapter(this, gamesToShow, dbHelper, isGrid, colId);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setGrid(isGrid); // Передаємо актуальний режим у адаптер
            adapter.submitList(gamesToShow, () -> {
                if (!restoreScroll) recyclerView.scrollToPosition(0);
            });
        }
    }

    private void filterGames(String query) {
        // Отримуємо базовий список ігор цієї колекції з урахуванням поточного сортування та фільтрів
        List<Game> allGames = dbHelper.getGamesByCollectionSorted(collectionId, currentSortCriteria,
                selectedGenreFilter, selectedTagFilter, selectedPlatformFilter, selectedLanguageFilter);

        if (query == null || query.isEmpty()) {
            adapter.submitList(allGames, null);
            tvEmpty.setVisibility(allGames.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<Game> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (Game g : allGames) {
            if (g.getName() != null && g.getName().toLowerCase().contains(lowerQuery)) {
                filtered.add(g);
            }
        }

        adapter.submitList(filtered, () -> {
            // Показуємо напис "Порожньо", якщо нічого не знайдено саме пошуком
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void clearFilters() {
        selectedGenreFilter = ""; selectedTagFilter = ""; selectedPlatformFilter = ""; selectedLanguageFilter = "";
        selectedGenresList.clear(); selectedTagsList.clear(); selectedPlatformsList.clear(); selectedLanguagesList.clear();
    }

    private void loadGames() {
        List<Game> games = dbHelper.getGamesInCollection(collectionId);
        tvEmpty.setVisibility(games.isEmpty() ? View.VISIBLE : View.GONE);

        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isGrid = prefs.getBoolean("is_main_grid", false);

        adapter = new GameAdapter(this, games, dbHelper, isGrid, collectionId);
        recyclerView.setAdapter(adapter);
    }

    private void showAddGamesDialog() {
        // 1. Завантажуємо всі ігри
        List<Game> allGames = dbHelper.getAllGames();
        List<Game> currentInCol = dbHelper.getGamesInCollection(collectionId);
        List<Integer> currentIds = new ArrayList<>();
        for (Game g : currentInCol) currentIds.add(g.getId());

        // Створюємо список моделей для вибору
        List<SelectableGame> selectableGames = new ArrayList<>();
        for (Game g : allGames) {
            selectableGames.add(new SelectableGame(g, currentIds.contains(g.getId())));
        }

        // 2. Створюємо View діалогу
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_selector, null);
        androidx.appcompat.widget.SearchView sv = dialogView.findViewById(R.id.svGameSelector);
        RecyclerView rv = dialogView.findViewById(R.id.rvGameSelector);

        GameSelectorAdapter selectorAdapter = new GameSelectorAdapter(selectableGames);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(selectorAdapter);

        // Логіка пошуку
        sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                selectorAdapter.filter(newText);
                return true;
            }
        });

        // 3. Показуємо AlertDialog
        new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Add games to " + collectionName)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    dbHelper.removeAllGamesFromCollection(collectionId);
                    for (SelectableGame sg : selectableGames) {
                        if (sg.isSelected) {
                            dbHelper.addGameToCollection(collectionId, sg.game.getId());
                        }
                    }
                    loadGames(); // Оновити список в Activity
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showGenreFilterDialog() {
        // ВАЖЛИВО: беремо жанри ТІЛЬКИ цієї колекції
        List<String> allGenres = dbHelper.getGenresInCollection(collectionId);

        if (allGenres.isEmpty()) {
            Toast.makeText(this, "No genres found in this collection", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] genresArray = allGenres.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allGenres.size()];

        for (int i = 0; i < allGenres.size(); i++) {
            if (selectedGenresList.contains(allGenres.get(i))) {
                checkedItems[i] = true;
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Genres in " + collectionName)
                .setMultiChoiceItems(genresArray, checkedItems, (d, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Filter", (d, which) -> {
                    selectedGenresList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedGenresList.add(genresArray[i]);
                    }
                    selectedGenreFilter = selectedGenresList.isEmpty() ? "" : String.join("|", selectedGenresList);
                    applyAndLoad();
                })
                .setNeutralButton("Clear All", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        }

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
                dialog.getListView().setItemChecked(i, false);
            }
            selectedGenresList.clear();
            selectedGenreFilter = "";
        });
    }
    private void showLanguageFilterDialog() {
        // Беремо мови ТІЛЬКИ цієї колекції
        List<String> allLanguages = dbHelper.getLanguagesInCollection(collectionId);

        if (allLanguages.isEmpty()) {
            Toast.makeText(this, "No languages found in this collection", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] languagesArray = allLanguages.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allLanguages.size()];

        for (int i = 0; i < allLanguages.size(); i++) {
            if (selectedLanguagesList.contains(allLanguages.get(i))) {
                checkedItems[i] = true;
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Languages in " + collectionName)
                .setMultiChoiceItems(languagesArray, checkedItems, (d, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Filter", (d, which) -> {
                    selectedLanguagesList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedLanguagesList.add(languagesArray[i]);
                    }
                    selectedLanguageFilter = selectedLanguagesList.isEmpty() ? "" : String.join("|", selectedLanguagesList);
                    applyAndLoad();
                })
                .setNeutralButton("Clear All", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        }

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
                dialog.getListView().setItemChecked(i, false);
            }
            selectedLanguagesList.clear();
            selectedLanguageFilter = "";
        });
    }
    private void showPlatformFilterDialog() {
        // Беремо платформи ТІЛЬКИ цієї колекції
        List<String> allPlatforms = dbHelper.getPlatformsInCollection(collectionId);

        if (allPlatforms.isEmpty()) {
            Toast.makeText(this, "No platforms found in this collection", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] platformsArray = allPlatforms.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allPlatforms.size()];

        for (int i = 0; i < allPlatforms.size(); i++) {
            if (selectedPlatformsList.contains(allPlatforms.get(i))) {
                checkedItems[i] = true;
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Platforms in " + collectionName)
                .setMultiChoiceItems(platformsArray, checkedItems, (d, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Filter", (d, which) -> {
                    selectedPlatformsList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedPlatformsList.add(platformsArray[i]);
                    }
                    selectedPlatformFilter = selectedPlatformsList.isEmpty() ? "" : String.join("|", selectedPlatformsList);
                    applyAndLoad();
                })
                .setNeutralButton("Clear All", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        }

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
                dialog.getListView().setItemChecked(i, false);
            }
            selectedPlatformsList.clear();
            selectedPlatformFilter = "";
        });
    }
    private void showTagFilterDialog() {
        List<String> allTags = dbHelper.getTagsInCollection(collectionId);
        if (allTags.isEmpty()) {
            Toast.makeText(this, "No tags found in this collection", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> tempSelected = new ArrayList<>(selectedTagsList);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_list, null);
        androidx.appcompat.widget.SearchView searchView = dialogView.findViewById(R.id.searchViewTags);
        RecyclerView rv = dialogView.findViewById(R.id.rvTags);

        FilterSelectionAdapter tagAdapter = new FilterSelectionAdapter(allTags, tempSelected);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(tagAdapter);

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                tagAdapter.filter(newText);
                return true;
            }
        });

        // 1. Створюємо діалог
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Tags in " + collectionName)
                .setView(dialogView)
                .setPositiveButton("Filter", (d, which) -> {
                    selectedTagsList.clear();
                    selectedTagsList.addAll(tempSelected);
                    selectedTagFilter = selectedTagsList.isEmpty() ? "" : String.join("|", selectedTagsList);
                    applyAndLoad();
                })
                .setNeutralButton("Clear All", (d, which) -> {
                    selectedTagsList.clear();
                    selectedTagFilter = "";
                    applyAndLoad();
                })
                .setNegativeButton("Cancel", null)
                .create(); // Використовуємо create() замість show()

        // 2. ВСТАНОВЛЮЄМО ВАШ БЕКГРАУНД (це те, що зникло)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        }

        // 3. Показуємо
        dialog.show();
    }
    public class SelectableGame {
        public Game game;
        public boolean isSelected;

        public SelectableGame(Game game, boolean isSelected) {
            this.game = game;
            this.isSelected = isSelected;
        }
    }
    private class FilterSelectionAdapter extends RecyclerView.Adapter<FilterSelectionAdapter.ViewHolder> {
        private List<String> allItems;
        private List<String> filteredItems;
        private List<String> selected;

        FilterSelectionAdapter(List<String> items, List<String> currentlySelected) {
            this.allItems = new ArrayList<>(items);
            this.filteredItems = new ArrayList<>(items);
            this.selected = currentlySelected;
        }

        void filter(String query) {
            filteredItems.clear();
            if (query.isEmpty()) {
                filteredItems.addAll(allItems);
            } else {
                for (String item : allItems) {
                    if (item.toLowerCase().contains(query.toLowerCase())) {
                        filteredItems.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = filteredItems.get(position);
            android.widget.CheckedTextView ctv = (android.widget.CheckedTextView) holder.itemView;
            ctv.setText(item);
            ctv.setChecked(selected.contains(item));

            holder.itemView.setOnClickListener(v -> {
                if (selected.contains(item)) {
                    selected.remove(item);
                } else {
                    selected.add(item);
                }
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() { return filteredItems.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) { super(v); }
        }
    }
}


