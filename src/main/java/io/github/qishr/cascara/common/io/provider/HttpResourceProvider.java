package io.github.qishr.cascara.common.io.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.github.qishr.cascara.common.io.ResourceStream;

public class HttpResourceProvider implements ResourceProvider {

    @Override
    public ResourceStream getResourceAsStream(URI uri) throws IOException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }

            String mime = response.headers()
                    .firstValue("Content-Type")
                    .orElse(null);

            return new ResourceStream(response.body(), mime);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Remote load interrupted", e);
        }
    }

}
