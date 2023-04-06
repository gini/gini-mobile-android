package net.gini.android.capture.network;

import android.content.Context;

import androidx.annotation.NonNull;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.analysis.AnalysisFragmentListener;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLoggerListener;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.util.CancellationToken;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 29.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Interface specifying network related tasks required by the Gini Capture SDK in order to
 * communicate with the Gini API.
 *
 * <p> The easiest way to get started is to use the Gini Capture Network Library package which
 * provides a default implementation.
 *
 * <p> You can also create your own implementation and communicate directly with the Gini API or
 * pass requests through your backend. For direct communication with the Gini API we recommend using
 * the Gini Bank API lib.
 *
 * <p> In order for the Gini Capture SDK to use your implementation pass an instance of it to
 * {@link GiniCapture.Builder#setGiniCaptureNetworkService(GiniCaptureNetworkService)} when creating a
 * {@link GiniCapture} instance.
 *
 */
public interface GiniCaptureNetworkService extends ErrorLoggerListener {

    /**
     * Called when a document needs to be uploaded to the Gini API.
     *
     * <p> You should only upload the document. Polling or retrieving extractions is not needed at
     * this point.
     *
     * @param document a {@link Document} containing an image, pdf or other supported formats
     * @param callback a callback implementation to return the outcome of the upload
     *
     * @return a {@link CancellationToken} to be used for requesting upload cancellation
     */
    CancellationToken upload(@NonNull final Document document,
            @NonNull final GiniCaptureNetworkCallback<Result, Error> callback);

    /**
     * Called when a document needs to be deleted from the Gini API.
     *
     * @param giniApiDocumentId id of the document received when it was uploaded to the Gini API
     * @param callback          a callback implementation to return the outcome of the deletion
     *
     * @return a {@link CancellationToken} to be used for requesting cancellation of the deletion
     */
    CancellationToken delete(@NonNull final String giniApiDocumentId,
            @NonNull final GiniCaptureNetworkCallback<Result, Error> callback);

    /**
     * Called when a document needs to be analyzed by the Gini API.
     *
     * <p> The documents were already uploaded and only the Gini API document ids of documents are
     * passed in along with the user applied document rotations.
     *
     * @param giniApiDocumentIdRotationMap a map of Gini API document ids and the user applied
     *                                     document rotations
     * @param callback                     a callback implementation to return the outcome of the
     *                                     analysis
     *
     * @return a {@link CancellationToken} to be used for requesting analysis cancellation
     */
    CancellationToken analyze(
            @NonNull final LinkedHashMap<String, Integer> giniApiDocumentIdRotationMap, // NOPMD
            @NonNull final GiniCaptureNetworkCallback<AnalysisResult, Error> callback);



    /**
     * Call this method with the extractions the user has seen and accepted. The {@link
     * GiniCaptureSpecificExtraction}s must contain the final user corrected and/or accepted values.
     *
     * @param extractions a map of extraction labels and specific extractions
     * @param callback    a callback implementation to return the outcome
     */
    void sendFeedback(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                      @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                      @NonNull final GiniCaptureNetworkCallback<Void, Error> callback);

    /**
     * Delete the anonymous gini user credentials. These were automatically generated when the first document was uploaded.
     * <p>
     * By deleting the credentials, new ones will be generated at the next upload.
     */
    void deleteGiniUserCredentials();

    /**
     * Called when the Gini Capture SDK is not needed anymore and the {@link
     * GiniCapture#cleanup(Context, String, String, String, String, String)} method has been called.
     *
     * <p> Free up any resources your implementation is using.
     */
    void cleanup();

    @Override
    default void handleErrorLog(@NonNull ErrorLog errorLog) {
    }
}
