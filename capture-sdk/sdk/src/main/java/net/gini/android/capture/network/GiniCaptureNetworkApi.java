package net.gini.android.capture.network;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 22.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Interface specifying network calls which can be performed manually from outside the Gini Captrue
 * SDK (e.g. for sending feedback).
 *
 * * <p> In order to easily access your implementation pass an instance of it to {@link
 * GiniCapture.Builder#setGiniCaptureNetworkApi(GiniCaptureNetworkApi)} when creating a {@link
 * GiniCapture} instance. You can then get the instance in your app with {@link
 * GiniCapture#getGiniCaptureNetworkApi()}.
 */
public interface GiniCaptureNetworkApi {

    /**
     * Call this method with the extractions the user has seen and accepted. The {@link
     * GiniCaptureSpecificExtraction}s must contain the final user corrected and/or accepted values.
     *
     * @param extractions a map of extraction labels and specific extractions
     * @param callback    a callback implementation to return the outcome
     */
    void sendFeedback(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
            @NonNull final GiniCaptureNetworkCallback<Void, Error> callback);

    /**
     * Delete the anonymous gini user credentials. These were automatically generated when the first document was uploaded.
     * <p>
     * By deleting the credentials, new ones will be generated at the next upload.
     */
    void deleteGiniUserCredentials();

    /**
     * This method is called by the Gini Capture Library with the compound extractions (e.g., line items) the user has seen. Contains changes
     * made by the user.
     * <p>
     * <b>Note:</b> If the compound extractions are modified in your app, then call this method to have the latest changes available when
     * the feedback is sent.
     *
     * @param compoundExtractions the updated compound extractions
     */
    void setUpdatedCompoundExtractions(@NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions);
}
