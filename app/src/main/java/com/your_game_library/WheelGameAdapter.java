package com.your_game_library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WheelGameAdapter extends RecyclerView.Adapter<WheelGameAdapter.ViewHolder> {
    private List<WheelItem> items;
    private Runnable onWheelDataChanged; // Колбек для оновлення колеса

    public WheelGameAdapter(List<WheelItem> items, Runnable onWheelDataChanged) {
        this.items = items;
        this.onWheelDataChanged = onWheelDataChanged;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wheel_game, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WheelItem item = items.get(position);
        holder.tvGameName.setText(item.getGame().getName());
        holder.cbInclude.setChecked(item.isIncluded());
        holder.tvWeight.setText(String.valueOf(item.getWeight()));

        // Виключаємо/Включаємо гру
        holder.cbInclude.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setIncluded(isChecked);
            onWheelDataChanged.run(); // Оновлюємо колесо
        });

        // Зменшуємо вагу (мінімум 1)
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getWeight() > 1) {
                item.setWeight(item.getWeight() - 1);
                holder.tvWeight.setText(String.valueOf(item.getWeight()));
                onWheelDataChanged.run(); // Оновлюємо колесо
            }
        });

        // Збільшуємо вагу (максимум 10, наприклад)
        holder.btnPlus.setOnClickListener(v -> {
            if (item.getWeight() < 10) {
                item.setWeight(item.getWeight() + 1);
                holder.tvWeight.setText(String.valueOf(item.getWeight()));
                onWheelDataChanged.run(); // Оновлюємо колесо
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGameName, btnMinus, btnPlus, tvWeight;
        CheckBox cbInclude;
        ViewHolder(View v) {
            super(v);
            tvGameName = v.findViewById(R.id.tvGameName);
            cbInclude = v.findViewById(R.id.cbInclude);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnPlus = v.findViewById(R.id.btnPlus);
            tvWeight = v.findViewById(R.id.tvWeight);
        }
    }
}