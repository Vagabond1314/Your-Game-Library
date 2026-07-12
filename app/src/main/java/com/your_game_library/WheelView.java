package com.your_game_library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class WheelView extends View {
    private List<WheelItem> items = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();

    private int[] colors = {
            Color.parseColor("#58A870"), Color.parseColor("#fc6f03"),
            Color.parseColor("#9146FF"), Color.parseColor("#FF4242"),
            Color.parseColor("#2D5E85"), Color.parseColor("#FFC107")
    };

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(List<WheelItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            for (WheelItem item : newItems) {
                if (item.isIncluded()) {
                    this.items.add(item);
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        int radius = Math.min(width, height) / 2 - 40;
        rectF.set(width / 2 - radius, height / 2 - radius, width / 2 + radius, height / 2 + radius);

        if (items.isEmpty()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.DKGRAY);
            paint.setStrokeWidth(5f);
            canvas.drawCircle(width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(40f);
            paint.setColor(Color.GRAY);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Empty Wheel", width / 2, height / 2, paint);
            return;
        }

        float totalWeight = 0;
        for (WheelItem item : items) totalWeight += item.getWeight();

        float startAngle = -90f;
        float dynamicTextSize = Math.max(8f, Math.min(35f, 900f / items.size()));

        for (int i = 0; i < items.size(); i++) {
            WheelItem item = items.get(i);
            float sweepAngle = (item.getWeight() / totalWeight) * 360f;

            // 1. Малюємо сектор
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i % colors.length]);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            // 2. Малюємо текст
            paint.setColor(Color.WHITE);
            paint.setTextSize(dynamicTextSize);
            paint.setTextAlign(Paint.Align.RIGHT);

            canvas.save();

            // ОСЬ ВИПРАВЛЕНИЙ РЯДОК (прибрано + 90f, тепер текст рівно по центру свого сектора)
            canvas.rotate(startAngle + (sweepAngle / 2f), width / 2, height / 2);

            String name = item.getGame().getName();
            int maxLength = items.size() > 30 ? 12 : 18;
            if (name.length() > maxLength) name = name.substring(0, maxLength - 2) + "..";

            float textOffset = dynamicTextSize / 3;
            canvas.drawText(name, width / 2 + radius - 20, height / 2 + textOffset, paint);
            canvas.restore();

            startAngle += sweepAngle;
        }

        // Обводка
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(8f);
        canvas.drawCircle(width / 2, height / 2, radius, paint);

        // Крапка в центрі
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(width / 2, height / 2, 15, paint);
    }
}