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

public class SimilarGamesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private IgdbAdapter adapter;
    private IgdbApiService igdbApi;
    private String accessToken = null;

    private List<IgdbGame> games = new ArrayList<>();
    private boolean isLoading = false;

    private int targetGameId = -1;
    private String targetGameName = "";

    private final String CLIENT_ID = Config.IGDB_CLIENT_ID;
    private final String CLIENT_SECRET = Config.IGDB_CLIENT_SECRET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_similar_games);

        // Отримання даних з Intent
        targetGameId = getIntent().getIntExtra("GAME_ID", -1);
        targetGameName = getIntent().getStringExtra("GAME_NAME");

        initViews();
        initRetrofit();
        fetchTokenAndLoadInitialGames();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerRawg);
        progressBar = findViewById(R.id.progressRawg);

// Наприклад, сітка на 3 колонки
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new IgdbAdapter(this, games, IgdbAdapter.TYPE_GRID);

        recyclerView.setAdapter(adapter);

        // Колір статус-бару
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#121212"));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Similar to " + (targetGameName != null ? targetGameName : "Game"));
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
                Toast.makeText(SimilarGamesActivity.this, "Auth Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildIgdbQuery() {
        if (targetGameId == -1) return "";

        // Запитуємо вкладені поля схожих ігор
        return "fields similar_games.name, " +
                "similar_games.cover.url, " +
                "similar_games.total_rating, " +
                "similar_games.first_release_date, " +
                "similar_games.summary, " +
                "similar_games.slug; " +
                "where id = " + targetGameId + ";";
    }

    private void fetchGames() {
        if (accessToken == null || isLoading || targetGameId == -1) return;

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        String body = buildIgdbQuery();

        igdbApi.getGames(CLIENT_ID, accessToken, body).enqueue(new Callback<List<IgdbGame>>() {
            @Override
            public void onResponse(Call<List<IgdbGame>> call, Response<List<IgdbGame>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                    IgdbGame rootGame = response.body().get(0);

                    if (rootGame.similar_games != null && !rootGame.similar_games.isEmpty()) {
                        games.clear();

                        for (IgdbGame.SimilarGame sg : rootGame.similar_games) {
                            IgdbGame convertedGame = new IgdbGame();
                            convertedGame.id = sg.id;
                            convertedGame.name = sg.name;
                            convertedGame.summary = sg.summary;
                            convertedGame.slug = sg.slug;
                            convertedGame.total_rating = sg.total_rating;
                            convertedGame.first_release_date = sg.first_release_date;
                            convertedGame.cover = sg.cover;

                            games.add(convertedGame);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(SimilarGamesActivity.this, "No similar games found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("IGDB_ERROR", "Response error or empty body");
                }
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }

            @Override
            public void onFailure(Call<List<IgdbGame>> call, Throwable t) {
                Log.e("IGDB_FAILURE", t.getMessage());
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }
        });
    }
}