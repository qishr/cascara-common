package io.github.qishr.cascara.common.util;

import java.net.URI;

public enum UriScheme {
    CASCARA, FILE, RES, HTTP, HTTPS, UNKNOWN;

    public static UriScheme of(URI uri) {
        if (uri == null) {
            return UriScheme.UNKNOWN;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return UriScheme.FILE;
        } else if (scheme.equals("cascara")) {
            return UriScheme.CASCARA;
        } else if (scheme.equals("res")) {
            return UriScheme.RES;
        } else if (scheme.equals("file") || scheme.equals("C") || uri.toString().startsWith("/")) {
            return UriScheme.FILE;
        } else if (scheme.equals("http")) {
            return UriScheme.HTTP;
        } else if (scheme.equals("https")) {
            return UriScheme.HTTPS;
        } else {
            return UriScheme.UNKNOWN;
        }
    }
}
