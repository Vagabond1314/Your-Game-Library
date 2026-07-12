package com.your_game_library;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CollectionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CollectionsAdapter adapter;
    private List<GameCollection> fullList = new ArrayList<>(); // Оригінальний список
    private GameDatabaseHelper dbHelper;

    // Тимчасові змінні для нового діалогу
    private String selectedImagePath = "";
    private int selectedColor = Color.parseColor("#1E1E1E");
    private ImageView ivPreview; // Для предперегляду в діалозі
    private List<Integer> quickSelectedIds = new ArrayList<>();

    private static final int PICK_COLLECTION_IMAGE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

        dbHelper = GameDatabaseHelper.getInstance(this);
        recyclerView = findViewById(R.id.recyclerCollections);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddCollection);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        loadCollections();

        fabAdd.setOnClickListener(v -> showAddCollectionDialog());

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Collections");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        setupDragAndDrop();

        adapter.setOnCollectionActionListener(new CollectionsAdapter.OnCollectionActionListener() {
            @Override
            public void onEdit(GameCollection collection) {
                showEditCollectionDialog(collection);
            }

            @Override
            public void onDelete(GameCollection collection) {
                confirmDelete(collection);
            }
        });
    }
    private void confirmDelete(GameCollection col) {
        new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Delete Collection")
                .setMessage("Are you sure you want to delete '" + col.getName() + "'?")
                .setPositiveButton("Delete", (d, w) -> {
                    dbHelper.deleteCollection(col.getId());
                    loadCollections();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showEditCollectionDialog(GameCollection col) {
        // 1. Початкові дані
        selectedImagePath = col.getImagePath();
        selectedColor = col.getColor();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_collection, null);

        EditText etName = view.findViewById(R.id.etCollectionName);
        View btnColor = view.findViewById(R.id.btnPickColor);
        View btnImage = view.findViewById(R.id.btnPickImage);
        ivPreview = view.findViewById(R.id.ivCollectionPreview);
        RadioGroup rgVisibility = view.findViewById(R.id.rgNameVisibility);
        if (col.getShowName() == 1) {
            rgVisibility.check(R.id.rbShowName);
        } else {
            rgVisibility.check(R.id.rbHideName);
        }
        // Елементи для вибору фото з ігор
        TextView tvLabel = view.findViewById(R.id.tvQuickAddLabel);
        RecyclerView rvQuickAdd = view.findViewById(R.id.rvQuickAddGames);

        etName.setText(col.getName());
        btnColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selectedColor));

        // Показ поточної обкладинки
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            ivPreview.setVisibility(View.VISIBLE);
            Object source = selectedImagePath.startsWith("http") ? selectedImagePath : new File(selectedImagePath);
            Glide.with(this).load(source).into(ivPreview);
        }

        // 2. Логіка вибору фото з ігор КОЛЕКЦІЇ
        List<Game> gamesInCollection = dbHelper.getGamesInCollection(col.getId());

        if (gamesInCollection.isEmpty()) {
            tvLabel.setVisibility(View.GONE);
            rvQuickAdd.setVisibility(View.GONE);
        } else {
            tvLabel.setVisibility(View.VISIBLE);
            tvLabel.setText("Choose cover from games in collection"); // Міняємо напис
            rvQuickAdd.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            // Використовуємо наш QuickAddAdapter для вибору фото
            QuickAddAdapter pickImgAdapter = new QuickAddAdapter(gamesInCollection, (game, isSelected) -> {
                // При кліку на гру просто беремо її фото як обкладинку колекції
                selectedImagePath = game.getImagePath();

                // Оновлюємо прев'ю
                if (selectedImagePath != null) {
                    ivPreview.setVisibility(View.VISIBLE);
                    Object source = selectedImagePath.startsWith("http") ? selectedImagePath : new File(selectedImagePath);
                    Glide.with(this).load(source).into(ivPreview);
                }
            });
            pickImgAdapter.setSingleChoice(true);
            rvQuickAdd.setAdapter(pickImgAdapter);
        }

        // 3. Інші обробники
        btnColor.setOnClickListener(v -> showFullColorPicker(btnColor));
        btnImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_COLLECTION_IMAGE);
        });

        builder.setView(view);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                boolean showName = rgVisibility.getCheckedRadioButtonId() == R.id.rbShowName;

                // Оновлюємо в БД з новим параметром
                dbHelper.updateCollection(col.getId(), name, selectedImagePath, selectedColor, showName ? 1 : 0);

                loadCollections();
                dialog.dismiss();
            }
        });
    }
    private void showFullColorPicker(View colorPreviewInDialog) {
        // Створюємо контейнер для діалогу
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // Додаємо наш Custom View
        ColorPickerView colorPicker = new ColorPickerView(this, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 600); // Висота палітри 600px
        colorPicker.setLayoutParams(params);
        colorPicker.setColor(selectedColor); // Встановлюємо поточний колір

        layout.addView(colorPicker);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Pick Collection Color")
                .setView(layout)
                .setPositiveButton("Select", (d, which) -> {
                    colorPreviewInDialog.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(selectedColor));
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Слухаємо зміни кольору в реальному часі
        colorPicker.setOnColorChangedListener(color -> {
            selectedColor = color;
            // Можна відразу міняти колір заголовка діалогу для ефекту
        });

        dialog.show();
    }
    private void setupSearch(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCollections(newText);
                return true;
            }
        });
    }

    private void filterCollections(String query) {
        List<GameCollection> filteredList = new ArrayList<>();
        for (GameCollection col : fullList) {
            if (col.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(col);
            }
        }
        adapter = new CollectionsAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);
    }

    private void loadCollections() {
        // Отримуємо свіжий список з бази
        fullList = dbHelper.getAllCollections();

        // Створюємо новий адаптер або оновлюємо існуючий
        adapter = new CollectionsAdapter(this, fullList);

        // Перепідключаємо слухача (якщо ти створюєш новий адаптер)
        adapter.setOnCollectionActionListener(new CollectionsAdapter.OnCollectionActionListener() {
            @Override
            public void onEdit(GameCollection collection) {
                showEditCollectionDialog(collection);
            }

            @Override
            public void onDelete(GameCollection collection) {
                confirmDelete(collection);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // Переміщуємо в локальному списку
                java.util.Collections.swap(fullList, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Не використовуємо swipe
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Коли користувач відпустив плитку — зберігаємо новий порядок у БД
                saveNewOrder();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void saveNewOrder() {
        for (int i = 0; i < fullList.size(); i++) {
            dbHelper.updateCollectionOrder(fullList.get(i).getId(), i);
        }
    }
    private List<Game> selectedGamesForNewCol = new ArrayList<>();

    private void showAddCollectionDialog() {
        quickSelectedIds.clear();
        selectedImagePath = "";
        selectedColor = Color.parseColor("#1E1E1E");

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_collection, null);

        EditText etName = view.findViewById(R.id.etCollectionName);
        ImageView ivPreview = view.findViewById(R.id.ivCollectionPreview);
        RecyclerView rvQuickAdd = view.findViewById(R.id.rvQuickAddGames);
        TextView tvQuickAddLabel = view.findViewById(R.id.tvQuickAddLabel);
        tvQuickAddLabel.setVisibility(View.VISIBLE);
        RadioGroup rgVisibility = view.findViewById(R.id.rgNameVisibility);

        List<Game> allGames = dbHelper.getAllGames();
        rvQuickAdd.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        QuickAddAdapter quickAdapter = new QuickAddAdapter(allGames, (game, isSelected) -> {
            if (isSelected) {
                quickSelectedIds.add(game.getId());
                selectedImagePath = game.getImagePath();
                Object source = selectedImagePath.startsWith("http") ? selectedImagePath : new File(selectedImagePath);
                Glide.with(this).load(source).into(ivPreview);
                ivPreview.setVisibility(View.VISIBLE);
            } else {
                quickSelectedIds.remove(Integer.valueOf(game.getId()));
                if (quickSelectedIds.isEmpty()) {
                    ivPreview.setImageResource(R.drawable.placeholder);
                    selectedImagePath = "";
                }
            }
        });
        rvQuickAdd.setAdapter(quickAdapter);

        view.findViewById(R.id.btnPickColor).setOnClickListener(v -> showFullColorPicker(view.findViewById(R.id.btnPickColor)));
        view.findViewById(R.id.btnPickImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_COLLECTION_IMAGE);
        });

        builder.setView(view);
        builder.setPositiveButton("Create", null); // Ставимо null, логіку пропишемо нижче
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Перевизначаємо логіку кнопки після показу діалогу
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                // Показуємо попередження прямо в полі вводу
                etName.setError("Collection name is required!");
                Toast.makeText(this, "Please enter a name for your collection", Toast.LENGTH_SHORT).show();
            } else {
                // Перевіряємо, чи вибрано "Show Name"
                boolean showName = rgVisibility.getCheckedRadioButtonId() == R.id.rbShowName;

                // Передаємо параметр showName (1 або 0) в БД
                long colId = dbHelper.addCollection(name, selectedImagePath, selectedColor, showName ? 1 : 0);
                for (Integer gameId : quickSelectedIds) {
                    dbHelper.addGameToCollection((int)colId, gameId);
                }
                loadCollections();
                dialog.dismiss(); // Тепер закриваємо вручну
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_COLLECTION_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                selectedImagePath = saveCollectionImage(bitmap);

                // Оновлюємо прев'ю в поточному відкритому діалозі
                if (ivPreview != null) {
                    ivPreview.setImageBitmap(bitmap);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String saveCollectionImage(Bitmap bitmap) {
        try {
            File dir = new File(getFilesDir(), "collection_images");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "col_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }
}