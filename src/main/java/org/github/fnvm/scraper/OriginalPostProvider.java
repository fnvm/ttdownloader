package org.github.fnvm.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.github.fnvm.scraper.flaresolverr.FlaresolverrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OriginalPostProvider {
    private static final Logger log = LoggerFactory.getLogger(OriginalPostProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode getResponseFlaresolverr(String link, String cookie) throws UrlScrapingException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("cmd", "request.post");
        request.put("url", "https://www.tikwm.com/api/video/task/submit");

        ObjectNode headers = objectMapper.createObjectNode();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("x-proxy-cookie", "sessionid=" + cookie);

        request.set("headers", headers);
        request.put("postData", "url=" + link + "&web=1");

        JsonNode response = FlaresolverrService.getResponse(request);
        JsonNode taskIdNode = response.path("data").path("task_id");

        if (taskIdNode.isMissingNode()) throw new UrlScrapingException("");

        ObjectNode secondRequest = objectMapper.createObjectNode();
        secondRequest.put("cmd", "request.get");
        secondRequest.put("url", "https://www.tikwm.com/api/video/task/result?task_id=" + taskIdNode.asText());

        JsonNode secondResponse = FlaresolverrService.getResponse(secondRequest);

        JsonNode playUrlNode = secondResponse
                .path("data")
                .path("detail")
                .path("play_url");

        if (playUrlNode.isMissingNode()
                || playUrlNode.isNull()
                || playUrlNode.asText().isBlank()) {
            throw new UrlScrapingException("");
        }

        return secondResponse;
    }

    public static JsonNode getDirectResponse(String link, String cookie) throws UrlScrapingException, IOException, InterruptedException {
        var requestBody = "url=" + URLEncoder.encode(link, StandardCharsets.UTF_8) + "&web=1";
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("https://www.tikwm.com/api/video/task/submit"))
                .header("x-proxy-cookie", "sessionid=" + cookie)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        HttpRequest request = requestBuilder.build();
        HttpClient client = HttpClientService.getClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = objectMapper.readTree(response.body());

        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String message = root.path("msg").asText("Unknown error");
            log.error("API returned error code {}: {}", code, message);
            throw new UrlScrapingException("API error: " + message);
        }

        JsonNode dataNode = root.path("data").path("task_id");
        if (dataNode.isMissingNode()) {
            throw new UrlScrapingException("Response missing data field");
        }

        String taskId = root.path("data").path("task_id").asText("");
        var secondRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.tikwm.com/api/video/task/result?task_id=" + taskId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode secondRoot = objectMapper.readTree(response.body());

    }

}
