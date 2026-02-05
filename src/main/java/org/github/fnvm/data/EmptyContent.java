package org.github.fnvm.data;

public record EmptyContent(String reason) implements Content {

  @Override
  public ContentType getType() {
    return ContentType.BLANK;
  }
}
