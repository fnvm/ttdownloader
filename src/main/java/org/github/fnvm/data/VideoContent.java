package org.github.fnvm.data;

import java.util.Optional;

public record VideoContent(
    String url, long sizeBytes, String title, QualityPreference actualQuality) implements Content {

  private static final long MB = 1024L * 1024;
  private static final long TELEGRAM_MAX_SIZE = 50 * MB - 2 * MB;

  @Override
  public ContentType getType() {
    return ContentType.VIDEO;
  }

  public boolean exceedsTelegramLimit() {
    return sizeBytes > TELEGRAM_MAX_SIZE;
  }

  public double getSizeMB() {
    return sizeBytes / (1024.0 * 1024.0);
  }

  public Optional<String> getValidTitle() {
    return Optional.ofNullable(title).filter(t -> !t.isBlank()).map(String::trim);
  }

  public String getFormattedSize() {
    if (sizeBytes < 1024) {
      return sizeBytes + " B";
    } else if (sizeBytes < 1024 * 1024) {
      return String.format("%.2f KB", sizeBytes / 1024.0);
    } else {
      return String.format("%.2f MB", getSizeMB());
    }
  }
}
