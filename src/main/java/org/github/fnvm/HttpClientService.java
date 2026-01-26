package org.github.fnvm;

import java.net.http.HttpClient;

public class HttpClientService {
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();


    public static HttpClient getClient() {
        return client;
    }

}
