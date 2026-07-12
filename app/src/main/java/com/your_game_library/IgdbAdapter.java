package com.your_game_library;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class IgdbAdapter extends RecyclerView.Adapter<IgdbAdapter.VH> {

    // Створюємо константи для типів макетів
    public static final int TYPE_LIST = 0;
    public static final int TYPE_GRID = 1;

    private Context ctx;
    private List<IgdbGame> list;
    private static int layoutType; // Змінна, яка зберігає поточний тип

    // Оновлений конструктор
    public IgdbAdapter(Context ctx, List<IgdbGame> list, int layoutType) {
        this.ctx = ctx;
        this.list = list;
        this.layoutType = layoutType;
    }

    @Override
    public int getItemViewType(int position) {
        // Повертаємо тип, який передали в конструктор
        return layoutType;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;

        // Вибираємо макет через switch
        switch (viewType) {
            case TYPE_GRID:
                layoutRes = R.layout.item_grid_game;
                break;
            case TYPE_LIST:
            default:
                layoutRes = R.layout.item_rawg_game;
                break;
        }

        View v = LayoutInflater.from(ctx).inflate(layoutRes, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        IgdbGame g = list.get(position);

        if(layoutType == 0)
            holder.title.setText(g.name);

        // --- ЛОГІКА ОЦІНКИ (Рейтингу) ---
        if (holder.sub != null) {
            // Перевіряємо, чи є рейтинг з IGDB
            if (g.total_rating > 0) { // Або game.getRating(), залежно від твоєї моделі в IgdbAdapter
                float ratingValue = (float) (g.total_rating / 10.0f);
                holder.sub.setText("★ " + String.format(java.util.Locale.ROOT, "%.1f", ratingValue));
                holder.sub.setVisibility(View.VISIBLE);
            } else {
                holder.sub.setVisibility(View.GONE);
            }
        }

        String imageUrl = null;
        if (g.cover != null && g.cover.url != null) {
            // Для списку можна вантажити t_720p, для сітки t_cover_big (економія трафіку)
            String quality = (layoutType == TYPE_GRID) ? "t_cover_big" : "t_1080p";
            imageUrl = getHighResUrl(g.cover.url, quality);
        }

        Glide.with(ctx)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(holder.cover);

        // Решта логіки (кліки, анімації) залишається такою ж
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, GameDetailsActivity.class);
            intent.putExtra("game_id", g.id);
            intent.putExtra("game_name", g.name);
            ctx.startActivity(intent);
        });
}

    // Додайте цей метод у клас IgdbAdapter
    private String getHighResUrl(String url, String sizeTag) {
        if (url == null || url.isEmpty()) return null;

        // Додаємо протокол https:
        if (url.startsWith("//")) {
            url = "https:" + url;
        }

        // Замінюємо будь-який розмір (наприклад, t_thumb) на обраний (t_cover_big)
        // Використовуємо регулярний вираз для пошуку будь-якого тегу, що починається на t_
        return url.replaceAll("t_\\w+", sizeTag);
    }
    // Метод для оновлення списку (наприклад, при пошуку)
    public void updateList(List<com.your_game_library.IgdbGame> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, sub;

        VH(@NonNull View itemView) {
            super(itemView);
            // Використовуємо ваші існуючі ID з layout
            cover = itemView.findViewById(R.id.gameImage);
            title = itemView.findViewById(R.id.gameName);
            sub = itemView.findViewById(R.id.gameRating);
        }
    }
}