package net.gini.android.helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Explodes a query string into a {@link Map} which is then used to compare it to another query string.
 *
 * Query strings must be in the format of {@code "foo=bar&zool=tool"}.
 */
public class URIQuery {

    private final Map<String, String> queryParameters;

    public URIQuery(final String query) {
        this.queryParameters = parseQuery(query);
    }

    private Map<String, String> parseQuery(final String query) {
        final String trimmedQuery = query.replace("?", "").trim();
        final Map<String, String> parameters = new HashMap<>();
        final String[] keyValuePairs = trimmedQuery.split("&");
        for (final String keyValuePair : keyValuePairs) {
            final String[] keyAndValue = keyValuePair.split("=");
            if (keyAndValue.length == 2) {
                parameters.put(keyAndValue[0], keyAndValue[1]);
            } else {
                throw new IllegalArgumentException("Cannot parse query: " + query);
            }
        }
        return parameters;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final URIQuery uriQuery = (URIQuery) o;

        return queryParameters != null ? queryParameters.equals(uriQuery.queryParameters)
                : uriQuery.queryParameters == null;
    }

    @Override
    public int hashCode() {
        return queryParameters != null ? queryParameters.hashCode() : 0;
    }
}
