package com.your_game_library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class SteamPreviewAdapter extends RecyclerView.Adapter<SteamPreviewAdapter.ViewHolder> {
    private List<SteamResponse.SteamGame> games;
    private List<SteamResponse.SteamGame> selectedGames;

    public SteamPreviewAdapter(List<SteamResponse.SteamGame> games, List<SteamResponse.SteamGame> selectedGames) {
        this.games = games;
        this.selectedGames = selectedGames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_steam_preview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SteamResponse.SteamGame game = games.get(position);

        holder.tvName.setText(game.name);

        float hours = game.playtime_forever / 60.0f;
        if (hours == 0) {
            holder.tvTime.setText("0h (Will go to Planned)");
            holder.tvTime.setTextColor(android.graphics.Color.parseColor("#2D5E85")); // Синій
        } else {
            holder.tvTime.setText(String.format(java.util.Locale.getDefault(), "%.1fh", hours));
            holder.tvTime.setTextColor(android.graphics.Color.parseColor("#58A870")); // Зелений
        }

        // Завантаження іконки Steam
        if (game.img_icon_url != null && !game.img_icon_url.isEmpty()) {
            String iconUrl = "http://media.steampowered.com/steamcommunity/public/images/apps/" + game.appid + "/" + game.img_icon_url + ".jpg";
            Glide.with(holder.itemView.getContext()).load(iconUrl).centerCrop().into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.placeholder);
        }

        // Логіка чекбоксу
        holder.cbSelect.setOnCheckedChangeListener(null); // Скидаємо слухач, щоб уникнути багів при скролі
        holder.cbSelect.setChecked(selectedGames.contains(game));

        holder.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !selectedGames.contains(game)) {
                selectedGames.add(game);
            } else if (!isChecked) {
                selectedGames.remove(game);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelect.setChecked(!holder.cbSelect.isChecked()));
    }

    @Override
    public int getItemCount() { return games.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivCover;
        TextView tvName, tvTime;
        ViewHolder(View v) {
            super(v);
            cbSelect = v.findViewById(R.id.cbSelectGame);
            ivCover = v.findViewById(R.id.ivCover);
            tvName = v.findViewById(R.id.tvName);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}