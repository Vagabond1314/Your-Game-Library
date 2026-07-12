package com.your_game_library;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomPickerActivity extends AppCompatActivity {

    private Spinner spinnerSource;
    private ProgressBar progressBar;
    private WheelView wheelView;
    private TextView tvResultName, tvManageEntriesLabel;
    private Button btnSpin, btnOpenGame;
    private RecyclerView recyclerWheelGames;
    private View layoutManageHeader;
    private View btnAddGameToWheel;

    private GameDatabaseHelper dbHelper;
    private List<String> sourceNames = new ArrayList<>();
    private List<GameCollection> allCollections = new ArrayList<>();

    private List<WheelItem> currentItemsPool = new ArrayList<>();
    private Game winningGame = null;

    private boolean isSpinning = false;
    private WheelGameAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_picker);

        dbHelper = GameDatabaseHelper.getInstance(this);

        initViews();
        setupToolbar();
        loadSources();

        btnSpin.setOnClickListener(v -> startSpinning());

        btnOpenGame.setOnClickListener(v -> {
            if (winningGame != null) {
                Intent intent = new Intent(this, MyGameDetailsActivity.class);
                intent.putExtra("gameId", winningGame.getId());
                startActivity(intent);
            }
        });
    }

    private void initViews() {
        spinnerSource = findViewById(R.id.spinnerSource);
        progressBar = findViewById(R.id.progressBar);
        wheelView = findViewById(R.id.wheelView);
        tvResultName = findViewById(R.id.tvResultName);
        btnSpin = findViewById(R.id.btnSpin);
        btnOpenGame = findViewById(R.id.btnOpenGame);
        layoutManageHeader = findViewById(R.id.layoutManageHeader);
        btnAddGameToWheel = findViewById(R.id.btnAddGameToWheel);

        btnAddGameToWheel.setOnClickListener(v -> showAddGameDialog()); // Додаємо клік

        recyclerWheelGames = findViewById(R.id.recyclerWheelGames);
        recyclerWheelGames.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadSources() {
        sourceNames.clear();
        sourceNames.add("Select Category..."); // НОВИЙ ПУНКТ (Індекс 0)
        sourceNames.add("All Games");           // Індекс 1
        sourceNames.add("Planned Games");      // Індекс 2
        sourceNames.add("Playing Currently");  // Індекс 3

        allCollections = dbHelper.getAllCollections();
        for (GameCollection col : allCollections) {
            sourceNames.add("Collection: " + col.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sourceNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(spinnerAdapter);

        // Ставимо початковий стан кнопок
        btnSpin.setEnabled(false);
        btnSpin.setAlpha(0.5f);

        spinnerSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // Якщо вибрано "Select Category..."
                    clearWheel();
                } else {
                    updateGamesPool(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void clearWheel() {
        currentItemsPool.clear();
        wheelView.setData(currentItemsPool);
        layoutManageHeader.setVisibility(View.VISIBLE);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        btnSpin.setEnabled(false);
        btnSpin.setAlpha(0.5f);
        tvResultName.setText("Pick a category first!");

        // Ховаємо список знизу
        recyclerWheelGames.setVisibility(View.GONE);
        if(tvManageEntriesLabel != null) tvManageEntriesLabel.setVisibility(View.GONE);
    }

    private void updateGamesPool(int position) {
        // Показуємо завантаження
        progressBar.setVisibility(View.VISIBLE);
        wheelView.setVisibility(View.GONE);
        recyclerWheelGames.setVisibility(View.GONE);
        if(tvManageEntriesLabel != null) tvManageEntriesLabel.setVisibility(View.GONE);
        layoutManageHeader.setVisibility(View.GONE);
        btnSpin.setEnabled(false);
        btnSpin.setAlpha(0.5f);
        btnOpenGame.setVisibility(View.GONE);
        tvResultName.setText("Loading...");

        // Робимо запит до БД у фоновому потоці, щоб уникнути зависання UI
        new Thread(() -> {
            List<Game> rawGames;
            if (position == 1) { // 1 - це тепер Planned
                rawGames = dbHelper.getAllGames();
            }
            else if (position == 2) { // 2 - це Playing
                rawGames = dbHelper.getGamesByCategoryObject("planned");
            } else if (position == 3) { // 2 - це Playing
                rawGames = dbHelper.getGamesByCategoryObject("playing");
            } else {
                GameCollection selectedCol = allCollections.get(position - 4);
                rawGames = dbHelper.getGamesInCollection(selectedCol.getId());
            }

            currentItemsPool.clear();
            for (Game game : rawGames) {
                currentItemsPool.add(new WheelItem(game));
            }

            // Повертаємось на головний потік для оновлення UI
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                wheelView.setVisibility(View.VISIBLE);

                adapter = new WheelGameAdapter(currentItemsPool, () -> {
                    wheelView.setData(currentItemsPool);
                    checkIfCanSpin();
                });
                recyclerWheelGames.setAdapter(adapter);

                wheelView.setData(currentItemsPool);
                checkIfCanSpin();

                // Показуємо список
                if (!currentItemsPool.isEmpty()) {
                    recyclerWheelGames.setVisibility(View.VISIBLE);
                    if(tvManageEntriesLabel != null) tvManageEntriesLabel.setVisibility(View.VISIBLE);
                }
                layoutManageHeader.setVisibility(View.VISIBLE);
            });
        }).start();
    }
    private void showAddGameDialog() {
        // 1. Створюємо діалог
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.MyDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_search_wheel, null);
        builder.setView(view);

        android.widget.EditText etSearch = view.findViewById(R.id.etSearchGame);
        RecyclerView rvResults = view.findViewById(R.id.rvSearchResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        // 2. Отримуємо всі ігри з бази (у фоні, щоб не зависало)
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        new Thread(() -> {
            List<Game> allGames = dbHelper.getAllGames();

            // Видаляємо з результатів пошуку ті ігри, які ВЖЕ є в колесі
            List<Game> availableGames = new ArrayList<>();
            for (Game g : allGames) {
                boolean alreadyInWheel = false;
                for (WheelItem item : currentItemsPool) {
                    if (item.getGame().getId() == g.getId()) {
                        alreadyInWheel = true;
                        break;
                    }
                }
                if (!alreadyInWheel) availableGames.add(g);
            }

            runOnUiThread(() -> {
                // Створюємо простий внутрішній адаптер для результатів пошуку
                SearchAdapter searchAdapter = new SearchAdapter(availableGames, selectedGame -> {
                    // Коли гру вибрано:
                    currentItemsPool.add(new WheelItem(selectedGame));

                    // Якщо список був порожній, створюємо новий WheelGameAdapter
                    if (adapter == null) {
                        adapter = new WheelGameAdapter(currentItemsPool, () -> {
                            wheelView.setData(currentItemsPool);
                            checkIfCanSpin();
                        });
                        recyclerWheelGames.setAdapter(adapter);
                    } else {
                        adapter.notifyItemInserted(currentItemsPool.size() - 1);
                    }

                    wheelView.setData(currentItemsPool);
                    checkIfCanSpin();
                    dialog.dismiss(); // Закриваємо діалог

                    // Робимо список видимим, якщо він був схований
                    recyclerWheelGames.setVisibility(View.VISIBLE);
                });

                rvResults.setAdapter(searchAdapter);

                // Додаємо пошук по тексту
                etSearch.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchAdapter.filter(s.toString());
                    }
                    @Override public void afterTextChanged(android.text.Editable s) {}
                });
            });
        }).start();

        dialog.show();
    }
    private void checkIfCanSpin() {
        boolean hasActiveGames = false;
        for (WheelItem item : currentItemsPool) {
            if (item.isIncluded()) {
                hasActiveGames = true;
                break;
            }
        }

        if (!hasActiveGames) {
            btnSpin.setEnabled(false);
            btnSpin.setAlpha(0.5f);
            tvResultName.setText("No active games selected!");
        } else {
            btnSpin.setEnabled(true);
            btnSpin.setAlpha(1.0f);
            tvResultName.setText("Ready to spin!");
        }
    }

    private void startSpinning() {
        List<WheelItem> activeItems = new ArrayList<>();
        float totalWeight = 0;

        for (WheelItem item : currentItemsPool) {
            if (item.isIncluded()) {
                activeItems.add(item);
                totalWeight += item.getWeight();
            }
        }

        if (isSpinning || activeItems.isEmpty()) return;

        isSpinning = true;
        btnSpin.setEnabled(false);
        btnOpenGame.setVisibility(View.GONE);
        spinnerSource.setEnabled(false);

        recyclerWheelGames.setAlpha(0.5f);

        Random random = new Random();

        float randomValue = random.nextFloat() * totalWeight;
        float currentWeightSum = 0;
        int winningIndex = -1;

        for (int i = 0; i < activeItems.size(); i++) {
            currentWeightSum += activeItems.get(i).getWeight();
            if (randomValue <= currentWeightSum) {
                winningIndex = i;
                break;
            }
        }

        if (winningIndex == -1) winningIndex = activeItems.size() - 1;

        WheelItem winningWheelItem = activeItems.get(winningIndex);
        winningGame = winningWheelItem.getGame();

        float offsetFromZero = 0;
        for (int i = 0; i < winningIndex; i++) {
            offsetFromZero += (activeItems.get(i).getWeight() / totalWeight) * 360f;
        }

        float winnerSweepAngle = (winningWheelItem.getWeight() / totalWeight) * 360f;
        float centerOffset = offsetFromZero + (winnerSweepAngle / 2f);
        float randomOffset = (random.nextFloat() - 0.5f) * (winnerSweepAngle * 0.8f);

        float targetAngle = 3600f + 360f - centerOffset + randomOffset;

        RotateAnimation rotate = new RotateAnimation(0, targetAngle,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);

        rotate.setDuration(5000);
        rotate.setInterpolator(new android.view.animation.PathInterpolator(0.1f, 0.9f, 0.2f, 1.0f));
        rotate.setFillAfter(true);

        rotate.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation animation) {}
            @Override public void onAnimationRepeat(android.view.animation.Animation animation) {}

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                tvResultName.setText("🎉 " + winningGame.getName() + " 🎉");
                tvResultName.setTextColor(android.graphics.Color.parseColor("#fc6f03"));

                btnSpin.setEnabled(true);
                btnSpin.setText("SPIN AGAIN");
                spinnerSource.setEnabled(true);
                btnOpenGame.setVisibility(View.VISIBLE);
                recyclerWheelGames.setAlpha(1.0f);
                isSpinning = false;
            }
        });

        wheelView.startAnimation(rotate);
        tvResultName.setText("Good Luck!");
        tvResultName.setTextColor(android.graphics.Color.WHITE);
    }

    // --- Внутрішній клас-адаптер для діалогу пошуку ---
    public interface OnGameSelectedListener {
        void onGameSelected(Game game);
    }
    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private List<Game> fullList;
        private List<Game> filteredList;
        private OnGameSelectedListener listener;

        public SearchAdapter(List<Game> games, OnGameSelectedListener listener) {
            this.fullList = games;
            this.filteredList = new ArrayList<>(games);
            this.listener = listener;
        }

        public void filter(String query) {
            filteredList.clear();
            if (query.isEmpty()) {
                filteredList.addAll(fullList);
            } else {
                for (Game g : fullList) {
                    if (g.getName().toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(g);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(16, 24, 16, 24);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setTextSize(18f);

            // Ефект натискання
            android.util.TypedValue outValue = new android.util.TypedValue();
            parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tv.setBackgroundResource(outValue.resourceId);

            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            Game game = filteredList.get(position);
            holder.tv.setText(game.getName());
            holder.tv.setOnClickListener(v -> listener.onGameSelected(game));
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            ViewHolder(View v) { super(v); tv = (TextView) v; }
        }
    }
}