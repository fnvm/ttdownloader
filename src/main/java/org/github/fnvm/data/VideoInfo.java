package org.github.fnvm.data;

public class VideoInfo implements Content {
    String link;
    ContentType type = ContentType.VIDEO;

    public VideoInfo(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    @Override
    public ContentType getType() {
        return type;
    }
}
