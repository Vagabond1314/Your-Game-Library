package com.your_game_library;

import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotsAdapter extends RecyclerView.Adapter<ScreenshotsAdapter.VH> {

    private final Context context;
    private final List<String> urls;

    public ScreenshotsAdapter(Context context, List<String> urls) {
        this.context = context;
        this.urls = urls;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView img = new ImageView(context);
        img.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        // Для скріншотів краще використовувати CENTER_CROP, щоб вони заповнювали картку,
        // або FIT_CENTER, якщо хочете бачити весь скріншот з полями.
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new VH(img);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String path = urls.get(position);

        if (path == null) return;

        if (path.startsWith("http")) {
            // Завантаження з інтернету
            Glide.with(context)
                    .load(path)
                    .placeholder(R.drawable.placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Кешуємо і оригінал, і змінений розмір
                    .into(holder.imageView);
        } else {
            // Локальний шлях. Передаємо об'єкт File.
            // Навіть якщо файлу немає фізично після Restore, Glide спробує знайти його у своєму кеші за цим шляхом.
            File f = new File(path);
            Glide.with(context)
                    .load(f)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder) // Важливо: якщо файлу немає і в кеші немає - показуємо плейсхолдер
                    .into(holder.imageView);
        }

        // Відкриття на весь екран
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            intent.putStringArrayListExtra("image_list", new ArrayList<>(urls));
            intent.putExtra("start_position", position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return urls != null ? urls.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;
        VH(@NonNull ImageView itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}