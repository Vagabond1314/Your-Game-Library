package com.example.gamestracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private Context context;
    private List<Game> games;
    private GameDatabaseHelper dbHelper;

    public GameAdapter(Context context, List<Game> games, GameDatabaseHelper dbHelper) {
        this.context = context;
        this.games = games;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);

        holder.gameName.setText(game.getName());

        // Обрізати опис
        String description = game.getDescription();
        if (description.length() > 150) {
            description = description.substring(0, 150) + "...";
        }
        holder.gameDescription.setText(description);

        // Рейтинг
        if (game.getRating() != null) {
            holder.gameRating.setText("Rating: " + game.getRating());
            holder.gameRating.setVisibility(View.VISIBLE);
        } else {
            holder.gameRating.setVisibility(View.GONE);
        }

        // 🔥 ВІДОБРАЖЕННЯ КАРТИНКИ ЧЕРЕЗ FILE PATH
        if (game.getImagePath() != null) {
            File imgFile = new File(game.getImagePath());
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.gameImage.setImageBitmap(bitmap);
            } else {
                holder.gameImage.setImageResource(R.drawable.placeholder);
            }
        } else {
            holder.gameImage.setImageResource(R.drawable.placeholder);
        }

        // Натискання → редагування
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddGameActivity.class);
            intent.putExtra("gameId", game.getId());
            context.startActivity(intent);
        });

        // Довге натискання → видалення
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return true;

            Game gameToDelete = games.get(adapterPosition);

            if (dbHelper == null) {
                Toast.makeText(context, "Error: database not initialized", Toast.LENGTH_SHORT).show();
                return true;
            }

            new AlertDialog.Builder(context)
                    .setTitle("Delete game")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        boolean deleted = dbHelper.deleteGame(gameToDelete.getId());
                        if (deleted) {

                            // Якщо є файл картинки — видалити
                            if (gameToDelete.getImagePath() != null) {
                                File file = new File(gameToDelete.getImagePath());
                                if (file.exists()) file.delete();
                            }

                            games.remove(adapterPosition);
                            notifyItemRemoved(adapterPosition);

                            Toast.makeText(context, "Game deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView gameImage;
        TextView gameName, gameDescription, gameRating;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameImage = itemView.findViewById(R.id.gameImage);
            gameName = itemView.findViewById(R.id.gameName);
            gameDescription = itemView.findViewById(R.id.gameDescription);
            gameRating = itemView.findViewById(R.id.gameRating);
        }
    }
}
