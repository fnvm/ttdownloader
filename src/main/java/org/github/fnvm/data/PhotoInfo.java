package org.github.fnvm.data;

import java.util.List;

public class PhotoInfo implements Content {
    List<String> links;
    ContentType type = ContentType.PHOTO;

    public PhotoInfo(List<String> links) {
        this.links = links;
    }

    public List<String> getLinks() {
        return links;
    }

    @Override
    public ContentType getType() {
        return type;
    }
}
