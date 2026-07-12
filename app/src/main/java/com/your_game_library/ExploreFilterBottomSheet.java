package com.your_game_library;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ExploreFilterBottomSheet extends BottomSheetDialogFragment {

    public interface ExploreFilterListener {
        void onApply(int filterIndex, int year, boolean reset);
        void onOpenGenres(ExploreFilterBottomSheet sheet);
        void onOpenPlatforms(ExploreFilterBottomSheet sheet);
        void onOpenLanguages(ExploreFilterBottomSheet sheet);
        void onOpenYear(ExploreFilterBottomSheet sheet);
    }

    private final ExploreFilterListener listener;
    private int currentFilterIndex, selectedYear;
    private final List<Integer> genreIds, themeIds, platformIds, langIds;
    private TextView tvGen, tvPlat, tvLang, tvYear;

    public ExploreFilterBottomSheet(int index, int year, List<Integer> genres, List<Integer> themes,
                                    List<Integer> platforms, List<Integer> languages, ExploreFilterListener listener) {
        this.currentFilterIndex = index;
        this.selectedYear = year;
        this.genreIds = genres;
        this.themeIds = themes;
        this.platformIds = platforms;
        this.langIds = languages;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_explore_filter_sheet, container, false);

        ChipGroup cgMode = v.findViewById(R.id.cgExploreMode);
        tvGen = v.findViewById(R.id.tvExSelectedGenres);
        tvPlat = v.findViewById(R.id.tvExSelectedPlatforms);
        tvLang = v.findViewById(R.id.tvExSelectedLanguages);
        tvYear = v.findViewById(R.id.tvExSelectedYear);

        setupInitialState(cgMode);
        updateLabels();

        // Слухачі кнопок
        v.findViewById(R.id.btnExploreGenres).setOnClickListener(view -> listener.onOpenGenres(this));
        v.findViewById(R.id.btnExplorePlatforms).setOnClickListener(view -> listener.onOpenPlatforms(this));
        v.findViewById(R.id.btnExploreLanguages).setOnClickListener(view -> listener.onOpenLanguages(this));
        v.findViewById(R.id.btnExploreYear).setOnClickListener(view -> listener.onOpenYear(this));

        v.findViewById(R.id.btnResetExplore).setOnClickListener(view -> {
            listener.onApply(0, -1, true);
            dismiss();
        });

        v.findViewById(R.id.btnApplyExplore).setOnClickListener(view -> {
            int checkedId = cgMode.getCheckedChipId();
            if (checkedId == R.id.chipTrending) currentFilterIndex = 0;
            else if (checkedId == R.id.chipPopular) currentFilterIndex = 1;
            else if (checkedId == R.id.chipTopRated) currentFilterIndex = 2;
            else if (checkedId == R.id.chipNewReleases) currentFilterIndex = 3;
            else if (checkedId == R.id.chipAnticipated) currentFilterIndex = 4;

            // Якщо вибрані специфічні ID, індекс перемикається в buildIgdbQuery автоматично,
            // але для логіки UI ми передаємо поточний вибраний.
            listener.onApply(currentFilterIndex, selectedYear, false);
            dismiss();
        });

        return v;
    }
    @Override
    public void onStart() {
        super.onStart();
        // Змушуємо BottomSheet відкритися повністю відразу
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            com.google.android.material.bottomsheet.BottomSheetBehavior behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }
    private void setupInitialState(ChipGroup group) {
        if (currentFilterIndex == 0) group.check(R.id.chipTrending);
        else if (currentFilterIndex == 1) group.check(R.id.chipPopular);
        else if (currentFilterIndex == 2) group.check(R.id.chipTopRated);
        else if (currentFilterIndex == 3) group.check(R.id.chipNewReleases);
        else if (currentFilterIndex == 4) group.check(R.id.chipAnticipated);
    }

    // Додайте ці поля в клас ExploreFilterBottomSheet
    private List<String> genreNames = new ArrayList<>();
    private List<String> themeNames = new ArrayList<>();
    private List<String> platformNames = new ArrayList<>();
    private List<String> languageNames = new ArrayList<>();

    // Додайте сетери, щоб Activity могла передавати назви
    public void setGenreNames(List<String> names) { this.genreNames = names; }
    public void setThemeNames(List<String> names) { this.themeNames = names; }
    public void setPlatformNames(List<String> names) { this.platformNames = names; }
    public void setLanguageNames(List<String> names) { this.languageNames = names; }

    // Оновлений метод updateLabels
    public void updateLabels() {
        // Об'єднуємо назви жанрів та тем
        List<String> combinedGenres = new ArrayList<>(genreNames);
        combinedGenres.addAll(themeNames);

        setLabel(tvGen, formatList(combinedGenres), "All");
        setLabel(tvPlat, formatList(platformNames), "All");
        setLabel(tvLang, formatList(languageNames), "All");
        setLabel(tvYear, selectedYear != -1 ? String.valueOf(selectedYear) : "All Time", "All Time");
    }

    // Допоміжний метод для перетворення списку в рядок "Item1, Item2..."
    private String formatList(List<String> list) {
        if (list == null || list.isEmpty()) return "All";
        return android.text.TextUtils.join(", ", list);
    }

    // Оновлений setLabel
    private void setLabel(TextView tv, String text, String emptyPlaceholder) {
        tv.setText(text);
        if (text.equals(emptyPlaceholder)) {
            tv.setTextColor(Color.parseColor("#44FFFFFF")); // Тьмяний
        } else {
            tv.setTextColor(Color.parseColor("#58A870")); // Зелений акцент
        }
    }

    public void setSelectedYear(int year) { this.selectedYear = year; }

    private void setLabel(TextView tv, String text) {
        tv.setText(text);
        tv.setTextColor(text.equals("All") || text.equals("All Time") ? Color.parseColor("#44FFFFFF") : Color.parseColor("#58A870"));
    }

    @Override
    public int getTheme() { return R.style.BottomSheetDialogTheme; }
}