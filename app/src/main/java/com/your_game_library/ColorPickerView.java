package com.your_game_library;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    private Paint paint;
    private Paint cpPaint;
    // ПРАВИЛЬНИЙ ПОРЯДОК КОЛЬОРІВ ДЛЯ HSV:
    private int[] hueColors = {
            Color.RED,      // 0°
            Color.YELLOW,   // 60°
            Color.GREEN,    // 120°
            Color.CYAN,     // 180°
            Color.BLUE,     // 240°
            Color.MAGENTA,  // 300°
            Color.RED       // 360°
    };
    private float[] hsv = {0f, 1f, 1f};
    private OnColorChangedListener listener;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cpPaint.setStyle(Paint.Style.STROKE);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float paletteHeight = height * 0.80f; // Поле палітри
        float sliderTop = height * 0.88f;    // Початок слайдера
        float sliderBottom = height;         // Кінець слайдера

        // 1. Малюємо основне поле кольору (Saturation/Value)
        // Горизонтальний градієнт: Білий -> Чистий вибраний колір
        paint.setShader(new LinearGradient(0, 0, width, 0,
                Color.WHITE, Color.HSVToColor(new float[]{hsv[0], 1f, 1f}), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, paletteHeight, paint);

        // Вертикальний градієнт: Прозорий -> Чорний (яскравість)
        paint.setShader(new LinearGradient(0, 0, 0, paletteHeight,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, paletteHeight, paint);

        // 2. Малюємо смужку вибору відтінку (Hue) - Веселка
        paint.setShader(new LinearGradient(0, 0, width, 0, hueColors, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(0, sliderTop, width, sliderBottom, 15, 15, paint);

        // 3. Малюємо індикатор (паличку) для відтінку
        float hueX = (hsv[0] / 360f) * width;
        cpPaint.setStrokeWidth(8);
        cpPaint.setColor(Color.BLACK);
        canvas.drawLine(hueX, sliderTop - 4, hueX, sliderBottom + 4, cpPaint);
        cpPaint.setStrokeWidth(4);
        cpPaint.setColor(Color.WHITE);
        canvas.drawLine(hueX, sliderTop, hueX, sliderBottom, cpPaint);

        // 4. Малюємо кружечок на основній палітрі (S/V)
        float x = hsv[1] * width;
        float y = (1f - hsv[2]) * paletteHeight;

        // Малюємо чорну обводку
        cpPaint.setStyle(Paint.Style.STROKE);
        cpPaint.setStrokeWidth(6);
        cpPaint.setColor(Color.BLACK);
        canvas.drawCircle(x, y, 22, cpPaint);

        // Малюємо основне біле кільце
        cpPaint.setStrokeWidth(4);
        cpPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, 20, cpPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float width = getWidth();
        float height = getHeight();
        float paletteHeight = height * 0.80f;
        float sliderTop = height * 0.88f;

        // Обмежуємо координати межами в'юшки
        x = Math.max(0, Math.min(x, width));
        y = Math.max(0, Math.min(y, height));

        if (y <= paletteHeight) {
            // Клік по основному полю (Saturation та Value)
            hsv[1] = x / width;
            hsv[2] = 1f - (y / paletteHeight);
        } else if (y >= sliderTop - 20) {
            // Клік по смужці веселки (Hue)
            hsv[0] = (x / width) * 360f;
        }

        int selectedColor = Color.HSVToColor(hsv);
        if (listener != null) listener.onColorChanged(selectedColor);

        invalidate();
        return true;
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        invalidate();
    }
}