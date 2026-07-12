package com.your_game_library;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import net.dankito.readability4j.Readability4J;
import net.dankito.readability4j.Article;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class NewsDetailActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String originalUrl; // Зберігаємо URL для кнопки

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fabOpenBrowser);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidInterface");

        originalUrl = getIntent().getStringExtra("url");

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(originalUrl));
            startActivity(intent);
        });

        // 1. Налаштовуємо статус-бар (чорний фон)
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#121212"));

// 2. Знаходимо тулбар
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

// 3. Увімкнення стрілки назад
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

// 4. Обробка кліку на стрілку
        toolbar.setNavigationOnClickListener(v -> finish());
        if (originalUrl != null) {
            loadCleanArticle(originalUrl);
        }
    }

    private void loadCleanArticle(String url) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // 1. Завантажуємо документ
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .get();

                // 2. Покращене виправлення картинок
                for (org.jsoup.nodes.Element img : doc.select("img")) {
                    String realSrc = img.hasAttr("data-src") ? img.attr("abs:data-src") :
                            img.hasAttr("data-original") ? img.attr("abs:data-original") :
                                    img.attr("abs:src");
                    img.attr("src", realSrc);

                    // Видаляємо атрибути ширини та висоти, щоб вони не обмежували розмір
                    img.removeAttr("width");
                    img.removeAttr("height");
                }

                String html = doc.html();
                Readability4J readability4J = new Readability4J(url, html);
                Article article = readability4J.parse();

                String cleanContent = article.getContent();
                String title = article.getTitle();

                // 3. ОНОВЛЕНИЙ ДИЗАЙН (Justified text + Large Images)
                String finalHtml = "<html><head>" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                        "<style>" +
                        "  body { " +
                        "    background-color: #121212; " +
                        "    color: #E0E0E0; " +
                        "    font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; " +
                        "    padding: 0; " + // Прибираємо падінг у body для картинок
                        "    line-height: 1.7; " +
                        "    font-size: 18px; " +
                        "    text-align: justify; " + // Вирівнювання тексту по обох краях
                        "    -webkit-hyphens: auto; " + // Автоматичні переноси для Android WebView
                        "    hyphens: auto; " +
                        "  } " +
                        "  .container { padding: 20px; } " + // Контейнер для тексту з відступами
                        "  h1 { " +
                        "    color: #FFFFFF; " +
                        "    font-size: 28px; " +
                        "    line-height: 1.2; " +
                        "    margin-bottom: 20px; " +
                        "    font-weight: bold; " +
                        "    text-align: left; " + // Заголовок краще залишити по лівому краю
                        "  } " +
                        "  img { " +
                        "    width: 100% !important; " + // Картинка на всю ширину екрану
                        "    max-width: 100%; " +
                        "    height: auto; " +
                        "    display: block; " +
                        "    margin: 24px 0; " +
                        "    border-radius: 0; " + // Прибираємо заокруглення для ефекту "на весь екран"
                        "  } " +
                        "  p { margin-bottom: 18px; color: #CCCCCC; } " +
                        "  a { color: #58A870; text-decoration: none; font-weight: bold; } " +
                        "  iframe { width: 100%; height: 250px; border: none; margin: 20px 0; } " +
                        "  img { " +
                        "    width: 100% !important; " +
                        "    max-width: 100%; " +
                        "    height: auto; " +
                        "    display: block; " +
                        "    margin: 24px 0; " +
                        "    cursor: pointer; " + // Показує, що на картинку можна натиснути
                        "  } " +
                        "</style></head><body>" +
                        "<div class='container'>" +
                        "  <h1>" + title + "</h1>" +
                        "</div>" +
                        // Виводимо контент (картинки будуть без відступів контейнера, якщо вони не вкладені в p)
                        "<div class='content'>" + cleanContent + "</div>" +
                        "<script>" +
                        "  var images = document.getElementsByTagName('img');" +
                        "  for (var i = 0; i < images.length; i++) {" +
                        "    images[i].onclick = function() {" +
                        "      AndroidInterface.openImage(this.src);" +
                        "    };" +
                        "  }" +
                        "</script>"+
                        "</body></html>";

                runOnUiThread(() -> {
                    // Використовуємо BaseURL для коректних посилань
                    webView.loadDataWithBaseURL(url, finalHtml, "text/html", "utf-8", null);
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    webView.loadUrl(url);
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void openImage(String url) {
            // Викликається при натисканні на картинку
            Intent intent = new Intent(mContext, FullScreenImageActivity.class);
            intent.putExtra("image_url", url);
            mContext.startActivity(intent);
        }
    }
}