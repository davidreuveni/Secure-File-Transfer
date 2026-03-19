package com.davidr.secureft.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class utils {
    private static final String MELACHA_BLOCKED_MESSAGE = "Blocked: issur melacha is currently in effect";

    public static void verifyMelacha() throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://www.hebcal.com/zmanim?cfg=json&im=1&latitude=32.0853&longitude=34.7818&tzid=Asia/Jerusalem"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return;
            }

            JsonNode data = new ObjectMapper().readTree(response.body());
            JsonNode melachaStatus = data.path("status").path("isAssurBemlacha");

            if (!melachaStatus.isBoolean()) {
                return;
            }

            if (melachaStatus.asBoolean()) {
                throw new IOException(MELACHA_BLOCKED_MESSAGE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (MELACHA_BLOCKED_MESSAGE.equals(e.getMessage())) {
                throw e;
            }
        }
    }
}
