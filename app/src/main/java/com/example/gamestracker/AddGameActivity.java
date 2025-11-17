package com.example.gamestracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class AddGameActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    EditText editTextName, editTextDescription;
    Spinner spinnerCategory;
    RatingBar ratingBar;
    ImageView imageViewGame;
    Button buttonSelectImage, buttonAddGame;

    GameDatabaseHelper dbHelper;

    // FILE PATH (замість byte[])
    private String selectedImagePath = null;
    private Bitmap selectedImageBitmap = null;

    int editingGameId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_game);

        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ratingBar = findViewById(R.id.ratingBar);
        imageViewGame = findViewById(R.id.imageViewGame);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonAddGame = findViewById(R.id.buttonAddGame);

        dbHelper = new GameDatabaseHelper(this);


            //createTestGames();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"planned", "playing", "completed"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ratingBar.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        buttonSelectImage.setOnClickListener(v -> showImageSourceOptions());

        editingGameId = getIntent().getIntExtra("gameId", -1);
        if (editingGameId != -1) loadGameForEditing(editingGameId);

        buttonAddGame.setOnClickListener(v -> saveGame());
    }


    // ;--- GENERATE 1000 TEST GAMES ---
//    private void createTestGames() {
//        for (int i = 901; i <= 1000; i++) {
//            // Створюємо маленький Bitmap 100x100 px з різним кольором
//            Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
//            int color = 0xFF000000 | (int)(Math.random() * 0xFFFFFF);
//            bitmap.eraseColor(color);
//
//            // Зберігаємо Bitmap у внутрішнє сховище
//            String imagePath = saveBitmapToInternalStorage(bitmap);
//
//            // Створюємо тестову гру
//            Game game = new Game(
//                    -1,
//                    "Test Game " + i,
//                    "playing",
//                    "Description " + i,
//                    null, // рейтинг
//                    imagePath // шлях до картинки
//            );
//
//            // Додаємо у базу
//            dbHelper.addGame(game);
//        }
//
//        Toast.makeText(this, "1000 test games added", Toast.LENGTH_SHORT).show();
//    }


    private void showImageSourceOptions() {
        String[] options = {"Select from phone", "Search in internet"};
        new AlertDialog.Builder(this)
                .setTitle("Image source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) selectImageFromPhone();
                    else searchImageFromInternet();
                }).show();
    }

    private void selectImageFromPhone() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void searchImageFromInternet() {
        EditText input = new EditText(this);
        input.setHint("Enter game name");
        new AlertDialog.Builder(this)
                .setTitle("Search image")
                .setView(input)
                .setPositiveButton("Search", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) fetchImagesFromRAWG(query);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchImagesFromRAWG(String query) {
        RAWGImageSearch search = new RAWGImageSearch(this);
        search.searchImages(query, new RAWGImageSearch.OnImagesLoadedListener() {
            @Override
            public void onImagesLoaded(List<Bitmap> images) {
                if (images.isEmpty()) {
                    Toast.makeText(AddGameActivity.this, "No images found", Toast.LENGTH_SHORT).show();
                    return;
                }
                showImagesSelectionDialog(images);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AddGameActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagesSelectionDialog(List<Bitmap> images) {
        GridView gridView = new GridView(this);
        gridView.setNumColumns(2);

        ImageAdapter adapter = new ImageAdapter(this, images);
        gridView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose image")
                .setView(gridView)
                .setNegativeButton("Cancel", null)
                .create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Bitmap chosenBitmap = images.get(position);

            selectedImagePath = saveBitmapToInternalStorage(chosenBitmap);

            imageViewGame.setImageBitmap(chosenBitmap);

            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                selectedImagePath = saveBitmapToInternalStorage(bitmap);

                imageViewGame.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- SAVE BITMAP TO FILE ---
    private String saveBitmapToInternalStorage(Bitmap bitmap) {
        try {
            String fileName = "img_" + System.currentTimeMillis() + ".png";
            File file = new File(getFilesDir(), fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadGameForEditing(int gameId) {
        Cursor cursor = dbHelper.getGameById(gameId);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            Float rating = cursor.isNull(cursor.getColumnIndexOrThrow("rating")) ? null :
                    cursor.getFloat(cursor.getColumnIndexOrThrow("rating"));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));

            editTextName.setText(name);
            editTextDescription.setText(description);
            spinnerCategory.setSelection(getCategoryIndex(category));

            if (rating != null) {
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(rating);
            }

            if (imagePath != null && !imagePath.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    selectedImageBitmap = bitmap;
                    imageViewGame.setImageBitmap(bitmap);
                } else {
                    imageViewGame.setImageResource(R.drawable.placeholder);
                }
            } else {
                imageViewGame.setImageResource(R.drawable.placeholder);
            }

            cursor.close();
        }
    }

    private int getCategoryIndex(String cat) {
        switch (cat) {
            case "planned": return 0;
            case "playing": return 1;
            case "completed": return 2;
            default: return 0;
        }
    }

    private void saveGame() {
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String category = (String) spinnerCategory.getSelectedItem();
        Float rating = category.equals("completed") ? ratingBar.getRating() : null;

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter game name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingGameId == -1 && dbHelper.isGameExists(name)) {
            Toast.makeText(this, "Game already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Зберегти зображення, якщо обране
        if (selectedImageBitmap != null) {
            selectedImagePath = saveBitmapToInternalStorage(selectedImageBitmap);
        }

        Game game = new Game(
                editingGameId,
                name,
                category,
                description,
                rating,
                selectedImagePath
        );

        boolean success;

        if (editingGameId == -1) {
            success = dbHelper.addGame(game) != -1;
        } else {
            success = dbHelper.updateGame(game);
        }

        if (success) {
            Toast.makeText(this, editingGameId == -1 ? "Game added" : "Game updated", Toast.LENGTH_SHORT).show();
            finish(); // або clearFields() якщо хочеш залишитись на екрані
        } else {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }
}
