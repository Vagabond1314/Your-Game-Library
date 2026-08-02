package com.your_game_library;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SteamSyncActivity extends AppCompatActivity {

    private TextInputEditText etSteamId;
    private Button btnFetchPreview, btnStartSync, btnSelectAll;
    private LinearLayout llInputContainer, llPreviewContainer;
    private TextView tvPreviewGames;
    private RecyclerView rvSteamGames;
    private SearchView svSteamGames;

    private SteamApiService steamApi;
    private List<SteamResponse.SteamGame> fetchedSteamGames = new ArrayList<>();
    private List<SteamResponse.SteamGame> selectedGamesToSync = new ArrayList<>();
    private SteamPreviewAdapter adapter;
    private boolean isAllSelected = true;

    private static final String PREF_NAME = "app_settings";
    private static final String KEY_STEAM_ID = "saved_steam_id";

    private final String STEAM_API_KEY = Config.STEAM_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steam_sync);
        getWindow().setStatusBarColor(Color.parseColor("#121212"));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        Retrofit steamRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.steampowered.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        steamApi = steamRetrofit.create(SteamApiService.class);

        etSteamId = findViewById(R.id.etSteamId);
        btnFetchPreview = findViewById(R.id.btnFetchPreview);
        btnStartSync = findViewById(R.id.btnStartSync);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        llInputContainer = findViewById(R.id.llInputContainer);
        llPreviewContainer = findViewById(R.id.llPreviewContainer);
        tvPreviewGames = findViewById(R.id.tvPreviewGames);
        rvSteamGames = findViewById(R.id.rvSteamGames);
        svSteamGames = findViewById(R.id.svSteamGames);

        rvSteamGames.setLayoutManager(new LinearLayoutManager(this));

        // REMEMBER FEATURE: Auto-fill saved SteamID
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedSteamId = prefs.getString(KEY_STEAM_ID, "");
        if (!savedSteamId.isEmpty()) {
            etSteamId.setText(savedSteamId);
            etSteamId.setSelection(savedSteamId.length());
        }

        btnFetchPreview.setOnClickListener(v -> {
            String steamId = etSteamId.getText().toString().trim();
            if (steamId.length() != 17) {
                etSteamId.setError("Valid 17-digit SteamID64 required");
                return;
            }

            // Save SteamID to memory
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_STEAM_ID, steamId)
                    .apply();

            fetchSteamLibraryPreview(steamId);
        });

        btnSelectAll.setOnClickListener(v -> {
            isAllSelected = !isAllSelected;
            selectedGamesToSync.clear();
            if (isAllSelected) {
                selectedGamesToSync.addAll(fetchedSteamGames);
            }
            if (adapter != null) adapter.notifyDataSetChanged();
        });

        if (svSteamGames != null) {
            svSteamGames.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (adapter != null) adapter.filter(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (adapter != null) adapter.filter(newText);
                    return true;
                }
            });
        }

        btnStartSync.setOnClickListener(v -> {
            if (selectedGamesToSync.isEmpty()) {
                Toast.makeText(this, "Select at least 1 game", Toast.LENGTH_SHORT).show();
                return;
            }

            String gamesJson = new Gson().toJson(selectedGamesToSync);

            Intent serviceIntent = new Intent(this, SteamSyncService.class);
            serviceIntent.putExtra("GAMES_JSON", gamesJson);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(this, "Sync started in background! You can close the app.", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void fetchSteamLibraryPreview(String steamId) {
        btnFetchPreview.setEnabled(false);
        btnFetchPreview.setText("Fetching...");

        steamApi.getOwnedGames(STEAM_API_KEY, steamId, true, "json").enqueue(new Callback<SteamResponse>() {
            @Override
            public void onResponse(Call<SteamResponse> call, Response<SteamResponse> response) {
                btnFetchPreview.setEnabled(true);
                btnFetchPreview.setText("FETCH LIBRARY");

                if (response.isSuccessful() && response.body() != null && response.body().response.games != null) {
                    fetchedSteamGames = response.body().response.games;

                    if (fetchedSteamGames.isEmpty()) {
                        Toast.makeText(SteamSyncActivity.this, "Profile is private or empty.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    selectedGamesToSync.clear();
                    selectedGamesToSync.addAll(fetchedSteamGames);

                    tvPreviewGames.setText("Found " + fetchedSteamGames.size() + " games");

                    adapter = new SteamPreviewAdapter(fetchedSteamGames, selectedGamesToSync);
                    rvSteamGames.setAdapter(adapter);

                    llInputContainer.setVisibility(View.GONE);
                    llPreviewContainer.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(SteamSyncActivity.this, "Error fetching data.", Toast.LENGTH_LONG).show();
                }
            }

            @Override public void onFailure(Call<SteamResponse> call, Throwable t) {
                btnFetchPreview.setEnabled(true);
                btnFetchPreview.setText("FETCH LIBRARY");
                Toast.makeText(SteamSyncActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}