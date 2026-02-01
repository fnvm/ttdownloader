package org.github.fnvm.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.github.fnvm.scraper.flaresolverr.FlaresolverrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginalPostProvider {
    private static final Logger log = LoggerFactory.getLogger(OriginalPostProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode getResponse(String link, String cookie) throws UrlScrapingException {
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

}
