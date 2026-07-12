package com.your_game_library;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class SeriesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private IgdbAdapter adapter;
    private IgdbApiService igdbApi;
    private String accessToken = null;

    private List<com.your_game_library.IgdbGame> games = new ArrayList<>();
    private boolean isLoading = false;

    private int targetGameId = -1;
    private String targetGameName = "";

    private final String CLIENT_ID = Config.IGDB_CLIENT_ID;
    private final String CLIENT_SECRET = Config.IGDB_CLIENT_SECRET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series);

        targetGameId = getIntent().getIntExtra("GAME_ID", -1);
        targetGameName = getIntent().getStringExtra("GAME_NAME");

        initViews();
        initRetrofit();
        fetchTokenAndLoadInitialGames();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerRawg);
        progressBar = findViewById(R.id.progressRawg);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new IgdbAdapter(this, games, IgdbAdapter.TYPE_GRID);

        recyclerView.setAdapter(adapter);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#121212"));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.igdb.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        igdbApi = retrofit.create(IgdbApiService.class);
    }

    private void fetchTokenAndLoadInitialGames() {
        igdbApi.getToken(CLIENT_ID, CLIENT_SECRET, "client_credentials").enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    accessToken = "Bearer " + response.body().access_token;
                    fetchGames();
                }
            }
            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Toast.makeText(SeriesActivity.this, "Auth Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildIgdbQuery() {
        if (targetGameId == -1) return "";

        // ВАЖЛИВО: використовуємо collection замість series
        return "fields name, " +
                "collection.name, " +
                "collection.games.name, " +
                "collection.games.cover.url, " +
                "collection.games.total_rating, " +
                "collection.games.first_release_date, " +
                "collection.games.summary, " +
                "collection.games.slug, " +
                "franchises.name, " +
                "franchises.games.name, " +
                "franchises.games.cover.url, " +
                "franchises.games.total_rating, " +
                "franchises.games.first_release_date, " +
                "franchises.games.summary, " +
                "franchises.games.slug; " +
                "where id = " + targetGameId + ";";
    }

    private void fetchGames() {
        if (accessToken == null || isLoading || targetGameId == -1) return;

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        // 1. Спершу шукаємо в таблиці КОЛЕКЦІЙ (Collections)
        // Запит знайде колекцію, де в масиві games є ID нашої гри
        String collectionQuery = "fields name, games.name, games.id, games.cover.url, " +
                "games.total_rating, games.first_release_date, games.summary, games.slug; " +
                "where games = (" + targetGameId + ");";

        Log.d("IGDB_SERIES", "Запит до /collections: " + collectionQuery);

        igdbApi.getCollections(CLIENT_ID, accessToken, collectionQuery).enqueue(new Callback<List<IgdbGame.IgdbSeries>>() {
            @Override
            public void onResponse(Call<List<IgdbGame.IgdbSeries>> call, Response<List<IgdbGame.IgdbSeries>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Знайшли серію!
                    processSeriesResult(response.body().get(0));
                } else {
                    // Якщо в колекціях немає, пробуємо шукати у ФРАНШИЗАХ (Franchises)
                    fetchFranchiseData();
                }
            }

            @Override
            public void onFailure(Call<List<IgdbGame.IgdbSeries>> call, Throwable t) {
                Log.e("IGDB_SERIES", "Помилка Collections: " + t.getMessage());
                fetchFranchiseData(); // Пробуємо франшизи навіть при помилці мережі
            }
        });
    }

    // Допоміжний метод для пошуку у франшизах
    private void fetchFranchiseData() {
        String franchiseQuery = "fields name, games.name, games.id, games.cover.url, " +
                "games.total_rating, games.first_release_date, games.summary, games.slug; " +
                "where games = (" + targetGameId + ");";

        Log.d("IGDB_SERIES", "Запит до /franchises: " + franchiseQuery);

        igdbApi.getFranchises(CLIENT_ID, accessToken, franchiseQuery).enqueue(new Callback<List<IgdbGame.IgdbSeries>>() {
            @Override
            public void onResponse(Call<List<IgdbGame.IgdbSeries>> call, Response<List<IgdbGame.IgdbSeries>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    processSeriesResult(response.body().get(0));
                } else {
                    // Нічого не знайшли ні там, ні там
                    stopLoading("No series or franchise found");
                }
            }

            @Override
            public void onFailure(Call<List<IgdbGame.IgdbSeries>> call, Throwable t) {
                stopLoading("Network error: " + t.getMessage());
            }
        });
    }

    // Загальний метод для обробки отриманого списку ігор
    private void processSeriesResult(IgdbGame.IgdbSeries foundSeries) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(foundSeries.name);
        }

        games.clear();
        if (foundSeries.games != null) {
            for (IgdbGame.GameInCollection gc : foundSeries.games) {
                if (gc.id != targetGameId) { // Пропускаємо саму себе
                    IgdbGame conv = new IgdbGame();
                    conv.id = gc.id;
                    conv.name = gc.name;
                    conv.summary = gc.summary;
                    conv.slug = gc.slug;
                    conv.total_rating = gc.total_rating;
                    conv.first_release_date = gc.first_release_date;
                    conv.cover = gc.cover;
                    games.add(conv);
                }
            }
        }

        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        isLoading = false;
    }

    private void stopLoading(String message) {
        Log.d("IGDB_SERIES", message);
        progressBar.setVisibility(View.GONE);
        isLoading = false;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}