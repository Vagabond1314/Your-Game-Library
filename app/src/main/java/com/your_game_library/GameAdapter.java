package com.your_game_library;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private final Context context;
    private final GameDatabaseHelper dbHelper;
    private boolean isGrid;

    // Track images currently being downloaded to prevent duplicate threads
    private final Set<Integer> downloadingGameIds = new HashSet<>();

    public enum AdapterMode {
        MAIN_LIST,
        COLLECTION_LIST
    }
    private final AdapterMode mode;
    private int collectionId = -1;

    private final DiffUtil.ItemCallback<Game> diffCallback = new DiffUtil.ItemCallback<Game>() {
        @Override
        public boolean areItemsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
            return oldItem.equals(newItem);
        }
    };
    private final AsyncListDiffer<Game> differ = new AsyncListDiffer<>(this, diffCallback);

    public GameAdapter(Context context, List<Game> initialList, GameDatabaseHelper dbHelper, boolean isGrid) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.isGrid = isGrid;
        this.mode = AdapterMode.MAIN_LIST;
        setHasStableIds(true);
        // KEEP SCROLL POSITION AUTOMATICALLY:
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        submitList(initialList, null);
    }

    public GameAdapter(Context context, List<Game> initialList, GameDatabaseHelper dbHelper, boolean isGrid, int collectionId) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.isGrid = isGrid;
        this.mode = AdapterMode.COLLECTION_LIST;
        this.collectionId = collectionId;
        setHasStableIds(true);
        // KEEP SCROLL POSITION AUTOMATICALLY:
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        submitList(initialList, null);
    }

    public void setGrid(boolean grid) {
        if (this.isGrid != grid) {
            this.isGrid = grid;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        // FIX #1: Return distinct view types for Grid (1) vs List (0)
        return isGrid ? 1 : 0;
    }

    @Override
    public long getItemId(int position) {
        return differ.getCurrentList().get(position).getId();
    }

    public void submitList(List<Game> list, Runnable commitCallback) {
        differ.submitList(list != null ? new ArrayList<>(list) : null, commitCallback);
    }

    public List<Game> getCurrentList() {
        return differ.getCurrentList();
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate correct XML layout based on viewType
        int layoutId = (viewType == 1) ? R.layout.item_grid_game : R.layout.item_game;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = differ.getCurrentList().get(position);

        View llPriority = holder.itemView.findViewById(R.id.llPriority);
        TextView tvGamePriority = holder.itemView.findViewById(R.id.tvGamePriority);
        View llTimeSpent = holder.itemView.findViewById(R.id.llTimeSpent);
        TextView tvGameTimeSpent = holder.itemView.findViewById(R.id.tvGameTimeSpent);

        // Reset visibility states
        if (llPriority != null) llPriority.setVisibility(View.GONE);
        if (llTimeSpent != null) llTimeSpent.setVisibility(View.GONE);
        if (holder.gameRating != null) holder.gameRating.setVisibility(View.GONE);

        // --- BADGES LOGIC (Priority, Rating, Time Spent) ---
        if ("planned".equalsIgnoreCase(game.getCategory())) {
            if (game.getRating() != null && game.getRating() > 0) {
                if (holder.gameRating != null) {
                    holder.gameRating.setText("★ " + String.format(java.util.Locale.ROOT, "%.1f", game.getRating()));
                    holder.gameRating.setVisibility(View.VISIBLE);
                }
            }
            if (game.getPriority() != null && game.getPriority() > 0) {
                if (llPriority != null) llPriority.setVisibility(View.VISIBLE);
                if (tvGamePriority != null) tvGamePriority.setText(String.valueOf(game.getPriority()));
            }
        } else if ("completed".equalsIgnoreCase(game.getCategory())) {
            if (game.getRating() != null && game.getRating() > 0) {
                if (holder.gameRating != null) {
                    holder.gameRating.setText("★ " + String.format(java.util.Locale.ROOT, "%.1f", game.getRating()));
                    holder.gameRating.setVisibility(View.VISIBLE);
                }
            }
            if (game.getTime() != null && game.getTime() > 0) {
                if (llTimeSpent != null) {
                    llTimeSpent.setVisibility(View.VISIBLE);
                    float time = (float) Math.round(game.getTime() * 10) / 10;
                    if (tvGameTimeSpent != null) tvGameTimeSpent.setText(time + "h");
                }
            }
        } else if ("playing".equalsIgnoreCase(game.getCategory())) {
            if (game.getRating() != null && game.getRating() > 0) {
                if (holder.gameRating != null) {
                    holder.gameRating.setText("★ " + String.format(java.util.Locale.ROOT, "%.1f", game.getRating()));
                    holder.gameRating.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // FIX #3: Null check added to prevent NullPointerException
            if (game.getRating() != null && game.getRating() > 0) {
                if (holder.gameRating != null) {
                    holder.gameRating.setText("★ " + String.format(java.util.Locale.ROOT, "%.1f", game.getRating()));
                    holder.gameRating.setVisibility(View.VISIBLE);
                }
            }
        }

        // --- GAME NAME DISPLAY ---
        if (holder.gameName != null && !isGrid) {
            holder.gameName.setVisibility(View.VISIBLE);
            holder.gameName.setText(game.getName());
        }

        // --- IMAGE LOADING LOGIC ---
        String path = game.getImagePath();
        String remoteUrl = game.getImageUrl();

        if (path != null && !path.isEmpty()) {
            if (path.startsWith("http")) {
                String highResUrl = path.replaceAll("t_\\w+", "t_1080p");
                Glide.with(context).load(highResUrl).placeholder(R.drawable.placeholder).into(holder.gameImage);
                downloadImageLocally(game, highResUrl);
            } else {
                File imgFile = new File(path);
                if (imgFile.exists()) {
                    Glide.with(context).load(imgFile).placeholder(R.drawable.placeholder).into(holder.gameImage);
                } else if (remoteUrl != null && remoteUrl.startsWith("http")) {
                    String highResUrl = remoteUrl.replaceAll("t_\\w+", "t_1080p");
                    Glide.with(context).load(highResUrl).placeholder(R.drawable.placeholder).into(holder.gameImage);
                    downloadImageLocally(game, highResUrl);
                } else {
                    holder.gameImage.setImageResource(R.drawable.placeholder);
                }
            }
        } else {
            holder.gameImage.setImageResource(R.drawable.placeholder);
        }

        // --- CLICK LISTENERS ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MyGameDetailsActivity.class);
            intent.putExtra("gameId", game.getId());
            context.startActivity(intent);
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    holder.gameImage.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holder.gameImage.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    break;
            }
            return false;
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mode == AdapterMode.MAIN_LIST) {
                showFullDeleteDialog(game);
            } else {
                showRemoveFromCollectionDialog(game);
            }
            return true;
        });
    }

    private void showFullDeleteDialog(Game game) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Game")
                .setMessage("Are you sure you want to permanently delete '" + game.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteGame(game.getId());
                    List<Game> updated = new ArrayList<>(differ.getCurrentList());
                    updated.remove(game);
                    submitList(updated, null);
                    Toast.makeText(context, "Game deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveFromCollectionDialog(Game game) {
        new AlertDialog.Builder(context)
                .setTitle("Remove from Collection")
                .setMessage("Remove '" + game.getName() + "' from this collection?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    dbHelper.removeGameFromCollection(collectionId, game.getId());
                    List<Game> updated = new ArrayList<>(differ.getCurrentList());
                    updated.remove(game);
                    submitList(updated, null);
                    Toast.makeText(context, "Removed from collection", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void downloadImageLocally(Game game, String imageUrl) {
        // FIX #2: Prevent duplicate background download threads for the same game
        if (downloadingGameIds.contains(game.getId())) return;
        downloadingGameIds.add(game.getId());

        new Thread(() -> {
            try {
                File dir = new File(context.getFilesDir(), "game_images");
                if (!dir.exists()) dir.mkdirs();

                Bitmap bitmap = Glide.with(context).asBitmap().load(imageUrl).submit().get();

                if (bitmap != null) {
                    File file = new File(dir, "img_" + game.getId() + "_" + System.currentTimeMillis() + ".webp");
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 70, out);
                    out.close();

                    game.setImagePath(file.getAbsolutePath());
                    // Update database without triggering UI reload loops
                    dbHelper.updateGame(game);
                }
            } catch (Exception e) {
                Log.e("ADAPTER_SAVE", "Error saving image for " + game.getName());
            } finally {
                downloadingGameIds.remove(game.getId());
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView gameImage;
        TextView gameName, gameRating;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameImage = itemView.findViewById(R.id.gameImage);
            gameName = itemView.findViewById(R.id.gameName);
            gameRating = itemView.findViewById(R.id.gameRating);
        }
    }
}