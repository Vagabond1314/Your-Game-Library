package com.example.gamestracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class RawgAdapter extends RecyclerView.Adapter<RawgAdapter.VH> {

    public interface OnAddClick {
        void onAdd(RawgGame rawgGame);
    }

    private Context ctx;
    private List<RawgGame> list;
    private OnAddClick listener;

    public RawgAdapter(Context ctx, List<RawgGame> list, OnAddClick listener) {
        this.ctx = ctx;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_rawg_game, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RawgGame g = list.get(position);
        holder.title.setText(g.getName());
        holder.sub.setText("Rating: " + g.getRating());

        if (g.getBackgroundImage() != null) {
            Glide.with(ctx)
                    .load(g.getBackgroundImage())
                    .placeholder(R.drawable.placeholder)
                    .into(holder.cover);
        } else {
            holder.cover.setImageResource(R.drawable.placeholder);
        }

        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) listener.onAdd(g);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, sub;
        Button btnAdd;
        VH(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.rawgCover);
            title = itemView.findViewById(R.id.rawgTitle);
            sub = itemView.findViewById(R.id.rawgSub);
            btnAdd = itemView.findViewById(R.id.btnAddToMy);
        }
    }
}
