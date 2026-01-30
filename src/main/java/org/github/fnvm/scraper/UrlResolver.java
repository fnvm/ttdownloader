package org.github.fnvm.scraper;

import org.github.fnvm.data.ContentType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UrlResolver {
    public static String resolveShortUrl(String shortLink) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(shortLink))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpClient client = HttpClientService.getClient();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.uri().toString();
    }

    public static boolean isShortLink(String link) {
        return link.startsWith("https://vt");
    }

    /**
     * Heuristically infers the content type from the link.
     * The result is not guaranteed to be accurate and may be incorrect,
     * particularly for story content.
     */
    public static ContentType getContentType(String link) throws UrlScrapingException {
        if (link == null || link.isBlank()) {
            throw new UrlScrapingException("Invalid URL");
        }

        String cleanLink = link.trim();

        int queryIndex = cleanLink.indexOf('?');
        if (queryIndex != -1) {
            cleanLink = cleanLink.substring(0, queryIndex);
        }

        if (cleanLink.contains("/photo/")) {
            return ContentType.PHOTO;
        }

        // 1) https://www.tiktok.com/@username
        // 2) @username
        // 3) username
        if (isProfileLink(cleanLink)) {
            return ContentType.PROFILE;
        }

        return ContentType.VIDEO;
    }

    private static boolean isProfileLink(String value) {
        if (value.matches("^https?://(www\\.)?tiktok\\.com/@[^/]+$")) {
            return true;
        }

        if (value.matches("^@[^/]+$")) {
            return true;
        }

        return value.matches("^[a-zA-Z0-9._]+$");
    }
}

