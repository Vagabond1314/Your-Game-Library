package com.your_game_library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GameSelectorAdapter extends RecyclerView.Adapter<GameSelectorAdapter.VH> {
    private List<CollectionDetailsActivity.SelectableGame> fullList; // Оригінальний список
    private List<CollectionDetailsActivity.SelectableGame> filteredList; // Список для відображення (пошук)

    public GameSelectorAdapter(List<CollectionDetailsActivity.SelectableGame> list) {
        this.fullList = list;
        this.filteredList = new ArrayList<>(list);
    }

    public void filter(String query) {
        filteredList.clear();
        for (CollectionDetailsActivity.SelectableGame item : fullList) {
            if (item.game.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_game_selector, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        CollectionDetailsActivity.SelectableGame item = filteredList.get(p);
        h.name.setText(item.game.getName());
        String path = item.game.getImagePath();
        if (path != null) {
            // Логіка гібридного завантаження (URL або Файл)
            Object source = path.startsWith("http") ? path : new java.io.File(path);
            com.bumptech.glide.Glide.with(h.itemView.getContext())
                    .load(source)
                    .placeholder(R.drawable.placeholder)
                    .into(h.cover);
        }
        h.cb.setOnCheckedChangeListener(null); // Важливо скинути перед встановленням
        h.cb.setChecked(item.isSelected);
        h.cb.setOnCheckedChangeListener((b, checked) -> item.isSelected = checked);
        h.itemView.setOnClickListener(v -> h.cb.setChecked(!h.cb.isChecked()));
    }

    @Override public int getItemCount() { return filteredList.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb; TextView name;
        ImageView cover;
        VH(View v) { super(v); cb = v.findViewById(R.id.cbSelectGame); name = v.findViewById(R.id.tvSelectGameName);cover = v.findViewById(R.id.ivSelectGameCover); }
    }
}
