package com.your_game_library;

import android.content.Context;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ExploreActivity extends AppCompatActivity {

    private static final String TAG = "IGDB_DEBUG";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvFilter;
    private SearchView searchView;

    private IgdbAdapter adapter;
    private IgdbApiService igdbApi;
    private TwitchAuthApiService twitchAuthApi; // Separate Retrofit for Twitch Token!
    private String accessToken = null;

    private List<IgdbGame> games = new ArrayList<>();
    private boolean isLoading = false;
    private int currentOffset = 0;
    private final int limit = 20;

    private boolean searchMode = false;
    private String currentSearchQuery = "";
    private int currentFilterIndex = 0;

    private MaterialButton btnToggleLayout;
    private boolean isGridView = false;

    private List<Integer> selectedGenreIds = new ArrayList<>();
    private List<Integer> selectedThemeIds = new ArrayList<>();
    private List<Integer> selectedPlatformIds = new ArrayList<>();
    private List<Integer> selectedLanguageIds = new ArrayList<>();
    private int selectedYear = -1;

    private static final String PREF_NAME = "explore_prefs";
    private static final String KEY_IS_GRID = "is_grid_view";

    private List<String> selectedGenreNames = new ArrayList<>();
    private List<String> selectedThemeNames = new ArrayList<>();
    private List<String> selectedPlatformNames = new ArrayList<>();
    private List<String> selectedLanguageNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        initViews();
        initRetrofit();
        setupFiltersMenu();
        setupScrollListener();
        fetchTokenAndLoadInitialGames();
        setupSearchLogic();

    }

    private void initViews() {
        // 1. Знаходимо всі View
        recyclerView = findViewById(R.id.recyclerRawg);
        progressBar = findViewById(R.id.progressRawg);
        tvFilter = findViewById(R.id.spinnerFilter);
        searchView = findViewById(R.id.searchView);
        btnToggleLayout = findViewById(R.id.btnToggleLayout);
        FloatingActionButton fabUp = findViewById(R.id.fabScrollToTop);
        ConstraintLayout topBarLayout = findViewById(R.id.topBarConstraintLayout);

        // 2. Налаштування Toolbar
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#121212"));
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Explore Games");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 3. Стилізація фільтра
        tvFilter.setTextSize(18);
        tvFilter.setGravity(android.view.Gravity.CENTER);
        tvFilter.setPadding(0, 0, 0, 0);

        // 4. Завантаження налаштувань та встановлення Layout
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isGridView = prefs.getBoolean(KEY_IS_GRID, false);

        // ВАЖЛИВО: Адаптер створюємо один раз, а в методі update зміним менеджер
        games = new ArrayList<>();
        adapter = new IgdbAdapter(this, games, isGridView ? IgdbAdapter.TYPE_GRID : IgdbAdapter.TYPE_LIST);
        recyclerView.setAdapter(adapter);

        updateRecyclerViewLayout(); // Тут налаштується правильний LayoutManager

        // 5. Обробка перемикача Layout
        btnToggleLayout.setOnClickListener(v -> {
            isGridView = !isGridView;
            prefs.edit().putBoolean(KEY_IS_GRID, isGridView).apply();
            updateRecyclerViewLayout();
        });

        // 6. Кнопка "Вгору"
        fabUp.setOnClickListener(v -> {
            recyclerView.scrollToPosition(0);
        });


        // 8. Scroll Listener (використовуємо rv.getLayoutManager() для надійності)
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                // Завжди беремо поточний менеджер з RecyclerView
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    int firstVisible = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                    if (firstVisible > 5) {
                        if (fabUp.getVisibility() != View.VISIBLE) fabUp.show();
                    } else {
                        if (fabUp.getVisibility() == View.VISIBLE) fabUp.hide();
                    }
                }
            }
        });
    }
    private void setupSearchLogic() {
        final View filterBtn = findViewById(R.id.spinnerFilter);
        final View toggleBtn = findViewById(R.id.btnToggleLayout);
        final EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        final ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        final ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        final ConstraintLayout topBarLayout = findViewById(R.id.topBarConstraintLayout);

        // 1. АКТИВАЦІЯ ТА КЛАВІАТУРА
        View.OnClickListener startSearch = v -> {
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        };

        searchView.setOnClickListener(startSearch);
        searchEditText.setOnClickListener(startSearch);
        if (searchIcon != null) searchIcon.setOnClickListener(startSearch);

        // 2. АНІМАЦІЯ ТА ГАРАНТОВАНА ПОЯВА ХРЕСТИКА
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            TransitionSet set = new TransitionSet()
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
                    .setDuration(300);
            TransitionManager.beginDelayedTransition(topBarLayout, set);

            if (hasFocus) {
                filterBtn.setVisibility(View.GONE);
                toggleBtn.setVisibility(View.GONE);

                // ВАЖЛИВО: Використовуємо post, щоб Android не встиг сховати кнопку відразу після фокусу
                if (closeButton != null) {
                    closeButton.post(() -> closeButton.setVisibility(View.VISIBLE));
                }
            } else {
                if (searchView.getQuery().length() == 0) {
                    filterBtn.setVisibility(View.VISIBLE);
                    toggleBtn.setVisibility(View.VISIBLE);
                    if (closeButton != null) closeButton.setVisibility(View.GONE);
                }
            }
        });

        // 3. ПІДТРИМКА ВИДИМОСТІ ХРЕСТИКА ПРИ ЗМІНІ ТЕКСТУ
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Щоразу, коли текст міняється (навіть на порожній), змушуємо хрестик бути видимим
                if (closeButton != null && searchEditText.hasFocus()) {
                    closeButton.setVisibility(View.VISIBLE);
                }

                if (newText.length() > 2) performSearch(newText);
                else if (newText.isEmpty() && searchMode) {
                    searchMode = false;
                    fetchGames(false);
                }
                return false;
            }
        });

        // 4. ЛОГІКА ХРЕСТИКА (Очищення або Закриття)
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                if (searchView.getQuery().length() > 0) {
                    searchView.setQuery("", false);
                    // Після очищення Android захоче сховати кнопку, ми її повертаємо:
                    closeButton.post(() -> closeButton.setVisibility(View.VISIBLE));
                } else {
                    // ЗАКРИТТЯ ПОШУКУ (коли поле вже порожнє)
                    searchMode = false;
                    currentSearchQuery = "";
                    searchEditText.clearFocus();

                    TransitionManager.beginDelayedTransition(topBarLayout, new Fade().setDuration(250));
                    filterBtn.setVisibility(View.VISIBLE);
                    toggleBtn.setVisibility(View.VISIBLE);
                    closeButton.setVisibility(View.GONE);

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    fetchGames(false); // Повертаємо Trending
                }
            });
        }
    }
    private void updateRecyclerViewLayout() {
        if (isGridView) {
            // Встановлюємо сітку
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
            adapter = new IgdbAdapter(this, games, IgdbAdapter.TYPE_GRID);

            // Якщо зараз СІТКА, то кнопка має показувати іконку СПИСКУ (як натяк на дію)
            btnToggleLayout.setIconResource(R.drawable.explore_list);
        } else {
            // Встановлюємо список
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new IgdbAdapter(this, games, IgdbAdapter.TYPE_LIST);

            // Якщо зараз СПИСОК, то кнопка має показувати іконку СІТКИ
            btnToggleLayout.setIconResource(R.drawable.explore_grid);
        }

        recyclerView.setAdapter(adapter);
        // notifyDataSetChanged() вже не обов'язковий, бо setAdapter перемальовує все
    }
    private void setupFiltersMenu() {
        tvFilter.setOnClickListener(v -> {
            ExploreFilterBottomSheet sheet = new ExploreFilterBottomSheet(
                    currentFilterIndex, selectedYear,
                    selectedGenreIds, selectedThemeIds, selectedPlatformIds, selectedLanguageIds,
                    new ExploreFilterBottomSheet.ExploreFilterListener() {
                        @Override
                        public void onApply(int index, int year, boolean reset) {
                            if (reset) {
                                resetAllFilters();
                                currentFilterIndex = 0;
                                selectedYear = -1;
                            } else {
                                currentFilterIndex = index;
                                selectedYear = year;
                            }
                            currentOffset = 0;
                            fetchGames(false);
                        }

                        @Override public void onOpenGenres(ExploreFilterBottomSheet s) { showCategoryDialog(s); }
                        @Override public void onOpenPlatforms(ExploreFilterBottomSheet s) { showPlatformDialog(s); }
                        @Override public void onOpenLanguages(ExploreFilterBottomSheet s) { showLanguageDialog(s); }
                        @Override public void onOpenYear(ExploreFilterBottomSheet s) { showYearDialog(s); }
                    }
            );

            // --- ПЕРЕДАЄМО ЗБЕРЕЖЕНІ НАЗВИ У SHEET ПЕРЕД ПОКАЗОМ ---
            sheet.setGenreNames(selectedGenreNames);
            sheet.setThemeNames(selectedThemeNames);
            sheet.setPlatformNames(selectedPlatformNames);
            sheet.setLanguageNames(selectedLanguageNames);
            // (Кількість ігор та виклик sheet.updateLabels() відбудеться автоматично всередині onViewCreated у самому шторці)

            sheet.show(getSupportFragmentManager(), "explore_filter");
        });
    }
    private void showYearDialog(ExploreFilterBottomSheet sheet) {
        // 1. Генеруємо список років (від наступного року до 1970)
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        List<String> yearsList = new ArrayList<>();
        for (int i = currentYear + 1; i >= 1970; i--) {
            yearsList.add(String.valueOf(i));
        }
        String[] yearsArray = yearsList.toArray(new String[0]);

        // 2. Створюємо діалог
        new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Select Release Year")
                .setItems(yearsArray, (dialog, which) -> {
                    // Встановлюємо вибраний рік
                    selectedYear = Integer.parseInt(yearsArray[which]);

                    // Очищуємо інші конфліктні фільтри (опціонально, за логікою IGDB)
                    selectedGenreIds.clear();
                    selectedThemeIds.clear();
                    selectedPlatformIds.clear();
                    selectedLanguageIds.clear();

                    // Оновлюємо інтерфейс самого BottomSheet, якщо він відкритий
                    if (sheet != null) {
                        sheet.setSelectedYear(selectedYear);
                        sheet.updateLabels();
                    }
                })
                .setNeutralButton("Clear Year", (dialog, which) -> {
                    // Скидання року
                    selectedYear = -1;
                    if (sheet != null) {
                        sheet.setSelectedYear(-1);
                        sheet.updateLabels();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showLanguageDialog(ExploreFilterBottomSheet sheet) {
        progressBar.setVisibility(View.VISIBLE);
        igdbApi.getLanguages(getClientId(), accessToken, "fields name; limit 50; sort name asc;").enqueue(new Callback<List<IgdbNameModel>>() {
            @Override
            public void onResponse(Call<List<IgdbNameModel>> call, Response<List<IgdbNameModel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.body() == null) return;

                List<IgdbNameModel> list = response.body();
                String[] names = new String[list.size()];
                boolean[] checked = new boolean[list.size()];

                for (int i = 0; i < list.size(); i++) {
                    names[i] = list.get(i).name;
                    checked[i] = selectedLanguageIds.contains(list.get(i).id);
                }

                new AlertDialog.Builder(ExploreActivity.this, R.style.MyDialogTheme)
                        .setTitle("Select Languages")
                        .setMultiChoiceItems(names, checked, (d, which, isC) -> checked[which] = isC)
                        .setPositiveButton("Select", (d, w) -> {
                            selectedLanguageIds.clear();
                            selectedLanguageNames.clear(); // ДОДАТИ
                            List<String> lNames = new ArrayList<>();

                            for (int i = 0; i < checked.length; i++) {
                                if (checked[i]) {
                                    selectedLanguageIds.add(list.get(i).id);
                                    lNames.add(list.get(i).name);
                                    selectedLanguageNames.add(list.get(i).name); // Зберігаємо глобально
                                }
                            }

                            if (sheet != null) {
                                sheet.setLanguageNames(lNames);
                                sheet.updateLabels();
                            }
                        })
                        .setNeutralButton("Clear All", (d, w) -> {
                            selectedLanguageIds.clear();
                            if (sheet != null) sheet.updateLabels();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            @Override public void onFailure(Call<List<IgdbNameModel>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private String buildIgdbQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("fields name, cover.url, total_rating, first_release_date, summary, slug, total_rating_count, hypes; ");

        if (searchMode) {
            // Режим пошуку (ігнорує всі інші фільтри)
            sb.append("search \"").append(currentSearchQuery).append("\"; ");
        } else {
            long now = System.currentTimeMillis() / 1000;
            StringBuilder whereClause = new StringBuilder("where cover != null & themes != (42)");

            // --- 1. ДОДАЄМО РУЧНІ ФІЛЬТРИ (Tuning) ДО УМОВИ WHERE ---
            if (!selectedGenreIds.isEmpty()) {
                whereClause.append(" & genres = (").append(idsToString(selectedGenreIds)).append(")");
            }
            if (!selectedThemeIds.isEmpty()) {
                whereClause.append(" & themes = (").append(idsToString(selectedThemeIds)).append(")");
            }
            if (!selectedPlatformIds.isEmpty()) {
                whereClause.append(" & platforms = (").append(idsToString(selectedPlatformIds)).append(")");
            }
            if (!selectedLanguageIds.isEmpty()) {
                whereClause.append(" & language_supports.language = (").append(idsToString(selectedLanguageIds)).append(")");
            }
            if (selectedYear != -1) {
                long start = getYearTimestamp(selectedYear, true);
                long end = getYearTimestamp(selectedYear, false);
                whereClause.append(" & first_release_date >= ").append(start).append(" & first_release_date <= ").append(end);
            }

            // --- 2. ДОДАЄМО СПЕЦИФІЧНІ УМОВИ ТА СОРТУВАННЯ З DISCOVERY MODE ---
            String sorting = "";
            switch (currentFilterIndex) {
                case 0: // Trending (За останній місяць)
                    whereClause.append(" & first_release_date > ").append(now - 2592000); // 30 днів
                    sorting = "sort total_rating_count desc;";
                    break;
                case 1: // Popular (Взагалі)
                    whereClause.append(" & total_rating_count > 100");
                    sorting = "sort total_rating_count desc;";
                    break;
                case 2: // Highly Rated
                    whereClause.append(" & total_rating >= 85 & total_rating_count > 30");
                    sorting = "sort total_rating desc;";
                    break;
                case 3: // Newly Released (Останні 60 днів)
                    whereClause.append(" & first_release_date <= ").append(now)
                            .append(" & first_release_date > ").append(now - 5184000);
                    sorting = "sort first_release_date desc;";
                    break;
                case 4: // Upcoming
                    whereClause.append(" & first_release_date > ").append(now);
                    sorting = "sort hypes desc;";
                    break;
                default:
                    // Якщо вибрано якийсь неіснуючий індекс, просто сортуємо за популярністю
                    sorting = "sort total_rating_count desc;";
                    break;
            }

            // --- 3. ЗБИРАЄМО ВСЕ РАЗОМ ---
            sb.append(whereClause.toString()).append("; ").append(sorting);
        }

        // --- 4. ДОДАЄМО ПАГІНАЦІЮ ---
        sb.append(" limit ").append(limit).append("; offset ").append(currentOffset).append(";");

        return sb.toString().replace(";;", ";"); // Захист від подвійних крапок з комою
    }

    // --- Методи для Платформ, Пошуку, Токена та Пагінації залишаються без змін ---

    private void showPlatformDialog(ExploreFilterBottomSheet sheet) {
        progressBar.setVisibility(View.VISIBLE);
        // Сучасні платформи: PC, PS4, PS5, Xbox One, Xbox Series, Switch
        String query = "fields name; where id = (6, 48, 49, 130, 167, 169); sort name asc;";

        igdbApi.getPlatforms(getClientId(), accessToken, query).enqueue(new Callback<List<IgdbNameModel>>() {
            @Override
            public void onResponse(Call<List<IgdbNameModel>> call, Response<List<IgdbNameModel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.body() == null) return;

                List<IgdbNameModel> list = response.body();
                String[] names = new String[list.size()];
                boolean[] checked = new boolean[list.size()];

                for (int i = 0; i < list.size(); i++) {
                    names[i] = list.get(i).name;
                    checked[i] = selectedPlatformIds.contains(list.get(i).id);
                }

                new AlertDialog.Builder(ExploreActivity.this, R.style.MyDialogTheme)
                        .setTitle("Select Platforms")
                        .setMultiChoiceItems(names, checked, (d, which, isC) -> checked[which] = isC)
                        .setPositiveButton("Select", (d, w) -> {
                            selectedPlatformIds.clear();
                            selectedPlatformNames.clear(); // ДОДАТИ
                            List<String> pNames = new ArrayList<>();

                            for (int i = 0; i < checked.length; i++) {
                                if (checked[i]) {
                                    selectedPlatformIds.add(list.get(i).id);
                                    pNames.add(list.get(i).name);
                                    selectedPlatformNames.add(list.get(i).name); // Зберігаємо глобально
                                }
                            }

                            if (sheet != null) {
                                sheet.setPlatformNames(pNames);
                                sheet.updateLabels();
                            }
                        })
                        .setNeutralButton("Clear All", (d, w) -> {
                            selectedPlatformIds.clear();
                            if (sheet != null) sheet.updateLabels();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            @Override public void onFailure(Call<List<IgdbNameModel>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void showCategoryDialog(ExploreFilterBottomSheet sheet) {
        progressBar.setVisibility(View.VISIBLE);
        String query = "fields name; limit 50; sort name asc;";

        igdbApi.getGenres(getClientId(), accessToken, query).enqueue(new Callback<List<IgdbNameModel>>() {
            @Override
            public void onResponse(Call<List<IgdbNameModel>> call, Response<List<IgdbNameModel>> response) {
                if (response.body() == null) return;
                List<IgdbNameModel> allOptions = new ArrayList<>(response.body());

                igdbApi.getThemes(getClientId(), accessToken, query).enqueue(new Callback<List<IgdbNameModel>>() {
                    @Override
                    public void onResponse(Call<List<IgdbNameModel>> call, Response<List<IgdbNameModel>> resT) {
                        progressBar.setVisibility(View.GONE);
                        if (resT.body() != null) {
                            for (IgdbNameModel t : resT.body()) {
                                t.isTheme = true;
                                t.name = "[T] " + t.name;
                                allOptions.add(t);
                            }
                        }

                        boolean[] checked = new boolean[allOptions.size()];
                        String[] names = new String[allOptions.size()];
                        for (int i = 0; i < allOptions.size(); i++) {
                            names[i] = allOptions.get(i).name;
                            if (allOptions.get(i).isTheme)
                                checked[i] = selectedThemeIds.contains(allOptions.get(i).id);
                            else
                                checked[i] = selectedGenreIds.contains(allOptions.get(i).id);
                        }

                        new AlertDialog.Builder(ExploreActivity.this, R.style.MyDialogTheme)
                                .setTitle("Genres & Themes")
                                .setMultiChoiceItems(names, checked, (d, which, isC) -> checked[which] = isC)
                                .setPositiveButton("Select", (d, w) -> {
                                    selectedGenreIds.clear();
                                    selectedThemeIds.clear();
                                    selectedGenreNames.clear(); // ДОДАТИ
                                    selectedThemeNames.clear(); // ДОДАТИ

                                    List<String> gNames = new ArrayList<>();
                                    List<String> tNames = new ArrayList<>();

                                    for (int i = 0; i < checked.length; i++) {
                                        if (checked[i]) {
                                            IgdbNameModel item = allOptions.get(i);
                                            if (item.isTheme) {
                                                selectedThemeIds.add(item.id);
                                                String cleanName = item.name.replace("[T] ", "");
                                                tNames.add(cleanName);
                                                selectedThemeNames.add(cleanName); // Зберігаємо глобально
                                            } else {
                                                selectedGenreIds.add(item.id);
                                                gNames.add(item.name);
                                                selectedGenreNames.add(item.name); // Зберігаємо глобально
                                            }
                                        }
                                    }

                                    if (sheet != null) {
                                        sheet.setGenreNames(gNames);
                                        sheet.setThemeNames(tNames);
                                        sheet.updateLabels();
                                    }
                                })
                                .setNeutralButton("Clear All", (d, w) -> {
                                    selectedGenreIds.clear();
                                    selectedThemeIds.clear();
                                    if (sheet != null) sheet.updateLabels();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                    @Override public void onFailure(Call<List<IgdbNameModel>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
                });
            }
            @Override public void onFailure(Call<List<IgdbNameModel>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void resetAllFilters() {
        selectedGenreIds.clear();
        selectedThemeIds.clear();
        selectedPlatformIds.clear();
        selectedLanguageIds.clear();

        selectedGenreNames.clear();
        selectedThemeNames.clear();
        selectedPlatformNames.clear();
        selectedLanguageNames.clear();

        selectedYear = -1;
    }

    private String idsToString(List<Integer> list) {
        return list.toString().replace("[", "").replace("]", "").replace(" ", "");
    }

    private long getYearTimestamp(int year, boolean start) {
        Calendar cal = Calendar.getInstance();
        if (start) cal.set(year, 0, 1, 0, 0, 0);
        else cal.set(year, 11, 31, 23, 59, 59);
        return cal.getTimeInMillis() / 1000;
    }

    private void performSearch(String query) {
        searchMode = true; currentSearchQuery = query; currentOffset = 0; fetchGames(false);
    }

    private void initRetrofit() {
        // 1. IGDB API Base URL (Use "https://api.igdb.com/")
        Retrofit igdbRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.igdb.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        igdbApi = igdbRetrofit.create(IgdbApiService.class);

        // 2. Twitch Auth API Base URL
        Retrofit twitchRetrofit = new Retrofit.Builder()
                .baseUrl("https://id.twitch.tv/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        twitchAuthApi = twitchRetrofit.create(TwitchAuthApiService.class);
    }

    private String getClientSecret() {
        return Config.getIgdbClientSecret();
    }
    private void fetchTokenAndLoadInitialGames() {
        progressBar.setVisibility(View.VISIBLE);

        String clientId = getClientId().trim();
        String clientSecret = getClientSecret().trim();

        Log.d(TAG, "Client ID: " + clientId);
        Log.d(TAG, "DEBUG Client ID: [" + clientId + "]");
        Log.d(TAG, "DEBUG Client Secret length: " + clientSecret.length());
        twitchAuthApi.getToken(clientId, clientSecret, "client_credentials").enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    accessToken = "Bearer " + response.body().access_token;
                    Log.d(TAG, "Twitch Token obtained successfully!");
                    fetchGames(false);
                } else {
                    progressBar.setVisibility(View.GONE);
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "empty";
                        Log.e(TAG, "Failed to get Twitch Token. HTTP " + response.code() + " Error: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "HTTP Code: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Twitch Token Network Failure: ", t);
            }
        });
    }

    private void fetchGames(boolean isNextPage) {
        if (accessToken == null || isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        if (!isNextPage) {
            currentOffset = 0;
            games.clear();
            adapter.notifyDataSetChanged();
        }

        String clientId = Config.getIgdbClientId();

        igdbApi.getGames(clientId, accessToken, buildIgdbQuery()).enqueue(new Callback<List<IgdbGame>>() {
            @Override
            public void onResponse(Call<List<IgdbGame>> call, Response<List<IgdbGame>> response) {
                progressBar.setVisibility(View.GONE);
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Fetched games count: " + response.body().size());
                    games.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "IGDB Games API Error: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<IgdbGame>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                isLoading = false;
                Log.e(TAG, "IGDB Games Network Failure: ", t);
            }
        });
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (!isLoading && lm != null && lm.findLastVisibleItemPosition() >= games.size() - 5) {
                    currentOffset += limit; fetchGames(true);
                }
            }
        });
    }

    private String getClientId() {
        return Config.getIgdbClientId(); // Or Config.INSTANCE.getIgdbClientId() if Config is Kotlin
    }
}