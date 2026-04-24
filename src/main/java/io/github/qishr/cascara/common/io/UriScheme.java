package io.github.qishr.cascara.common.io;

import java.net.URI;

public enum UriScheme {
    CASCARA, FILE, RES, HTTP, HTTPS, ZIP, JAR, UNKNOWN;

    public static UriScheme of(URI uri) {
        if (uri == null) {
            return UriScheme.UNKNOWN;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return UriScheme.FILE;
        } else if (scheme.equalsIgnoreCase("cascara")) {
            return UriScheme.CASCARA;
        } else if (scheme.equalsIgnoreCase("res")) {
            return UriScheme.RES;
        } else if (scheme.equalsIgnoreCase("file") || scheme.equalsIgnoreCase("C") || uri.toString().startsWith("/")) {
            return UriScheme.FILE;
        } else if (scheme.equalsIgnoreCase("http")) {
            return UriScheme.HTTP;
        } else if (scheme.equalsIgnoreCase("https")) {
            return UriScheme.HTTPS;
        } else if (scheme.equalsIgnoreCase("zip")) {
            return UriScheme.ZIP;
        } else if (scheme.equalsIgnoreCase("jar")) {
            return UriScheme.JAR;
        } else {
            return UriScheme.UNKNOWN;
        }
    }
}
