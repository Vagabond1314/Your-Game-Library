package com.your_game_library;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsActivity extends AppCompatActivity {

    private static final String API_KEY = "d75aba39f4d74c88b45eadc29f4e168c";
    private static final String API_KEY2 = "pub_8a0f37e3f4ed48cdada8af49e984e865";
    private String currentCategory = "all";
    private RecyclerView recyclerView;
    private NewsAdapter adapter;

    private AppBarLayout appBarLayout;
    private LinearLayout newsHeader;
    private Toolbar toolbar;
    private boolean isTitleVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        adapter = new NewsAdapter(this);
        recyclerView.setAdapter(adapter);

        ChipGroup chipGroup = findViewById(R.id.chipGroup);

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipGaming) currentCategory = "gaming";
            else if (checkedId == R.id.chipTech) currentCategory = "tech";
            else if (checkedId == R.id.chipFavorites) currentCategory = "favorites";
            else currentCategory = "all";

            loadNews();
        });

        final AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        //final View newsHeader = findViewById(R.id.newsHeader);
        final Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        loadNews();
    }
    private void loadNews() {
        NewsApiService api = ApiClient.getClient().create(NewsApiService.class);

        String query;
        String domains;
        if (currentCategory.equals("favorites")) {
            FavoritesManager favManager = new FavoritesManager(this);
            List<NewsArticle> favs = favManager.getFavorites();

            // ВАЖЛИВО: очищуємо старі дані та встановлюємо лише обрані
            adapter.clearData();
            if (favs.isEmpty()) {
                Toast.makeText(this, "List is empty", Toast.LENGTH_SHORT).show();
            } else {
                adapter.setNews(favs); // Використовуємо setNews для повної заміни
            }
            return;
        }
        String gameSources = "gamespot.com,pcgamer.com,ign.com,eurogamer.net,kotaku.com,gematsu.com,rockpapershotgun.com,gamesradar.com,gamedeveloper.com";
        String techSorces = "theverge.com,techcrunch.com,wired.com,tomshardware.com,9to5google.com,androidauthority.com,cnet.com,gizmodo.com";
        // Визначаємо запит залежно від обраної категорії
        switch (currentCategory) {
            case "gaming":
                query = "-movie -film";
                domains = "gamespot.com,pcgamer.com,ign.com,eurogamer.net,kotaku.com,gematsu.com,rockpapershotgun.com,gamesradar.com,gamedeveloper.com";
                break;
            case "tech":
                query = "-movie -game";
                domains = "theverge.com,techcrunch.com,wired.com,tomshardware.com,9to5google.com,androidauthority.com,cnet.com,gizmodo.com";
                break;
            default: // all
                query = "(gaming OR tech OR hardware OR \"video games\") -movie -film";
                domains = "gamespot.com,pcgamer.com,ign.com,theverge.com,pcgamer.com,techcrunch.com,gamespot.com,tomshardware.com,gizmodo.com";
                break;
        }
        api.getNews(query, domains, "en", "publishedAt", 100, API_KEY)
                .enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {

                            // ОСЬ ТУТ МИ ОГОЛОШУЄМО СПИСОК (це виправляло помилку)
                            List<NewsArticle> allArticles = response.body().getArticles();
                            List<NewsArticle> filtered = new ArrayList<>();

                            if (allArticles != null) {
                                for (NewsArticle article : allArticles) {
                                    if (article.getTitle() == null) continue;

                                    String title = article.getTitle().toLowerCase();

                                    // Фільтруємо непотрібний контент
                                    boolean isMovieOrShow = title.contains("episode") || title.contains("season") ||
                                            title.contains("netflix");

                                    // Якщо це DF або якщо це не фільми — додаємо в список
                                    if (!isMovieOrShow) {
                                        filtered.add(article);
                                    }
                                }
                            }

                            // Тепер змінна filtered існує і заповнена
                            adapter.setNews(filtered);
                            recyclerView.scrollToPosition(0);

                        } else {
                            Toast.makeText(NewsActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        Toast.makeText(NewsActivity.this, "Check internet connection", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
