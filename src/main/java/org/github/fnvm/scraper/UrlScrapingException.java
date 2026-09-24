package org.github.fnvm.scraper;

public class UrlScrapingException extends Exception {
    public UrlScrapingException(String message) {
        super(message);
    }

    public UrlScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
