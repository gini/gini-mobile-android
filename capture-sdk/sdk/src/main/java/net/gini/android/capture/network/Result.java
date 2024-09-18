package net.gini.android.capture.network;

/**
 * Created by Alpar Szotyori on 29.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Used by the {@link GiniCaptureNetworkService} to return network call results.
 */
public class Result {

    private final String giniApiDocumentId;
    private final String giniApiDocumentFilename;

    /**
     * Create a new result with a Gini API document id.
     *
     * @param giniApiDocumentId the id of a document in the Gini API
     */
    public Result(final String giniApiDocumentId, final String giniApiDocumentFilename) {
        this.giniApiDocumentId = giniApiDocumentId;
        this.giniApiDocumentFilename = giniApiDocumentFilename;
    }

    /**
     * @return document's id in the Gini API
     */
    public String getGiniApiDocumentId() {
        return giniApiDocumentId;
    }

    public String getGiniApiDocumentFilename() {
        return giniApiDocumentFilename;
    }
}
