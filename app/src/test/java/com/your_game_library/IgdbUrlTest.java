package com.your_game_library;

import org.junit.Test;
import static org.junit.Assert.*;

public class IgdbUrlTest {

    // Тестуємо логіку перетворення посилання
    @Test
    public void testUrlFormatting() {
        String rawUrl = "//images.igdb.com/igdb/image/upload/t_thumb/co1r3v.jpg";
        String expectedUrl = "https://images.igdb.com/igdb/image/upload/t_1080p/co1r3v.jpg";

        // Викликаємо ваш метод (якщо він статичний або через об'єкт)
        String result = formatUrl(rawUrl, "t_1080p");

        assertEquals("URL має починатися з https і мати правильний розмір", expectedUrl, result);
    }

    private String formatUrl(String url, String sizeTag) {
        if (url.startsWith("//")) url = "https:" + url;
        return url.replaceAll("t_\\w+", sizeTag);
    }
}