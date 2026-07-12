package com.your_game_library;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onApplyFilters(String sortColumn, boolean isAscending, boolean reset);
        void onOpenGenreDialog(FilterBottomSheet sheet);
        void onOpenTagDialog(FilterBottomSheet sheet);
        void onOpenPlatformDialog(FilterBottomSheet sheet);
        void onOpenLanguageDialog(FilterBottomSheet sheet);
    }

    private FilterListener listener;
    private String currentSortColumn;
    private boolean isAscending;
    private List<String> selGenres, selTags, selPlatforms, selLangs;
    private TextView tvGenres, tvTags, tvPlatforms, tvLangs;

    public FilterBottomSheet(String currentSort, boolean isAscending,
                             List<String> genres, List<String> tags,
                             List<String> platforms, List<String> languages,
                             FilterListener listener) {
        this.currentSortColumn = currentSort;
        this.isAscending = isAscending;
        this.selGenres = genres;
        this.selTags = tags;
        this.selPlatforms = platforms;
        this.selLangs = languages;
        this.listener = listener;
    }
    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            com.google.android.material.bottomsheet.BottomSheetBehavior behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_filter_sheet, container, false);

        ChipGroup cgSort = v.findViewById(R.id.cgSortColumn);
        tvGenres = v.findViewById(R.id.tvSelectedGenres);
        tvTags = v.findViewById(R.id.tvSelectedTags);
        tvPlatforms = v.findViewById(R.id.tvSelectedPlatforms);
        tvLangs = v.findViewById(R.id.tvSelectedLanguages);

        updateSummaryLabels();
        setupSortLogic(cgSort);

        // Кнопки фільтрів
        v.findViewById(R.id.btnFilterGenres).setOnClickListener(view -> listener.onOpenGenreDialog(this));
        v.findViewById(R.id.btnFilterTags).setOnClickListener(view -> listener.onOpenTagDialog(this));
        v.findViewById(R.id.btnFilterPlatforms).setOnClickListener(view -> listener.onOpenPlatformDialog(this));
        v.findViewById(R.id.btnFilterLanguages).setOnClickListener(view -> listener.onOpenLanguageDialog(this));

        v.findViewById(R.id.btnResetFilters).setOnClickListener(view -> {
            listener.onApplyFilters("id", false, true);
            dismiss();
        });

        v.findViewById(R.id.btnApplyFilters).setOnClickListener(view -> {
            listener.onApplyFilters(currentSortColumn, isAscending, false);
            dismiss();
        });

        return v;
    }

    private void setupSortLogic(ChipGroup group) {
        int count = group.getChildCount();

        for (int i = 0; i < count; i++) {
            Chip chip = (Chip) group.getChildAt(i);
            String chipSortValue = getSortValueFromId(chip.getId());

            // 1. ПОЧАТКОВИЙ СТАН ПРИ ВІДКРИТТІ
            if (chipSortValue.equals(currentSortColumn)) {
                chip.setChecked(true);
                chip.setChipIconVisible(true);
                chip.setChipIconResource(isAscending ?
                        R.drawable.ic_arrow :
                        R.drawable.ic_arrow_down);
            } else {
                chip.setChecked(false);
                chip.setChipIconVisible(false);
            }

            // 2. ОБРОБКА НАТИСКАННЯ
            chip.setOnClickListener(v -> {
                if (chipSortValue.equals(currentSortColumn)) {
                    isAscending = !isAscending;
                    chip.setChipIconResource(isAscending ?
                            R.drawable.ic_arrow :
                            R.drawable.ic_arrow_down);
                } else {
                    for (int j = 0; j < group.getChildCount(); j++) {
                        Chip c = (Chip) group.getChildAt(j);
                        c.setChipIconVisible(false);
                    }

                    currentSortColumn = chipSortValue;
                    isAscending = false;

                    chip.setChipIconVisible(true);
                    chip.setChipIconResource(R.drawable.ic_arrow_down);
                    chip.setChecked(true);
                }
            });
        }
    }

    // --- ПРАВИЛЬНИЙ І ПОВНИЙ МАПІНГ УСІХ ЧІПІВ СОРТУВАННЯ ---
    private String getSortValueFromId(int id) {
        if (id == R.id.chipName) return "name";
        if (id == R.id.chipYear) return "released";
        if (id == R.id.chipRating) return "rating";

        // Додаємо наші нові чіпи для нативного сортування:
        if (id == R.id.chipPriority) return "priority";
        if (id == R.id.chipDateAdded) return "date_added";
        if (id == R.id.chipDateStarted) return "date_started";
        if (id == R.id.chipTimeSpent) return "time_spent";
        if (id == R.id.chipDateFinished) return "date_completed"; // Має збігатися з XML id
        if (id == R.id.chipPlaythroughs) return "playthroughs";

        return "id";
    }

    public void updateSummaryLabels() {
        setLabel(tvGenres, selGenres);
        setLabel(tvTags, selTags);
        setLabel(tvPlatforms, selPlatforms);
        setLabel(tvLangs, selLangs);
    }

    private void setLabel(TextView tv, List<String> list) {
        if (list == null || list.isEmpty()) {
            tv.setText("All");
            tv.setTextColor(Color.parseColor("#44FFFFFF"));
        } else {
            tv.setText(TextUtils.join(", ", list));
            tv.setTextColor(Color.parseColor("#58A870"));
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}