package org.github.fnvm.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.github.fnvm.data.QualityPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class DirectPostProvider {
    private static final Logger log = LoggerFactory.getLogger(DirectPostProvider.class);

    public static JsonNode getResponse(String link, QualityPreference quality)
            throws UrlScrapingException {

        String qualityParam = switch (quality) {
            case HD -> "&hd=1";
            case SD -> "";
            case FULLHD -> {
                throw new UrlScrapingException("Unsupported quality for direct scraper");
            }
        };

        try {
            var requestBody = "url=" + URLEncoder.encode(link, StandardCharsets.UTF_8) + qualityParam;

            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://tikwm.com/api/"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            HttpRequest request = requestBuilder.build();
            HttpClient client = HttpClientService.getClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            var mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            int code = root.path("code").asInt(-1);
            if (code != 0) {
                String message = root.path("msg").asText("Unknown error");
                log.error("API returned error code {}: {}", code, message);
                throw new UrlScrapingException("API error: " + message);
            }

            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode()) {
                throw new UrlScrapingException("Response missing data field");
            }

            return dataNode;

        } catch (IOException e) {
            log.error("Failed to read response: {}", e.getMessage(), e);
            throw new UrlScrapingException("Failed to read response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request was interrupted: {}", e.getMessage(), e);
            throw new UrlScrapingException("Request was interrupted", e);
        }
    }
}
