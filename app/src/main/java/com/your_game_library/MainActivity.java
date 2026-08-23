package com.your_game_library;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton fabMain;
    Button buttonPlanned, buttonPlaying, buttonCompleted;
    SearchView searchView;
    GameDatabaseHelper dbHelper;
    TextView tvPlannedCount, tvPlayingCount, tvCompletedCount;
    View btnFilters;
    String currentCategory = "playing";
    private String currentSortCriteria = "id DESC";
    private boolean isAscending = false;
    private String selectedGenreFilter = "";
    private List<String> selectedGenresList = new ArrayList<>();
    private String currentSortColumn = "id";
    private List<String> selectedTagsList = new ArrayList<>();
    private String selectedTagFilter = "";
    private List<String> selectedPlatformsList = new ArrayList<>();
    private String selectedPlatformFilter = "";
    private List<String> selectedLanguagesList = new ArrayList<>();
    private String selectedLanguageFilter = "";

    private static final String PREF_NAME = "app_settings";
    private static final String KEY_SORT_COLUMN = "sort_column";
    private static final String KEY_IS_ASCENDING = "is_ascending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. UI Elements Initialization
        fabMain = findViewById(R.id.fabMain);
        buttonPlanned = findViewById(R.id.buttonPlanned);
        buttonPlaying = findViewById(R.id.buttonPlaying);
        buttonCompleted = findViewById(R.id.buttonCompleted);
        searchView = findViewById(R.id.searchView);
        tvPlannedCount = findViewById(R.id.tvPlannedCount);
        tvPlayingCount = findViewById(R.id.tvPlayingCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        btnFilters = findViewById(R.id.btnFilters);
        FloatingActionButton fabUp = findViewById(R.id.fabScrollToTop);

        dbHelper = GameDatabaseHelper.getInstance(this);

        // 2. Load settings
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentSortColumn = prefs.getString(KEY_SORT_COLUMN, "id");
        isAscending = prefs.getBoolean(KEY_IS_ASCENDING, false);
        updateSortCriteria();

        // 3. ViewPager2 setup
// Inside onCreate():
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        GamePagerAdapter pagerAdapter = new GamePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        viewPager.setUserInputEnabled(true);
// Keeps all 3 fragments (0, 1, 2) loaded in memory so swiping doesn't reload them
        viewPager.setOffscreenPageLimit(2);

        // 4. Connect category buttons with ViewPager
        buttonPlaying.setOnClickListener(v -> viewPager.setCurrentItem(0));
        buttonPlanned.setOnClickListener(v -> viewPager.setCurrentItem(1));
        buttonCompleted.setOnClickListener(v -> viewPager.setCurrentItem(2));
        updateWidget();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                switch (position) {
                    case 0: currentCategory = "playing"; break;
                    case 1: currentCategory = "planned"; break;
                    case 2: currentCategory = "completed"; break;
                }

                updateButtonColors(currentCategory);
                updateGameCount();
                fabUp.hide();

                String currentQuery = getCurrentSearchQuery();
                if (!currentQuery.isEmpty()) {
                    filterGames(currentQuery);
                }
            }
        });

        // 6. Scroll to top button
        fabUp.setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
            if (currentFragment instanceof GameListFragment) {
                ((GameListFragment) currentFragment).scrollToTop();
            }
            fabUp.hide();
        });

        // 7. Systems setup
        setupFab();
        setupSearchLogic();
        setupFilterButton();

        // 8. StatusBar
        getWindow().setStatusBarColor(Color.parseColor("#121212"));
    }

    public String getCurrentSearchQuery() {
        if (searchView != null && searchView.getQuery() != null) {
            return searchView.getQuery().toString().trim();
        }
        return "";
    }

    TransitionSet createTransition() {
        return new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new Fade())
                .setDuration(400)
                .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
    }

    private void setupSearchLogic() {
        final View filtersBtn = findViewById(R.id.btnFilters);
        final View counterContainer = findViewById(R.id.counterContainer);
        final ConstraintLayout topBarLayout = findViewById(R.id.topBarConstraintLayout);

        final EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        final ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        final ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);

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

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionManager.beginDelayedTransition(topBarLayout, createTransition());

            if (hasFocus) {
                filtersBtn.setVisibility(View.GONE);
                counterContainer.setVisibility(View.GONE);

                if (closeButton != null) {
                    closeButton.post(() -> closeButton.setVisibility(View.VISIBLE));
                }
            } else {
                if (searchView.getQuery().length() == 0) {
                    filtersBtn.setVisibility(View.VISIBLE);
                    counterContainer.setVisibility(View.VISIBLE);
                    if (closeButton != null) closeButton.setVisibility(View.GONE);
                }
            }
        });

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                if (searchView.getQuery().length() > 0) {
                    searchView.setQuery("", false);
                    closeButton.post(() -> closeButton.setVisibility(View.VISIBLE));
                } else {
                    searchEditText.clearFocus();

                    TransitionManager.beginDelayedTransition(topBarLayout, createTransition());
                    filtersBtn.setVisibility(View.VISIBLE);
                    counterContainer.setVisibility(View.VISIBLE);
                    closeButton.setVisibility(View.GONE);

                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    filterGames("");
                }
            });
        }

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

    private void setupFilterButton() {

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
                                clearAllFilters();
                            } else {
                                currentSortColumn = sortColumn;
                                isAscending = ascending;
                            }

                            applyAndLoad();
                        }

                        @Override public void onOpenGenreDialog(FilterBottomSheet sheet) { showGenreFilterDialog(sheet); }
                        @Override public void onOpenTagDialog(FilterBottomSheet sheet) { showTagFilterDialog(sheet); }
                        @Override public void onOpenPlatformDialog(FilterBottomSheet sheet) { showPlatformFilterDialog(sheet); }
                        @Override public void onOpenLanguageDialog(FilterBottomSheet sheet) { showLanguageFilterDialog(sheet); }
                    }
            );
            sheet.show(getSupportFragmentManager(), "filter_sheet");
        });
    }

    private String getSortLabelByColumn(String column) {
        if (column == null) return "Default";
        switch (column) {
            case "name": return "Name";
            case "released": return "Year";
            case "rating": return "Rating";
            case "priority": return "Priority";
            case "date_added": return "Date Added";
            case "date_started": return "Date Started";
            case "time_spent": return "Time Spent";
            case "date_completed": return "Finished Date";
            case "playthroughs": return "Playthroughs";
            case "comp_type": return "Comp. Type";
            case "price": return "Price";
            case "discount": return "Discount";
            case "id":
            default:
                return "Default";
        }
    }

    private void applyAndLoad() {
        updateSortCriteria();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_SORT_COLUMN, currentSortColumn)
                .putBoolean(KEY_IS_ASCENDING, isAscending)
                .apply();

        for (int i = 0; i < 3; i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag("f" + i);
            if (f instanceof GameListFragment) {
                ((GameListFragment) f).refreshData(
                        currentSortCriteria, selectedGenreFilter,
                        selectedTagFilter, selectedPlatformFilter,
                        selectedLanguageFilter, true
                );
            }
        }
        updateGameCount();
    }

    private void showGenreFilterDialog(FilterBottomSheet sheet) {
        List<String> allGenres = dbHelper.getAllUniqueGenres();
        String[] genresArray = allGenres.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allGenres.size()];

        for (int i = 0; i < allGenres.size(); i++) {
            if (selectedGenresList.contains(allGenres.get(i))) checkedItems[i] = true;
        }

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
                .setTitle("Select Genres")
                .setMultiChoiceItems(genresArray, checkedItems, (d, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("Select", (d, which) -> {
                    selectedGenresList.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selectedGenresList.add(genresArray[i]);
                    }
                    selectedGenreFilter = selectedGenresList.isEmpty() ? "" : String.join("|", selectedGenresList);
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNeutralButton("Clear All", (d, which) -> {
                    selectedGenresList.clear();
                    selectedGenreFilter = "";
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();
    }

    private void showTagFilterDialog(FilterBottomSheet sheet) {
        List<String> allTags = dbHelper.getAllUniqueTags();
        List<String> tempSelectedTags = new ArrayList<>(selectedTagsList);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchable_list, null);
        SearchView sv = dialogView.findViewById(R.id.searchViewTags);
        RecyclerView rv = dialogView.findViewById(R.id.rvTags);

        TagSelectionAdapter tagAdapter = new TagSelectionAdapter(allTags, tempSelectedTags);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(tagAdapter);

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                tagAdapter.filter(newText);
                return true;
            }
        });

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
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

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
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
                .setNeutralButton("Clear All", (d, which) -> {
                    selectedPlatformsList.clear();
                    selectedPlatformFilter = "";
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();
    }

    private void showLanguageFilterDialog(FilterBottomSheet sheet) {
        List<String> allLanguages = dbHelper.getAllUniqueLanguages();
        String[] languagesArray = allLanguages.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allLanguages.size()];

        for (int i = 0; i < allLanguages.size(); i++) {
            if (selectedLanguagesList.contains(allLanguages.get(i))) checkedItems[i] = true;
        }

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.MyDialogTheme)
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
                .setNeutralButton("Clear All", (d, which) -> {
                    selectedLanguagesList.clear();
                    selectedLanguageFilter = "";
                    if (sheet != null) sheet.updateSummaryLabels();
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        dialog.show();
    }

    private void clearAllFilters() {
        selectedGenresList.clear();
        selectedTagsList.clear();
        selectedPlatformsList.clear();
        selectedLanguagesList.clear();
        selectedGenreFilter = "";
        selectedTagFilter = "";
        selectedPlatformFilter = "";
        selectedLanguageFilter = "";
    }

    private void setupFab() {
        FloatingActionButton fabMain = findViewById(R.id.fabMain);
        fabMain.setOnClickListener(v -> showMenuBottomSheet());
    }

    private void showMenuBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_main_menu_sheet, null);

        sheetView.findViewById(R.id.menuAddGame).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, AddGameActivity.class));
        });

        sheetView.findViewById(R.id.menuExplore).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(this, ExploreActivity.class));
        });

        sheetView.findViewById(R.id.menuRandom).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(this, RandomPickerActivity.class));
        });

        sheetView.findViewById(R.id.menuSettings).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        sheetView.findViewById(R.id.menuCollections).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(MainActivity.this, CollectionsActivity.class));
        });

        sheetView.findViewById(R.id.menuStats).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(this, StatisticsActivity.class));
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void updateSortCriteria() {
        String direction = isAscending ? " ASC" : " DESC";
        currentSortCriteria = currentSortColumn + direction;
    }

    private void updateButtonColors(String activeCategory) {
        int activeColor = Color.parseColor("#58A870");
        int inactiveColor = Color.parseColor("#2A2A2A");
        int plannedColor = Color.parseColor("#2D5E85");
        int completedColor = Color.parseColor("#fc6f03");

        buttonPlaying.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
        buttonPlanned.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));
        buttonCompleted.setBackgroundTintList(ColorStateList.valueOf(inactiveColor));

        switch (activeCategory) {
            case "playing":
                buttonPlaying.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                break;
            case "planned":
                buttonPlanned.setBackgroundTintList(ColorStateList.valueOf(plannedColor));
                break;
            case "completed":
                buttonCompleted.setBackgroundTintList(ColorStateList.valueOf(completedColor));
                break;
        }
    }

    public String getSelectedGenreFilter() {
        return selectedGenreFilter != null ? selectedGenreFilter : "";
    }

    public String getSelectedTagFilter() {
        return selectedTagFilter != null ? selectedTagFilter : "";
    }

    public String getSelectedPlatformFilter() {
        return selectedPlatformFilter != null ? selectedPlatformFilter : "";
    }

    public String getSelectedLanguageFilter() {
        return selectedLanguageFilter != null ? selectedLanguageFilter : "";
    }

    private void filterGames(String query) {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        if (viewPager == null) return;

        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());

        if (currentFragment instanceof GameListFragment) {
            ((GameListFragment) currentFragment).filter(query.trim());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentSortColumn = prefs.getString(KEY_SORT_COLUMN, "id");
        isAscending = prefs.getBoolean(KEY_IS_ASCENDING, false);
        updateSortCriteria();
        updateGameCount();

        String currentQuery = getCurrentSearchQuery();
        if (!currentQuery.isEmpty()) {
            findViewById(R.id.btnFilters).setVisibility(View.GONE);
            findViewById(R.id.counterContainer).setVisibility(View.GONE);
            ImageView closeBtn = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeBtn != null) closeBtn.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.btnFilters).setVisibility(View.VISIBLE);
            findViewById(R.id.counterContainer).setVisibility(View.VISIBLE);
        }
    }

    private void updateGameCount() {
        int count = dbHelper.getGameCount(
                currentCategory,
                currentSortCriteria,
                selectedGenreFilter,
                selectedTagFilter,
                selectedPlatformFilter,
                selectedLanguageFilter
        );

        if (currentCategory.equals("playing")) {
            tvPlayingCount.setText(String.valueOf(count));
            tvPlayingCount.setVisibility(View.VISIBLE);
            tvPlannedCount.setVisibility(View.GONE);
            tvCompletedCount.setVisibility(View.GONE);
        } else if (currentCategory.equals("planned")) {
            tvPlannedCount.setText(String.valueOf(count));
            tvPlannedCount.setVisibility(View.VISIBLE);
            tvPlayingCount.setVisibility(View.GONE);
            tvCompletedCount.setVisibility(View.GONE);
        } else if (currentCategory.equals("completed")) {
            tvCompletedCount.setText(String.valueOf(count));
            tvCompletedCount.setVisibility(View.VISIBLE);
            tvPlayingCount.setVisibility(View.GONE);
            tvPlannedCount.setVisibility(View.GONE);
        }
    }

    private void updateWidget() {
        Intent intent = new Intent(this, StatsWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), StatsWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    static class TagSelectionAdapter extends RecyclerView.Adapter<TagSelectionAdapter.ViewHolder> {
        private List<String> allTags;
        private List<String> filteredTags;
        private List<String> selected;

        TagSelectionAdapter(List<String> tags, List<String> currentlySelected) {
            this.allTags = new ArrayList<>(tags);
            this.filteredTags = new ArrayList<>(tags);
            this.selected = currentlySelected;
        }

        void filter(String query) {
            filteredTags.clear();
            if (query.isEmpty()) {
                filteredTags.addAll(allTags);
            } else {
                for (String tag : allTags) {
                    if (tag.toLowerCase().contains(query.toLowerCase())) {
                        filteredTags.add(tag);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tag_filter, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String tag = filteredTags.get(position);
            holder.text.setText(tag);

            // FIX: Prevent CheckBox from intercepting touch events without updating 'selected'
            holder.checkBox.setClickable(false);
            holder.checkBox.setFocusable(false);
            holder.checkBox.setChecked(selected.contains(tag));

            holder.itemView.setOnClickListener(v -> {
                if (selected.contains(tag)) {
                    selected.remove(tag);
                    holder.checkBox.setChecked(false);
                } else {
                    selected.add(tag);
                    holder.checkBox.setChecked(true);
                }
            });
        }

        @Override
        public int getItemCount() { return filteredTags.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            com.google.android.material.checkbox.MaterialCheckBox checkBox;

            ViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.tvTagName);
                checkBox = v.findViewById(R.id.cbTag);
            }
        }
    }

    private class GamePagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public GamePagerAdapter(@NonNull androidx.fragment.app.FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return GameListFragment.newInstance("playing");
                case 1: return GameListFragment.newInstance("planned");
                case 2: return GameListFragment.newInstance("completed");
                default: return GameListFragment.newInstance("playing");
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}