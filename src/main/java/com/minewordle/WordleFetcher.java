package com.minewordle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class WordleFetcher {

    private static final String URL_TEMPLATE = "https://www.nytimes.com/svc/wordle/v2/%s.json";

    public static CompletableFuture<String> fetchTodaysSolution() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String date = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
                String url = String.format(URL_TEMPLATE, date);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible)");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) body.append(line);
                    }
                    String text = body.toString();
                    int idx = text.indexOf("\"solution\":\"");
                    if (idx >= 0) {
                        int start = idx + 12;
                        int end = text.indexOf('"', start);
                        if (end > start) {
                            return text.substring(start, end).toUpperCase();
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        });
    }
}
