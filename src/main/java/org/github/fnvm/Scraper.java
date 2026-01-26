package org.github.fnvm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;

import static org.github.fnvm.ContentType.*;

public class Scraper {
    private static final Logger log = LoggerFactory.getLogger(Scraper.class);

    public static ContentResult getContent(String link, boolean hd) throws UrlScrapingException {
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

        switch (getContentType(link)) {
            case VIDEO -> {
                List<String> content = getDirectVideoLink(link, hd);
                return new ContentResult(content, VIDEO);
            }
            case PHOTOS -> {
                List<String> content = getPhotoLinks(link);
                return new ContentResult(content, PHOTOS);
            }
            default -> {
                return new ContentResult(Collections.emptyList(), BLANK);
            }
        }

    }

    private static ContentType getContentType(String link) {
        if (link.contains("/photo/")) return PHOTOS;
        return VIDEO;
    }

    private static List<String> getDirectVideoLink(String link, boolean hd) throws UrlScrapingException {
        JsonNode data = getResponse(link, hd);
        String videoPath = "play";
        if (hd) {
            boolean hasHd = !data.path("hdplay").asText("").isEmpty();
            if (hasHd) videoPath = "hdplay";
        }
        String videoUrl = data.path(videoPath).asText("");

        if (videoUrl.isEmpty()) {
            log.warn("Video URL is empty for link: {}", link);
            return Collections.emptyList();
        }

        return List.of(videoUrl);
    }

    private static List<String> getPhotoLinks(String link) throws UrlScrapingException {
        JsonNode data = getResponse(link, true);
        JsonNode imagesNode = data.path("images");

        if (imagesNode.isMissingNode() || imagesNode.isNull()) {
            return Collections.emptyList();
        }

        List<String> photos = new ArrayList<>();

        if (imagesNode.isArray()) {
            imagesNode.forEach(node -> addUrlIfValid(photos, node.asText("")));
        } else {
            addUrlIfValid(photos, imagesNode.asText(""));
        }

        return photos;
    }

    private static void addUrlIfValid(List<String> photos, String url) {
        if (url != null && !url.isEmpty()) {
            photos.add(url);
        }
    }

    private static JsonNode getResponse(String link, boolean hd) throws UrlScrapingException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://tikwm.com/api/"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "url="
                                    + URLEncoder.encode(link, StandardCharsets.UTF_8)
                                    + (hd ? "&hd=1" : "")))
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
