package net.gini.android.helpers;

import java.net.URI;

public class URICompare {

    private final URI subjectURI;

    public URICompare(final URI subjectURI) {
        this.subjectURI = subjectURI;
    }

    /**
     * Determines, if the uris are equal based on the implementation of {@link
     * URI#equals(Object)} with the difference, that the query strings are compared by the
     * key-value pairs they contain. The order of the key-value pairs is ignored.
     *
     * @param targetUri compare with this {@link URI}
     * @return true if equal, false otherwise
     */
    public boolean isEqualToURI(URI targetUri) {
        if (targetUri == null) {
            return false;
        }

        if (fragmentIsNotEqual(targetUri) || schemeIsNotEqual(targetUri)) {
            return false;
        }

        if (bothOpaque(targetUri)) {
            return compareOpaqueURIs(targetUri);
        } else {
            return bothNotOpaque(targetUri) && compareNonOpaqueURIs(targetUri);
        }
    }

    private boolean fragmentIsNotEqual(final URI targetUri) {
        return !areStringsEqual(subjectURI.getFragment(), targetUri.getFragment());
    }

    private boolean areStringsEqual(final String string1, final String string2) {
        return bothNull(string1, string2) ||
                (bothNotNull(string1, string2) && areEqualIgnoringCase(string1, string2));
    }

    private boolean bothNull(final Object object1, final Object object2) {
        return object1 == null && object2 == null;
    }

    private boolean bothNotNull(final Object object1, final Object object2) {
        return object1 != null && object2 != null;
    }

    private boolean areEqualIgnoringCase(final String string1, final String string2) {
        return string1.equalsIgnoreCase(string2);
    }

    private boolean schemeIsNotEqual(final URI targetUri) {
        return !areStringsEqual(subjectURI.getScheme(), targetUri.getScheme());
    }

    private boolean bothOpaque(final URI targetUri) {
        return subjectURI.isOpaque() && targetUri.isOpaque();
    }

    private boolean compareOpaqueURIs(final URI targetUri) {
        return areSchemeSpecificPartsEqual(targetUri);
    }

    private boolean areSchemeSpecificPartsEqual(final URI targetUri) {
        return subjectURI.getSchemeSpecificPart().equalsIgnoreCase(
                targetUri.getSchemeSpecificPart());
    }

    private boolean bothNotOpaque(final URI targetUri) {
        return !subjectURI.isOpaque() && !targetUri.isOpaque();
    }

    private boolean compareNonOpaqueURIs(final URI targetUri) {
        return pathIsEqual(targetUri) && queryIsEqual(targetUri) &&
                (bothAuthoritiesAreNull(targetUri) ||
                        (bothAuthoritiesAreNotNull(targetUri) &&
                                compareURIsWithAuthorities(targetUri)));
    }

    private boolean pathIsEqual(final URI targetUri) {
        return subjectURI.getPath().equalsIgnoreCase(targetUri.getPath());
    }

    private boolean queryIsEqual(final URI targetUri) {
        return bothNull(subjectURI.getQuery(), targetUri.getQuery()) ||
                (bothNotNull(subjectURI.getQuery(), targetUri.getQuery()) &&
                        compareQueries(subjectURI.getQuery(), targetUri.getQuery()));
    }

    private boolean compareQueries(final String query1, final String query2) {
        URIQuery uriQuery1 = new URIQuery(query1);
        URIQuery uriQuery2 = new URIQuery(query2);
        return uriQuery1.equals(uriQuery2);
    }

    private boolean bothAuthoritiesAreNull(final URI targetUri) {
        return bothNull(subjectURI.getAuthority(), targetUri.getAuthority());
    }

    private boolean bothAuthoritiesAreNotNull(final URI targetUri) {
        return bothNotNull(subjectURI.getAuthority(), targetUri.getAuthority());
    }

    private boolean compareURIsWithAuthorities(final URI targetUri) {
        return (bothHostsAreNull(targetUri) && areAuthoritiesEqual(targetUri)) ||
                (bothHostsAreNotNull(targetUri) && compareURIsWithHosts(targetUri));
    }

    private boolean bothHostsAreNull(final URI targetUri) {
        return bothNull(subjectURI.getHost(), targetUri.getHost());
    }

    private boolean areAuthoritiesEqual(final URI targetUri) {
        return subjectURI.getAuthority().equalsIgnoreCase(targetUri.getAuthority());
    }

    private boolean bothHostsAreNotNull(final URI targetUri) {
        return bothNotNull(subjectURI.getHost(), targetUri.getHost());
    }

    private boolean compareURIsWithHosts(final URI targetUri) {
        return hostIsEqual(targetUri) && portIsEqual(targetUri) && isUserInfoEqual(
                targetUri);
    }

    private boolean hostIsEqual(final URI targetUri) {
        return targetUri.getHost().equalsIgnoreCase(subjectURI.getHost());
    }

    private boolean portIsEqual(final URI targetUri) {
        return targetUri.getPort() == subjectURI.getPort();
    }

    private boolean isUserInfoEqual(final URI targetUri) {
        return areStringsEqual(subjectURI.getUserInfo(), targetUri.getUserInfo());
    }

}
