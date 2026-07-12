package com.your_game_library;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ViewPager2 viewPager = findViewById(R.id.fullScreenPager);
        ImageButton btnClose = findViewById(R.id.btnClose);

        // 1. Спробуємо отримати список картинок (для ігор)
        List<String> imageList = getIntent().getStringArrayListExtra("image_list");
        int startPosition = getIntent().getIntExtra("start_position", 0);

        // 2. Спробуємо отримати одну картинку (для статей)
        String singleImageUrl = getIntent().getStringExtra("image_url");

        // 3. Логіка універсальності:
        // Якщо списку немає, але є одна посилка — створюємо список з одним елементом
        if (imageList == null && singleImageUrl != null) {
            imageList = new ArrayList<>();
            imageList.add(singleImageUrl);
            startPosition = 0;
        }

        // 4. Відображення
        if (imageList != null && !imageList.isEmpty()) {
            FullScreenPagerAdapter adapter = new FullScreenPagerAdapter(this, imageList);
            viewPager.setAdapter(adapter);

            // Встановлюємо початкову позицію
            viewPager.setCurrentItem(startPosition, false);
        } else {
            // Якщо даних зовсім немає
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnClose.setOnClickListener(v -> finish());
    }
}