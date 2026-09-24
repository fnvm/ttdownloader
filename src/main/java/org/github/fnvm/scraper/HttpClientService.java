package org.github.fnvm.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpClientService {
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger log = LoggerFactory.getLogger(HttpClientService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private HttpClientService() {}

    public static HttpClient getClient() {
        return CLIENT;
    }

    public static HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        IOException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean lastAttempt = attempt == MAX_ATTEMPTS;
            try {
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() < 500 || lastAttempt) {
                    return response;
                }

                log.warn(
                        "Request to {} returned status {}, retrying ({}/{})",
                        request.uri(),
                        response.statusCode(),
                        attempt,
                        MAX_ATTEMPTS);
            } catch (IOException e) {
                lastFailure = e;
                if (lastAttempt) {
                    throw e;
                }
                log.warn(
                        "Request to {} failed: {}, retrying ({}/{})",
                        request.uri(),
                        e.getMessage(),
                        attempt,
                        MAX_ATTEMPTS);
            }

            Thread.sleep(RETRY_DELAY.toMillis() * attempt);
        }

        throw lastFailure;
    }
}
