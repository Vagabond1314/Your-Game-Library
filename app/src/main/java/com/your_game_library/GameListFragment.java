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
    private FloatingActionButton fabUp;
    private List<Game> fullListForSearch = new ArrayList<>();
    private String currentSortCriteria = "id DESC";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fabUp = getActivity().findViewById(R.id.fabScrollToTop);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    int firstVisible = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();

                    if (getUserVisibleHint() || isVisible()) {
                        if (firstVisible > 5) fabUp.show();
                        else fabUp.hide();
                    }
                }
            }
        });
    }

    public void refreshData(String sortCriteria, String genre, String tag, String plat, String lang, boolean isSortingChanged) {
        if (dbHelper == null || adapter == null || recyclerView == null) return;
        this.currentSortCriteria = sortCriteria;

        new Thread(() -> {
            List<Game> games = dbHelper.getGamesByCategorySorted(category, sortCriteria, genre, tag, plat, lang);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                fullListForSearch = new ArrayList<>(games);

                String currentQuery = "";
                if (getActivity() instanceof MainActivity) {
                    currentQuery = ((MainActivity) getActivity()).getCurrentSearchQuery();
                }

                List<Game> listToDisplay;

                if (!currentQuery.isEmpty()) {
                    listToDisplay = new ArrayList<>();
                    for (Game game : fullListForSearch) {
                        if (game.getName() != null && game.getName().toLowerCase().contains(currentQuery.toLowerCase())) {
                            listToDisplay.add(game);
                        }
                    }
                } else {
                    listToDisplay = games;
                }

                adapter.submitList(listToDisplay, () -> {
                    if (isSortingChanged && recyclerView != null) {
                        recyclerView.scrollToPosition(0);
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
        loadDataFromPrefs();
    }

    public void loadDataFromPrefs() {
        if (getContext() == null || dbHelper == null || category == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        boolean isGrid = prefs.getBoolean("is_main_grid", false);
        String col = prefs.getString("sort_column", "id");
        boolean asc = prefs.getBoolean("is_ascending", false);
        String sortCriteria = col + (asc ? " ASC" : " DESC");

        updateLayout(isGrid);

        String genre = "", tag = "", plat = "", lang = "";
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            genre = activity.getSelectedGenreFilter();
            tag = activity.getSelectedTagFilter();
            plat = activity.getSelectedPlatformFilter();
            lang = activity.getSelectedLanguageFilter();
        }

        refreshData(sortCriteria, genre, tag, plat, lang, false);
    }

    private void updateLayout(boolean isGrid) {
        if (recyclerView == null) return;
        RecyclerView.LayoutManager current = recyclerView.getLayoutManager();

        if (isGrid) {
            if (!(current instanceof GridLayoutManager)) {
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                if (adapter != null) adapter.setGrid(true);
            }
        } else {
            // FIX: Only replace LayoutManager if switching from Grid or if current is null
            if (current instanceof GridLayoutManager || !(current instanceof LinearLayoutManager)) {
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                if (adapter != null) adapter.setGrid(false);
            }
        }
    }

    private void setupRecyclerView() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isGrid = prefs.getBoolean("is_main_grid", false);

        if (isGrid) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        adapter = new GameAdapter(requireContext(), new ArrayList<>(), dbHelper, isGrid);
        recyclerView.setAdapter(adapter);
    }

    public void loadData() {
        if (dbHelper == null || category == null) return;

        List<Game> games = dbHelper.getGamesByCategoryObject(category);
        fullListForSearch = new ArrayList<>(games);

        if (adapter != null) {
            adapter.submitList(games, null);
        }
    }

    public void filter(String query) {
        if (adapter == null) return;

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