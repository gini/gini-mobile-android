package net.gini.android.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class TestUtils {

    public static byte[] createByteArray(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    /**
     * Determines, if the uris are equal based on the implementation of {@link
     * URI#equals(Object)} with the difference, that the query strings are compared by the
     * key-value pairs they contain. The order of the key-value pairs is ignored.
     *
     * @param uriString1 an uri
     * @param uriString2 another uri
     * @return true, if the uris are equal, false otherwise
     * @throws URISyntaxException
     */
    public static boolean areEqualURIs(String uriString1, String uriString2)
            throws URISyntaxException {
        URI uri1 = new URI(uriString1);
        URI uri2 = new URI(uriString2);
        URICompare uriCompare = new URICompare(uri1);
        return uriCompare.isEqualToURI(uri2);
    }

    /**
     * Determines, if the two query strings are equal by exploding them into {@link Map}s and
     * comparing the maps. This ignores the order of the parameters.
     *
     * @param query1 a query string
     * @param query2 another query string
     * @return true, if the query strings are equal, false otherwise
     */
    public static boolean areEqualURIQueries(String query1, String query2) {
        URIQuery uriQuery1 = new URIQuery(query1);
        URIQuery uriQuery2 = new URIQuery(query2);
        return uriQuery1.equals(uriQuery2);
    }

    private TestUtils() {
    }
}
