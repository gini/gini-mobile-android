package net.gini.android.capture.screen;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDexApplication;

import net.gini.android.bank.api.GiniBankAPI;
import net.gini.android.bank.api.GiniBankAPIBuilder;
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.core.api.DocumentMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     Facilitates the application wide usage of the Gini Bank API Library's {@link GiniBankAPI} instance and a helper
 *     {@link SingleDocumentAnalyzer} instance.
 * </p>
 * <p>
 *     The {@link SingleDocumentAnalyzer} is used to analyze documents.
 *     It aids in starting the analysis of the document when the Review Screen starts and continuing it when the
 *     Analysis Screen was shown, if the document wasn't modified. In case it is modified the analysis is cancelled and
 *     only started when the Analysis Screen was shown where the reviewed final document is available.
 * </p>
 */
public abstract class BaseExampleApp extends MultiDexApplication {

    private static final Logger LOG = LoggerFactory.getLogger(BaseExampleApp.class);

    private GiniBankAPI mGiniApi;
    private SingleDocumentAnalyzer mSingleDocumentAnalyzer;
    private GiniCaptureNetworkService mGiniCaptureNetworkService;

    public SingleDocumentAnalyzer getSingleDocumentAnalyzer() {
        if (mSingleDocumentAnalyzer == null) {
            mSingleDocumentAnalyzer = new SingleDocumentAnalyzer(getGiniApi());
        }
        return mSingleDocumentAnalyzer;
    }

    protected abstract String getClientId();

    protected abstract String getClientSecret();

    public GiniBankAPI getGiniApi() {
        if (mGiniApi == null) {
            createGiniApi();
        }
        return mGiniApi;
    }

    private void createGiniApi() {
        final String clientId = getClientId();
        final String clientSecret = getClientSecret();
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
            LOG.warn(
                    "Missing Gini API client credentials. Either create a local.properties file "
                            + "with clientId and clientSecret properties or pass them in as gradle "
                            + "parameters with -PclientId and -PclientSecret.");
        }
        final GiniBankAPIBuilder builder = new GiniBankAPIBuilder(this,
                clientId,
                clientSecret,
                "example.com");
        mGiniApi = builder.build();
    }

    @NonNull
    public GiniCaptureNetworkService getGiniCaptureNetworkService(@NonNull final String appFlowApiType) {
        final String clientId = getClientId();
        final String clientSecret = getClientSecret();
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
            LOG.warn(
                    "Missing Gini API client credentials. Either create a local.properties file "
                            + "with clientId and clientSecret properties or pass them in as gradle "
                            + "parameters with -PclientId and -PclientSecret.");
        }
        if (mGiniCaptureNetworkService == null) {
            final DocumentMetadata documentMetadata = new DocumentMetadata();
            documentMetadata.setBranchId("GCSExampleAndroid");
            documentMetadata.add("AppFlow", appFlowApiType);
            mGiniCaptureNetworkService = GiniCaptureDefaultNetworkService.builder(this)
                    .setClientCredentials(clientId, clientSecret, "example.com")
                    .setDocumentMetadata(documentMetadata)
                        .build();
        }
        return mGiniCaptureNetworkService;
    }

    public void clearGiniCaptureNetworkInstances() {
        if (mGiniCaptureNetworkService != null) {
            mGiniCaptureNetworkService.cleanup();
            mGiniCaptureNetworkService = null;
        }
    }
}