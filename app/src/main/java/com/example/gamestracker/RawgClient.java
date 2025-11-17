package com.example.gamestracker;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RawgClient {
    private static final String TAG = "RawgClient";
    private static final String API_KEY = "YOUR_RAWG_API_KEY"; // <- встав свій ключ
    private static final String BASE = "https://api.rawg.io/api/games";

    private OkHttpClient client = new OkHttpClient();

    public interface CallbackGames {
        void onSuccess(List<RawgGame> games);
        void onError(String error);
    }

    /**
     * Параметр queryParams — це частина запиту після ? наприклад:
     * "ordering=-rating&page_size=20" або "search=witcher&page_size=20"
     */
    public void fetchGames(String queryParams, CallbackGames callback) {
        try {
            String url = BASE + "?key=" + API_KEY;
            if (queryParams != null && !queryParams.isEmpty()) {
                url += "&" + URLEncoder.encode(queryParams, "UTF-8").replace("+", "%20");
            }

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP " + response.code());
                        return;
                    }
                    try {
                        String body = response.body().string();
                        JSONObject root = new JSONObject(body);
                        JSONArray results = root.getJSONArray("results");
                        List<RawgGame> list = new ArrayList<>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject g = results.getJSONObject(i);
                            int id = g.optInt("id", -1);
                            String name = g.optString("name", "Unknown");
                            String bg = g.optString("background_image", null);
                            String released = g.optString("released", null);
                            float rating = (float) g.optDouble("rating", 0.0);
                            list.add(new RawgGame(id, name, bg, released, rating));
                        }
                        callback.onSuccess(list);
                    } catch (Exception e) {
                        Log.e(TAG, "parse error", e);
                        callback.onError(e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}
