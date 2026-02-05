package org.github.fnvm.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import org.github.fnvm.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ContentService {
  private static final Logger log = LoggerFactory.getLogger(ContentService.class);

  public static Content getContent(String link, QualityPreference quality, String cookies)
      throws UrlScrapingException {
    try {
      link = (UrlResolver.isShortLink(link)) ? UrlResolver.resolveShortUrl(link) : link;
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

    if (quality == QualityPreference.FULLHD) return handleFullHd(link, cookies);

    JsonNode response = DirectPostProvider.getResponse(link, quality);

    boolean isPhoto = response.hasNonNull("images") && !response.path("images").isEmpty();
    if (isPhoto) {
      return getPhotoInfo(response);
    } else {
      return getVideoInfo(response);
    }
  }

  private static Content getVideoInfo(JsonNode data) throws UrlScrapingException {
    boolean hasHd = data.hasNonNull("hdplay") && !data.path("hdplay").asText().isEmpty();

    String videoPath = hasHd ? "hdplay" : "play";
    String sizePath = hasHd ? "hd_size" : "size";

    String videoUrl = data.path(videoPath).asText("");
    if (videoUrl.isEmpty()) {
      throw new UrlScrapingException("Video URL is missing");
    }

    long sizeBytes = data.path(sizePath).asLong(0);
    String title = data.path("title").asText("");

    QualityPreference actualQuality = hasHd ? QualityPreference.HD : QualityPreference.SD;

    return new VideoContent(videoUrl, sizeBytes, title, actualQuality);
  }

  private static Content handleFullHd(String link, String cookie) throws UrlScrapingException {

    JsonNode response = null;
    boolean success = false;
    try {
      response = OriginalPostProvider.getDirectResponse(link, cookie);
      success = true;
    } catch (UrlScrapingException e) {
      log.warn("Error during direct original post request: {}", e.getMessage());
    }

    if (!success) {
      response = OriginalPostProvider.getResponseFlaresolverr(link, cookie);
    }

    JsonNode info = response.path("data").path("detail");

    String title = info.path("title").asText("");
    long sizeBytes = info.path("size").asLong(0);
    String url = info.path("play_url").asText("");

    return new VideoContent(url, sizeBytes, title, QualityPreference.FULLHD);
  }

  private static Content getPhotoInfo(JsonNode data) {
    JsonNode imagesNode = data.path("images");
    List<String> photos = new ArrayList<>();

    if (imagesNode.isArray()) {
      imagesNode.forEach(
          node -> {
            String url = node.asText("");
            addUrlIfValid(photos, url);
          });
    } else if (imagesNode.isTextual()) {
      addUrlIfValid(photos, imagesNode.asText(""));
    }

    return new PhotoContent(photos);
  }

  private static void addUrlIfValid(List<String> photos, String url) {
    if (url != null && !url.isEmpty()) {
      photos.add(url);
      log.trace("Added photo URL: {}", url);
    }
  }
}
