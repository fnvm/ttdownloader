package org.github.fnvm;

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
}

