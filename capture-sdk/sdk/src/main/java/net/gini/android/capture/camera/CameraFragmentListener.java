package net.gini.android.capture.camera;

import androidx.annotation.NonNull;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.QRCodeDocument;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.Map;

/**
 * Internal use only.
 *
 * <p>
 * Interface used by {@link CameraFragment} to dispatch events to the hosting Activity.
 * </p>
 *
 * @suppress
 */
interface CameraFragmentListener {
    /**
     * <p>
     * Called when the user has taken an image with the camera or has imported a document that passed the Gini Capture SDK's validation and any custom checks that were implemented.
     * </p>
     *
     * @param document the image taken by the camera or the validated imported document
     */
    void onDocumentAvailable(@NonNull Document document);


    /**
     * <p>
     * Called when starting MultiPageReview Screen
     * </p>
     *
     * @param multiPageDocument the image taken by the camera or the validated imported document
     * @param shouldScrollToLastPage flag which defines if pages should scroll to the last page
     */

    void onProceedToMultiPageReviewScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument, boolean shouldScrollToLastPage);

    /**
     * <p>
     *     This method is invoked for imported documents to allow custom validations.
     * </p>
     * <p>
     *     Invoke one of the {@link DocumentCheckResultCallback} methods on the main thread to inform the Gini Capture SDK about the result.
     * </p>
     * <p>
     *     <b>Note:</b> The Gini Capture SDK will wait until one of the {@link DocumentCheckResultCallback} methods are invoked.
     * </p>
     * @param document a {@link Document} created from the file the user picked
     * @param callback use this callback to inform the Gini Capture SDK about the result of the custom checks
     */
    void onCheckImportedDocument(@NonNull Document document,
            @NonNull DocumentCheckResultCallback callback);

    /**
     * <p>
     * Called when an error occurred.
     * </p>
     * @param error details about what went wrong
     */
    void onError(@NonNull GiniCaptureError error);

    /**
     * Called after a QRCode was successfully analyzed.
     *
     * @param extractions a map of the extractions with the extraction labels as keys
     */
    void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions);

    /**
     * <p>
     *     Called when the user clicked the QR Code detected popup.
     *     You should upload the {@link QRCodeDocument}'s data to the Gini API to get the extractions,
     *     close the Gini Capture SDK and continue to your app's transfer form.
     * </p>
     * <p>
     *      See {@link QRCodeDocument} for supported formats.
     * </p>
     *
     * @param qrCodeDocument contains payment data from a QR Code
     *
     * @Deprecated When a {@link GiniCapture} instance is available the QRCode
     * is analyzed internally by using the configured {@link GiniCaptureNetworkService}
     * implementation. The extractions will be returned in {@link CameraFragmentListener#onExtractionsAvailable(Map)}.
     */
    @Deprecated
    default void onQRCodeAvailable(@NonNull QRCodeDocument qrCodeDocument) {

    }

    /**
     * <p>
     *     Callback to inform the Gini Capture SDK about the outcome of the custom imported document checks.
     * </p>
     */
    interface DocumentCheckResultCallback {
        /**
         * <p>
         *     Call if the document was accepted and should be analysed.
         * </p>
         * <p>
         *     <b>Note:</b> Always call this method on the main thread.
         * </p>
         */
        void documentAccepted();

        /**
         * <p>
         *     Call if the document doesn't conform to your expectations and pass in a message to be shown to the user.
         * </p>
         * <p>
         *     <b>Note:</b> Always call this method on the main thread.
         * </p>
         *
         * @param messageForUser a message informing the user why the selected file was rejected
         */
        void documentRejected(@NonNull String messageForUser);
    }
}