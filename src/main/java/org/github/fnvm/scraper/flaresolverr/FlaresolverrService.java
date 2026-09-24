package org.github.fnvm.scraper.flaresolverr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.github.fnvm.scraper.HttpClientService;
import org.github.fnvm.scraper.UrlScrapingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FlaresolverrService {
    private static final Logger log = LoggerFactory.getLogger(FlaresolverrService.class);

    private static final String FLARESOLVERR_ENDPOINT = "http://localhost:8191/v1";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode getResponse(ObjectNode request) throws UrlScrapingException {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest requestToFlaresolverr = HttpRequest.newBuilder()
                    .uri(URI.create(FLARESOLVERR_ENDPOINT))
                    .timeout(HttpClientService.REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HttpClientService.send(requestToFlaresolverr);
            String responseBody = response.body();

            if (responseBody.contains("<pre>") && responseBody.contains("</pre>")) {
                int start = responseBody.indexOf("<pre>") + 5;
                int end = responseBody.indexOf("</pre>");
                String jsonContent = responseBody.substring(start, end);
                jsonContent = jsonContent.replace("\\\"", "\"").replace("\\\\", "\\");

                return objectMapper.readTree(jsonContent);
            } else {
                return objectMapper.readTree(responseBody);
            }
        } catch (IOException e) {
            log.error("Error during Flaresolverr request", e);
            throw new UrlScrapingException("Error during Flaresolverr request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Flaresolverr request was interrupted", e);
            throw new UrlScrapingException("Flaresolverr request was interrupted", e);
        }
    }
}
