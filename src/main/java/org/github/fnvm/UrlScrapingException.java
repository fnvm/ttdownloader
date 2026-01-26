package org.github.fnvm;

public class UrlScrapingException extends Exception {
    public UrlScrapingException(String message) {
        super(message);
    }

    public UrlScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
