package com.your_game_library;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.HttpUrl;

public class RawgClient {

    private static final String TAG = "RawgClient";
    private static final String API_KEY = "";
    private static final String BASE_URL = "";

    private final OkHttpClient client = new OkHttpClient();

    public interface CallbackGames {
        void onSuccess(List<RawgGame> games);
        void onError(String error);
    }

    // ==================== Фетч списку ігор ====================
    public void fetchGames(int page, int pageSize, String params, CallbackGames callback) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL).newBuilder();
            urlBuilder.addQueryParameter("key", API_KEY);
            urlBuilder.addQueryParameter("page", String.valueOf(page));
            urlBuilder.addQueryParameter("page_size", String.valueOf(pageSize));

            if (params != null && !params.isEmpty()) {
                String[] pairs = params.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        urlBuilder.addQueryParameter(kv[0], kv[1]);
                    }
                }
            }

            String url = urlBuilder.build().toString();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
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
                            String slug = g.optString("slug", null);

                            list.add(new RawgGame(id, name, bg, released, rating, slug));
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

    // ==================== Пошук гри за назвою ====================
    public void findGameByName(String name, CallbackGames callback) {
        if (name == null || name.trim().isEmpty()) {
            callback.onError("Empty search query");
            return;
        }

        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL).newBuilder();
            urlBuilder.addQueryParameter("key", API_KEY);
            urlBuilder.addQueryParameter("search", name.trim());
            urlBuilder.addQueryParameter("page_size", "20"); // обмеження результатів

            String url = urlBuilder.build().toString();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
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
                            String gameName = g.optString("name", "Unknown");
                            String bg = g.optString("background_image", null);
                            String released = g.optString("released", null);
                            float rating = (float) g.optDouble("rating", 0.0);
                            String slug = g.optString("slug", null);

                            list.add(new RawgGame(id, gameName, bg, released, rating, slug));
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