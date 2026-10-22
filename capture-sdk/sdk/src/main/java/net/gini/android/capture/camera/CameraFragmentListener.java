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
public interface CameraFragmentListener {

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
     * <p>Known extraction keys the SDK may deliver here:
     * <ul>
     *   <li>{@code epsPaymentQRCodeUrl} &mdash; a locally-parsed EPS QR Code. The value is the raw
     *       EPS payload (also present as the single element in {@code candidates}).</li>
     *   <li>{@code weroPaymentQRCodeUrl} &mdash; a Wero candidate the backend classified as Wero.
     *       The value is the raw QR Code payload (also present as the single element in
     *       {@code candidates}). Only delivered when
     *       {@code isWeroQRCodeScanningEnabled} is on in the client configuration.</li>
     *   <li>The Pay5 keys ({@code iban}, {@code bic}, {@code amountToPay},
     *       {@code paymentRecipient}, {@code paymentReference}) for parseable payment QR codes.</li>
     * </ul>
     *
     * <p>A Wero candidate that the backend does <b>not</b> classify as Wero is <b>not</b>
     * delivered through this callback: the SDK keeps the user on the camera and shows the
     * unsupported-QR popup instead.
     *
     * @param extractions a map of the extractions with the extraction labels as keys
     */
    void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions);

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
