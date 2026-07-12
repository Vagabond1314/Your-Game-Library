package com.your_game_library;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GameListFragment extends Fragment {
    private String category;
    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private GameDatabaseHelper dbHelper;
    // Додай ці змінні у GameListFragment
    private FloatingActionButton fabUp;
    private List<Game> fullListForSearch = new ArrayList<>();
    private String currentSortCriteria = "id DESC";
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Знаходимо кнопку FAB Up, яка лежить в Activity
        fabUp = getActivity().findViewById(R.id.fabScrollToTop);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    int firstVisible = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();

                    // Тільки активний фрагмент має керувати кнопкою
                    if (getUserVisibleHint() || isVisible()) {
                        if (firstVisible > 5) fabUp.show();
                        else fabUp.hide();
                    }
                }
            }
        });
    }

    // Покращений метод для усунення лагів при сортуванні
    public void refreshData(String sortCriteria, String genre, String tag, String plat, String lang, boolean isSortingChanged) {
        if (dbHelper == null || adapter == null || recyclerView == null) return;

        final android.os.Parcelable savedState = (recyclerView.getLayoutManager() != null && !isSortingChanged)
                ? recyclerView.getLayoutManager().onSaveInstanceState()
                : null;
        this.currentSortCriteria = sortCriteria;

        new Thread(() -> {
            // Отримуємо ПОВНІ дані з бази
            List<Game> games = dbHelper.getGamesByCategorySorted(category, sortCriteria, genre, tag, plat, lang);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                // Оновлюємо базовий список для пошуку
                fullListForSearch = new ArrayList<>(games);

                // --- МАГІЯ: ПЕРЕВІРЯЄМО, ЧИ Є АКТИВНИЙ ПОШУК ---
                String currentQuery = "";
                if (getActivity() instanceof MainActivity) {
                    currentQuery = ((MainActivity) getActivity()).getCurrentSearchQuery();
                }

                List<Game> listToDisplay;

                if (!currentQuery.isEmpty()) {
                    // Якщо в пошуку є текст, фільтруємо свіжі дані ПЕРЕД відправкою в адаптер!
                    listToDisplay = new ArrayList<>();
                    for (Game game : fullListForSearch) {
                        if (game.getName() != null && game.getName().toLowerCase().contains(currentQuery.toLowerCase())) {
                            listToDisplay.add(game);
                        }
                    }
                } else {
                    // Якщо пошук порожній, показуємо все
                    listToDisplay = games;
                }

                // Передаємо правильний список в адаптер
                adapter.submitList(listToDisplay, () -> {
                    if (isSortingChanged) {
                        recyclerView.scrollToPosition(0);
                    } else if (savedState != null) {
                        recyclerView.getLayoutManager().onRestoreInstanceState(savedState);
                    }
                });
            });
        }).start();
    }
    public static GameListFragment newInstance(String category) {
        GameListFragment fragment = new GameListFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // fragment_game_list.xml має містити RecyclerView з id: recyclerViewGames
        View v = inflater.inflate(R.layout.fragment_game_list, container, false);
        recyclerView = v.findViewById(R.id.recyclerViewGames);

        if (getArguments() != null) {
            category = getArguments().getString("category");
        }

        dbHelper = GameDatabaseHelper.getInstance(requireContext());

        setupRecyclerView();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Щоразу при появі вкладки беремо актуальне сортування з пам'яті
        loadDataFromPrefs();
    }
    public void loadDataFromPrefs() {
        if (getContext() == null || dbHelper == null || category == null) return;

        // 1. Отримуємо глобальні налаштування (Сортування та Вигляд)
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        boolean isGrid = prefs.getBoolean("is_main_grid", false);
        String col = prefs.getString("sort_column", "id");
        boolean asc = prefs.getBoolean("is_ascending", false);
        String sortCriteria = col + (asc ? " ASC" : " DESC");

        // 2. Оновлюємо LayoutManager (Grid/List) та стан адаптера
        updateLayout(isGrid);

        // 3. Отримуємо поточні фільтри з MainActivity
        // (Це дозволяє зберігати вибрані жанри/теги при перемиканні вкладок)
        String genre = "", tag = "", plat = "", lang = "";
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            genre = activity.getSelectedGenreFilter();
            tag = activity.getSelectedTagFilter();
            plat = activity.getSelectedPlatformFilter();
            lang = activity.getSelectedLanguageFilter();
        }

        // 4. Викликаємо refreshData, який зробить запит до БД в окремому потоці
        // false — означає не скидати скрол в 0 (зберігати позицію при свайпах)
        refreshData(sortCriteria, genre, tag, plat, lang, false);
    }

    private void updateLayout(boolean isGrid) {
        if (recyclerView == null) return;
        RecyclerView.LayoutManager current = recyclerView.getLayoutManager();

        if (isGrid && !(current instanceof GridLayoutManager)) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            if (adapter != null) adapter.setGrid(true);
        } else if (!isGrid && (current instanceof LinearLayoutManager && !(current instanceof GridLayoutManager))) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            if (adapter != null) adapter.setGrid(false);
        }
    }

    private void setupRecyclerView() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isGrid = prefs.getBoolean("is_main_grid", false);

        // Встановлюємо LayoutManager
        if (isGrid) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // Ініціалізуємо твій адаптер (передаємо isGrid)
        adapter = new GameAdapter(requireContext(), new ArrayList<>(), dbHelper, isGrid);
        recyclerView.setAdapter(adapter);
    }

    public void loadData() {
        if (dbHelper == null || category == null) return;

        // 1. Завантажуємо ігри з бази (БД сортує по дефолтних полях SQL)
        List<Game> games = dbHelper.getGamesByCategoryObject(category);

        fullListForSearch = new ArrayList<>(games); // Зберігаємо для пошуку вже відсортований список

        if (adapter != null) {
            adapter.submitList(games, null);
        }
    }
    public void filter(String query) {
        if (adapter == null) return;

        // ЗАХИСТ: Якщо список порожній (наприклад, після старту), пробуємо довантажити
        if (fullListForSearch.isEmpty()) {
            loadData();
        }

        if (query.isEmpty()) {
            adapter.submitList(new ArrayList<>(fullListForSearch), null);
            return;
        }

        List<Game> filtered = new ArrayList<>();
        for (Game game : fullListForSearch) {
            if (game.getName() != null && game.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(game);
            }
        }
        adapter.submitList(filtered, null);
    }

    public void scrollToTop() {
        if (recyclerView != null) recyclerView.scrollToPosition(0);
    }
}