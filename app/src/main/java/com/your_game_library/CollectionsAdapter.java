package com.your_game_library;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class CollectionsAdapter extends RecyclerView.Adapter<CollectionsAdapter.CollectionVH> {

    private final Context context;
    private final List<GameCollection> collections;

    public CollectionsAdapter(Context context, List<GameCollection> collections) {
        this.context = context;
        this.collections = collections;
    }

    @NonNull
    @Override
    public CollectionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_collection, parent, false);
        return new CollectionVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionVH holder, int position) {
        GameCollection col = collections.get(position);

        holder.name.setText(col.getName());
        if (col.getShowName() == 0) {
            holder.name.setVisibility(View.GONE);
        } else {
            holder.name.setVisibility(View.VISIBLE);
        }
        holder.count.setText(col.getGameCount() + " games");

        // 1. Встановлюємо фоновий колір контейнера
        // Якщо колір не заданий (0), ставимо стандартний темно-сірий
        int accentColor = col.getColor() != 0 ? col.getColor() : Color.parseColor("#58A870");

        // 1. Встановлюємо колір рамки (Stroke)
        holder.cardRoot.setStrokeColor(android.content.res.ColorStateList.valueOf(accentColor));

        // 2. Фарбуємо шар тонування поверх фото
        holder.colorOverlay.setBackgroundColor(accentColor);

        if (col.getImagePath() == null || col.getImagePath().isEmpty()) {
            holder.colorOverlay.setAlpha(1.0f);
        }
        // 2. Логіка завантаження зображення
        String path = col.getImagePath();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("http")) {
                // Якщо обрано обкладинку гри (URL)
                Glide.with(context)
                        .load(path)
                        .centerCrop()
                        .into(holder.image);
            } else {
                // Якщо обрано фото з галереї (локальний шлях)
                File file = new File(path);
                Glide.with(context)
                        .load(file)
                        .centerCrop()
                        .error(R.drawable.placeholder)
                        .into(holder.image);
            }
            holder.image.setVisibility(View.VISIBLE);
        } else {
            // Якщо фото немає - ховаємо ImageView, залишаючи лише колір фону
            holder.image.setVisibility(View.GONE);
        }
        holder.btnMenu.setOnClickListener(v -> {
            // Використовуємо ContextThemeWrapper для темного дизайну меню
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(context, v);
            popup.getMenuInflater().inflate(R.menu.collection_item_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (actionListener == null) return false;

                if (item.getItemId() == R.id.action_col_edit) {
                    actionListener.onEdit(col);
                    return true;
                } else if (item.getItemId() == R.id.action_col_delete) {
                    actionListener.onDelete(col);
                    return true;
                }
                return false;
            });
            popup.show();
        });
        // 3. Клік по колекції
        holder.itemView.setOnClickListener(v -> {
            // Тут ми відкриваємо нову Activity, яка покаже ігри цієї колекції
            Intent intent = new Intent(context, CollectionDetailsActivity.class);
            intent.putExtra("COLLECTION_ID", col.getId());
            intent.putExtra("COLLECTION_NAME", col.getName());
            intent.putExtra("COLLECTION_COLOR", col.getColor());
            context.startActivity(intent);
        });

        // 4. Довгий клік (для видалення або редагування)
        holder.itemView.setOnLongClickListener(v -> {
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }
    public interface OnCollectionClickListener {
        void onClick(GameCollection collection);
    }
    private OnCollectionClickListener clickListener;

    public void setOnCollectionClickListener(OnCollectionClickListener listener) {
        this.clickListener = listener;
    }
    static class CollectionVH extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardRoot; // Обов'язково MaterialCardView
        ConstraintLayout container;
        ImageView image;
        View colorOverlay; // Додано для відтінку
        TextView name, count;
        ImageButton btnMenu;
        View gradientTop;

        public CollectionVH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            container = itemView.findViewById(R.id.collectionContainer);
            image = itemView.findViewById(R.id.collectionImage);
            colorOverlay = itemView.findViewById(R.id.colorOverlay); // Знаходимо шар кольору
            name = itemView.findViewById(R.id.tvCollectionName);
            count = itemView.findViewById(R.id.tvGameCount);
            btnMenu = itemView.findViewById(R.id.btnCollectionMenu);
        }
    }

    public interface OnCollectionLongClickListener {
        void onLongClick(GameCollection collection);
    }
    public interface OnCollectionActionListener {
        void onEdit(GameCollection collection);
        void onDelete(GameCollection collection);
    }

    private OnCollectionActionListener actionListener;

    public void setOnCollectionActionListener(OnCollectionActionListener listener) {
        this.actionListener = listener;
    }
    private OnCollectionLongClickListener longClickListener;

    public void setOnCollectionLongClickListener(OnCollectionLongClickListener listener) {
        this.longClickListener = listener;
    }
}