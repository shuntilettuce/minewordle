package com.minewordle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (compatible)")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    int idx = body.indexOf("\"solution\":\"");
                    if (idx >= 0) {
                        int start = idx + 12;
                        int end = body.indexOf('"', start);
                        if (end > start) {
                            return body.substring(start, end).toUpperCase();
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
