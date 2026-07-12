package com.your_game_library;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import java.io.File;
import java.util.List;

public class FullScreenPagerAdapter extends RecyclerView.Adapter<FullScreenPagerAdapter.ViewHolder> {

    private final Context context;
    private final List<String> images;

    public FullScreenPagerAdapter(Context context, List<String> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PhotoView photoView = new PhotoView(context);
        photoView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new ViewHolder(photoView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String path = images.get(position);
        if (path.startsWith("http")) {
            Glide.with(context).load(path).into(holder.photoView);
        } else {
            Glide.with(context).load(new File(path)).into(holder.photoView);
        }
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        ViewHolder(@NonNull PhotoView itemView) {
            super(itemView);
            photoView = itemView;
        }
    }
}