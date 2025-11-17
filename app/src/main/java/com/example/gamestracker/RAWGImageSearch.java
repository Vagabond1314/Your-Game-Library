package com.example.gamestracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RAWGImageSearch {

    private static final String API_KEY = "53e4f996a60a48a7b08f49a75587fd3d"; // вставити свій ключ
    private static final String BASE_URL = "https://api.rawg.io/api/games?key=" + API_KEY + "&search=";

    private Context context;

    public interface OnImagesLoadedListener {
        void onImagesLoaded(List<Bitmap> images);
        void onError(String error);
    }

    public RAWGImageSearch(Context context) {
        this.context = context;
    }

    public void searchImages(@NonNull String query, @NonNull OnImagesLoadedListener listener) {
        new AsyncTask<String, Void, List<Bitmap>>() {
            private String errorMessage = null;

            @Override
            protected List<Bitmap> doInBackground(String... params) {
                List<Bitmap> resultImages = new ArrayList<>();
                try {
                    String searchQuery = params[0].replace(" ", "%20");
                    URL url = new URL(BASE_URL + searchQuery);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    InputStream inputStream = connection.getInputStream();
                    Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";

                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray results = jsonObject.getJSONArray("results");

                    for (int i = 0; i < results.length() && i < 10; i++) { // максимум 10 зображень
                        JSONObject game = results.getJSONObject(i);
                        if (game.has("background_image") && !game.isNull("background_image")) {
                            String imageUrl = game.getString("background_image");
                            Bitmap bitmap = downloadImage(imageUrl);
                            if (bitmap != null) resultImages.add(bitmap);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                }
                return resultImages;
            }

            @Override
            protected void onPostExecute(List<Bitmap> bitmaps) {
                if (bitmaps.isEmpty() && errorMessage != null) {
                    listener.onError(errorMessage);
                } else {
                    listener.onImagesLoaded(bitmaps);
                }
            }
        }.execute(query);
    }

    private Bitmap downloadImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
