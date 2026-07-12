package com.your_game_library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QuickAddAdapter extends RecyclerView.Adapter<QuickAddAdapter.VH> {

    public interface OnGameClickListener {
        void onGameClick(Game game, boolean isSelected);
    }

    private final List<Game> allGames;
    private final List<Integer> selectedIds = new ArrayList<>();
    private final OnGameClickListener listener;
    private boolean isSingleChoice = false; // Новий прапорець

    public QuickAddAdapter(List<Game> allGames, OnGameClickListener listener) {
        this.allGames = allGames;
        this.listener = listener;
    }

    // Метод для ввімкнення режиму одного вибору
    public void setSingleChoice(boolean singleChoice) {
        this.isSingleChoice = singleChoice;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_add_game, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Game game = allGames.get(position);
        boolean isSelected = selectedIds.contains(game.getId());

        // Візуал: рамка та оверлей
        holder.cardRoot.setStrokeWidth(isSelected ? 6 : 0); // Рамка 6px якщо вибрано
        holder.overlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        String path = game.getImagePath();
        if (path != null) {
            Object source = path.startsWith("http") ? path : new File(path);
            Glide.with(holder.itemView.getContext()).load(source).into(holder.cover);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSingleChoice) {
                // Логіка одиночного вибору
                if (!isSelected) {
                    selectedIds.clear(); // Видаляємо попередній вибір
                    selectedIds.add(game.getId());
                    notifyDataSetChanged(); // Оновлюємо весь список для перемалювання рамок
                    listener.onGameClick(game, true);
                }
            } else {
                // Логіка мульти-вибору (для створення нової колекції)
                if (isSelected) {
                    selectedIds.remove(Integer.valueOf(game.getId()));
                } else {
                    selectedIds.add(game.getId());
                }
                notifyItemChanged(position);
                listener.onGameClick(game, !isSelected);
            }
        });
    }

    @Override
    public int getItemCount() { return allGames.size(); }

    static class VH extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardRoot;
        ImageView cover;
        View overlay;
        VH(View v) {
            super(v);
            cardRoot = (com.google.android.material.card.MaterialCardView) v.findViewById(R.id.cvQuickGameRoot);
            cover = v.findViewById(R.id.ivQuickGameCover);
            overlay = v.findViewById(R.id.vSelectionOverlay);
        }
    }
}