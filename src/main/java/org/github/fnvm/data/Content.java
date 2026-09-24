package org.github.fnvm.data;

public sealed interface Content permits EmptyContent, PhotoContent, VideoContent {

    ContentType getType();

    default boolean isEmpty() {
        return this instanceof EmptyContent;
    }

    default boolean isVideo() {
        return this instanceof VideoContent;
    }

    default boolean isPhoto() {
        return this instanceof PhotoContent;
    }
}
