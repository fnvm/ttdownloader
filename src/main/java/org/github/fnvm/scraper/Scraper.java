package org.github.fnvm.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.github.fnvm.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Scraper {
    private static final Logger log = LoggerFactory.getLogger(Scraper.class);

    public static Content getContent(String link, QualityPreference quality, String cookies) throws UrlScrapingException {
        try {
            link = (UrlResolver.isShortLink(link))
                    ? UrlResolver.resolveShortUrl(link)
                    : link;
        } catch (URISyntaxException e) {
            log.error("Invalid URL syntax: {}", e.getMessage(), e);
            throw new UrlScrapingException("Invalid URL format", e);
        } catch (IOException e) {
            log.error("Network error resolving URL: {}", e.getMessage(), e);
            throw new UrlScrapingException("Network error during URL resolution", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("URL resolution was interrupted");
            throw new UrlScrapingException("URL resolution was interrupted", e);
        }

        JsonNode response;
        ContentType probablyType = UrlResolver.getContentType(link);

        switch (probablyType) {
            case VIDEO, PHOTO -> {
                response = getResponse(link, quality);
                boolean isPhoto = !response.path("images").asText("").isEmpty();
                if (isPhoto) {
                    return getPhotoInfo(response);
                }
                return getVideoInfo(response);
            }
            default -> throw new UrlScrapingException("Unsupported content type");
        }
    }


    private static Content getVideoInfo(JsonNode data) throws UrlScrapingException {
        boolean hasHd = data.hasNonNull("hdplay") && !data.path("hdplay").asText().isEmpty();

        String videoPath = hasHd ? "hdplay" : "play";
        String sizePath  = hasHd ? "hd_size" : "size";

        String videoUrl = data.path(videoPath).asText("");
        if (videoUrl.isEmpty()) {
            throw new UrlScrapingException("Video URL is missing");
        }

        long sizeBytes = data.path(sizePath).asLong(0);
        String title = data.path("title").asText("");

        QualityPreference actualQuality = hasHd
                ? QualityPreference.HD
                : QualityPreference.SD;

        return new VideoContent(
                videoUrl,
                sizeBytes,
                title,
                actualQuality
        );
    }

    private static Content getPhotoInfo(JsonNode data) {
        JsonNode imagesNode = data.path("images");

        List<String> photos = new ArrayList<>();

        if (imagesNode.isArray()) {
            imagesNode.forEach(node -> addUrlIfValid(photos, node.asText("")));
        } else {
            addUrlIfValid(photos, imagesNode.asText(""));
        }

        return new PhotoContent(photos);
    }

    private static void addUrlIfValid(List<String> photos, String url) {
        if (url != null && !url.isEmpty()) {
            photos.add(url);
        }
    }

    private static JsonNode getResponse(String link, QualityPreference quality) throws UrlScrapingException {
        String qualSet = switch (quality) {
            case HD, FULLHD -> "&hd=1";
            case null, default -> "";
        };

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://tikwm.com/api/"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "url="
                                    + URLEncoder.encode(link, StandardCharsets.UTF_8)
                                    + qualSet))
                    .build();

            HttpClient client = HttpClientService.getClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            if (root.path("code").asInt() != 0) {
                throw new UrlScrapingException("Failed to get response");
            }

            return root.path("data");
        } catch (IOException e) {
            log.error("Failed to read response: {}", e.getMessage(), e);
            throw new UrlScrapingException("Failed to read response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to send request: {}", e.getMessage(), e);
            throw new UrlScrapingException("Failed to send request", e);
        }

    }

}
