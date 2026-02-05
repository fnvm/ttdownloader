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
  private static final ObjectMapper mapper = new ObjectMapper();

  public static JsonNode getDirectResponse(String link, String cookie) throws UrlScrapingException {

    return fetchVideoData(
        link, cookie, OriginalPostProvider::directSubmit, OriginalPostProvider::directGet);
  }

  public static JsonNode getResponseFlaresolverr(String link, String cookie)
      throws UrlScrapingException {

    return fetchVideoData(
        link, cookie, OriginalPostProvider::flareSubmit, OriginalPostProvider::flareGet);
  }

  private interface SubmitFunc {
    JsonNode submit(String link, String cookie) throws Exception;
  }

  private interface GetFunc {
    JsonNode get(String taskId) throws Exception;
  }

  private static JsonNode fetchVideoData(
      String link, String cookie, SubmitFunc submitFunc, GetFunc getFunc)
      throws UrlScrapingException {

    try {
      JsonNode submitResponse = submitFunc.submit(link, cookie);
      String taskId = submitResponse.path("data").path("task_id").asText(null);

      if (taskId == null || taskId.isBlank()) {
        throw new UrlScrapingException("Missing task_id");
      }

      JsonNode result = getFunc.get(taskId);

      JsonNode playUrl = result.path("data").path("detail").path("play_url");
      if (playUrl.isMissingNode() || playUrl.asText().isBlank()) {
        throw new UrlScrapingException("Missing play_url");
      }

      return result;

    } catch (UrlScrapingException e) {
      throw e;
    } catch (Exception e) {
      throw new UrlScrapingException("Request failed: " + e.getMessage(), e);
    }
  }

  private static JsonNode directSubmit(String link, String cookie)
      throws IOException, InterruptedException, UrlScrapingException {

    String body = "url=" + URLEncoder.encode(link, StandardCharsets.UTF_8) + "&web=1";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("https://www.tikwm.com/api/video/task/submit"))
            .header("x-proxy-cookie", "sessionid=" + cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response =
        HttpClientService.getClient().send(request, HttpResponse.BodyHandlers.ofString());

    return validateResponse(response.body());
  }

  private static JsonNode directGet(String taskId)
      throws IOException, InterruptedException, UrlScrapingException {

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("https://www.tikwm.com/api/video/task/result?task_id=" + taskId))
            .GET()
            .build();

    HttpResponse<String> response =
        HttpClientService.getClient().send(request, HttpResponse.BodyHandlers.ofString());

    return validateResponse(response.body());
  }

  private static JsonNode flareSubmit(String link, String cookie) throws UrlScrapingException {
    ObjectNode req = mapper.createObjectNode();
    req.put("cmd", "request.post");
    req.put("url", "https://www.tikwm.com/api/video/task/submit");

    ObjectNode headers = mapper.createObjectNode();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("x-proxy-cookie", "sessionid=" + cookie);

    req.set("headers", headers);
    req.put("postData", "url=" + link + "&web=1");

    JsonNode response = FlaresolverrService.getResponse(req);
    return validateResponse(response);
  }

  private static JsonNode flareGet(String taskId) throws UrlScrapingException {

    ObjectNode req = mapper.createObjectNode();
    req.put("cmd", "request.get");
    req.put("url", "https://www.tikwm.com/api/video/task/result?task_id=" + taskId);

    JsonNode response = FlaresolverrService.getResponse(req);
    return validateResponse(response);
  }

  private static JsonNode validateResponse(String body) throws UrlScrapingException, IOException {
    return validateResponse(mapper.readTree(body));
  }

  private static JsonNode validateResponse(JsonNode root) throws UrlScrapingException {
    int code = root.path("code").asInt(-1);
    if (code != 0) {
      String msg = root.path("msg").asText("Unknown error");
      log.error("API returned error {}: {}", code, msg);
      throw new UrlScrapingException("API error: " + msg);
    }
    return root;
  }
}
