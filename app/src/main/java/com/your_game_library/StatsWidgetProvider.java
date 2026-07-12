package com.your_game_library;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class StatsWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d("WIDGET_DEBUG", "Виклик updateAppWidget для ID: " + appWidgetId);

        try {
            GameDatabaseHelper db = GameDatabaseHelper.getInstance(context);
            GameDatabaseHelper.WidgetStats stats = db.getStatsForWidget();

            if (stats == null) {
                Log.e("WIDGET_DEBUG", "Критична помилка: об'єкт stats дорівнює NULL");
                return;
            }

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_stats);

            // Встановлюємо текст
            views.setTextViewText(R.id.wPlayingCount, String.valueOf(stats.playing));
            views.setTextViewText(R.id.wPlannedCount, String.valueOf(stats.planned));
            views.setTextViewText(R.id.wPlayedCount, String.valueOf(stats.completed));
            views.setTextViewText(R.id.wTotalTime, "Total Time: " + stats.totalTime + "h");

            // Клік
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            Log.d("WIDGET_DEBUG", "Дані успішно відправлені в AppWidgetManager для ID: " + appWidgetId);

        } catch (Exception e) {
            Log.e("WIDGET_DEBUG", "ПОМИЛКА всередині updateAppWidget: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
