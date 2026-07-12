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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    private List<NewsArticle> newsList = new ArrayList<>();
    private Context context;

    public NewsAdapter(Context context) {
        this.context = context;
    }

    public void setNews(List<NewsArticle> articles) {
        newsList.clear();
        newsList.addAll(articles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsArticle article = newsList.get(position);
        holder.title.setText(article.getTitle());
        holder.description.setText(article.getDescription());
        FavoritesManager favManager = new FavoritesManager(context);
        boolean isFav = favManager.isFavorite(article.getUrl());

        holder.buttonFavorite.setColorFilter(isFav ? Color.parseColor("#58A870") : Color.WHITE);

        holder.buttonFavorite.setOnClickListener(v -> {
            String url = article.getUrl();
            if (url == null || url.isEmpty()) {
                Toast.makeText(context, "Invalid news link", Toast.LENGTH_SHORT).show();
                return;
            }
            if (favManager.isFavorite(url)) {
                favManager.removeFavorite(url);
            } else {
                favManager.addFavorite(article);
            }
            notifyItemChanged(position);
        });

        if (article.getUrlToImage() != null) {
            Glide.with(context).load(article.getUrlToImage()).into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.placeholder); // placeholder image
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NewsDetailActivity.class);
            intent.putExtra("url", article.getUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description;
        ImageView image;
        ImageButton buttonFavorite = itemView.findViewById(R.id.buttonFavorite);
        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.newsTitle);
            description = itemView.findViewById(R.id.newsDescription);
            image = itemView.findViewById(R.id.newsImage);
        }
    }
    // У NewsAdapter.java додайте цей метод замість або разом із setNews
    public void addNews(List<NewsArticle> newArticles) {
        int startPosition = newsList.size();
        newsList.addAll(newArticles);
        notifyItemRangeInserted(startPosition, newArticles.size());
    }

    // Також додайте метод для повного очищення (коли змінюєте категорію)
    public void clearData() {
        newsList.clear();
        notifyDataSetChanged();
    }
}
