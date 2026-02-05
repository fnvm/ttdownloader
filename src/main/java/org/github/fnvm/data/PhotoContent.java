package org.github.fnvm.data;

import java.util.List;

public record PhotoContent(List<String> urls) implements Content {

  @Override
  public ContentType getType() {
    return ContentType.PHOTO;
  }

  public boolean isEmpty() {
    return urls == null || urls.isEmpty();
  }

  public int count() {
    return urls != null ? urls.size() : 0;
  }
}
