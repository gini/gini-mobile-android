package net.gini.android.capture;

import android.content.Context;
import android.content.Intent;

import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.help.HelpItem;
import net.gini.android.capture.internal.cache.DocumentDataMemoryCache;
import net.gini.android.capture.internal.cache.PhotoMemoryCache;
import net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.logging.ErrorLoggerListener;
import net.gini.android.capture.network.GiniCaptureNetworkApi;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.onboarding.OnboardingPage;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.review.multipage.MultiPageReviewFragment;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.CameraScreenEvent;
import net.gini.android.capture.tracking.Event;
import net.gini.android.capture.tracking.EventTracker;
import net.gini.android.capture.tracking.OnboardingScreenEvent;
import net.gini.android.capture.tracking.ReviewScreenEvent;
import net.gini.android.capture.util.CancellationToken;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Created by Alpar Szotyori on 22.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Single entry point for the Gini Capture SDK for configuration and interaction.
 *
 * <p> This singleton class is preferred over the previous methods of configuration and interaction.
 * It is only mandatory for new features. You can continue using features from previous releases
 * without any modification.
 *
 * <p> To create and configure a singleton instance use the {@link #newInstance()} method and the
 * returned {@link Builder}. If an instance is already available you need to call {@link
 * #cleanup(Context)} before creating a new instance. Failing to do so will throw an exception.
 *
 * <p> After you are done using the Gini Capture SDK use the {@link #cleanup(Context)} method.
 * This will free up resources used by the library.
 */
public class GiniCapture {

    private static final Logger LOG = LoggerFactory.getLogger(GiniCapture.class);
    private static GiniCapture sInstance;
    private final GiniCaptureNetworkService mGiniCaptureNetworkService;
    private final GiniCaptureNetworkApi mGiniCaptureNetworkApi;
    private final NetworkRequestsManager mNetworkRequestsManager;
    private final DocumentDataMemoryCache mDocumentDataMemoryCache;
    private final PhotoMemoryCache mPhotoMemoryCache;
    private final ImageDiskStore mImageDiskStore;
    private final ImageMultiPageDocumentMemoryStore mImageMultiPageDocumentMemoryStore;
    private final GiniCaptureFileImport mGiniCaptureFileImport;
    private final Internal mInternal;
    private final DocumentImportEnabledFileTypes mDocumentImportEnabledFileTypes;
    private final boolean mFileImportEnabled;
    private final boolean mQRCodeScanningEnabled;
    private final ArrayList<OnboardingPage> mCustomOnboardingPages; // NOPMD - Bundle req. ArrayList
    private final boolean mShouldShowOnboardingAtFirstRun;
    private final boolean mMultiPageEnabled;
    private boolean mShouldShowOnboarding;
    private final boolean mIsSupportedFormatsHelpScreenEnabled;
    private final boolean mFlashButtonEnabled;
    private final boolean mBackButtonsEnabled;
    private final boolean mIsFlashOnByDefault;
    private final EventTracker mEventTracker;
    private final List<HelpItem.Custom> mCustomHelpItems;
    private final ErrorLogger mErrorLogger;

    /**
     * Retrieve the current instance.
     *
     * @return {@link GiniCapture} instance
     *
     * @throws IllegalStateException when there is no instance
     */
    @NonNull
    public static synchronized GiniCapture getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Not instantiated.");
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void setInstance(@Nullable final GiniCapture giniCapture) {
        sInstance = giniCapture;
    }

    /**
     * Check whether an instance exists.
     *
     * @return {@code true} if there is an instance
     */
    public static synchronized boolean hasInstance() {
        return sInstance != null;
    }

    /**
     * Configure and create a new instance using the returned {@link Builder}.
     *
     * @return a new {@link Builder}
     *
     * @throws IllegalStateException when an instance already exists. Call {@link #cleanup(Context)}
     *                               before trying to create a new instance
     */
    @NonNull
    public static synchronized Builder newInstance() {
        if (sInstance != null) {
            throw new IllegalStateException("An instance was already created. "
                    + "Call GiniCapture.cleanup() before creating a new instance.");
        }
        return new Builder();
    }

    /**
     * Destroys the {@link GiniCapture} instance and frees up used resources.
     *
     * @param context Android context
     */
    public static synchronized void cleanup(@NonNull final Context context) {
        if (sInstance != null) {
            sInstance.mDocumentDataMemoryCache.clear();
            sInstance.mPhotoMemoryCache.clear();
            if (sInstance.mNetworkRequestsManager != null) {
                sInstance.mNetworkRequestsManager.cleanup();
            }
            if (sInstance.mGiniCaptureNetworkApi != null) {
                sInstance.mGiniCaptureNetworkApi.setUpdatedCompoundExtractions(Collections.<String, GiniCaptureCompoundExtraction>emptyMap());
            }
            sInstance.mImageMultiPageDocumentMemoryStore.clear();
            sInstance.internal().setReviewScreenAnalysisError(null);
            sInstance = null; // NOPMD
        }
        ImageDiskStore.clear(context);
    }

    private static synchronized void createInstance(@NonNull final Builder builder) {
        sInstance = new GiniCapture(builder);
    }

    private GiniCapture(@NonNull final Builder builder) {
        mGiniCaptureNetworkService = builder.getGiniCaptureNetworkService();
        mGiniCaptureNetworkApi = builder.getGiniCaptureNetworkApi();
        mDocumentImportEnabledFileTypes = builder.getDocumentImportEnabledFileTypes();
        mFileImportEnabled = builder.isFileImportEnabled();
        mQRCodeScanningEnabled = builder.isQRCodeScanningEnabled();
        mCustomOnboardingPages = builder.getOnboardingPages();
        mShouldShowOnboardingAtFirstRun = builder.shouldShowOnboardingAtFirstRun();
        mShouldShowOnboarding = builder.shouldShowOnboarding();
        mDocumentDataMemoryCache = new DocumentDataMemoryCache();
        mPhotoMemoryCache = new PhotoMemoryCache(mDocumentDataMemoryCache);
        mImageDiskStore = new ImageDiskStore();
        mNetworkRequestsManager = mGiniCaptureNetworkService != null ? new NetworkRequestsManager(
                mGiniCaptureNetworkService, mDocumentDataMemoryCache) : null;
        mImageMultiPageDocumentMemoryStore = new ImageMultiPageDocumentMemoryStore();
        mGiniCaptureFileImport = new GiniCaptureFileImport(this);
        mInternal = new Internal(this);
        mMultiPageEnabled = builder.isMultiPageEnabled();
        mIsSupportedFormatsHelpScreenEnabled = builder.isSupportedFormatsHelpScreenEnabled();
        mFlashButtonEnabled = builder.isFlashButtonEnabled();
        mBackButtonsEnabled = builder.areBackButtonsEnabled();
        mIsFlashOnByDefault = builder.isFlashOnByDefault();
        mEventTracker = builder.getEventTracker();
        mCustomHelpItems = builder.getCustomHelpItems();
        mErrorLogger = new ErrorLogger(builder.getGiniErrorLoggerIsOn(),
                builder.getGiniCaptureNetworkService(),
                builder.getCustomErrorLoggerListener());
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @NonNull
    public Internal internal() {
        return mInternal;
    }

    /**
     * Retrieve the {@link GiniCaptureNetworkApi} instance, if available.
     *
     * @return {@link GiniCaptureNetworkApi} instance or {@code null}
     */
    @Nullable
    public GiniCaptureNetworkApi getGiniCaptureNetworkApi() {
        return mGiniCaptureNetworkApi;
    }

    /**
     * Retrieve the file types enabled for document import.
     *
     * <p> Disabled by default.
     *
     * @return enabled file types
     */
    @NonNull
    public DocumentImportEnabledFileTypes getDocumentImportEnabledFileTypes() {
        return mDocumentImportEnabledFileTypes;
    }

    /**
     * Find out whether file import has been enabled.
     *
     * <p> Disabled by default.
     *
     * @return {@code true} if file import was enabled
     */
    public boolean isFileImportEnabled() {
        return mFileImportEnabled;
    }

    /**
     * Find out whether QRCode scanning has been enabled.
     *
     * <p> Disabled by default.
     *
     * @return {@code true} if QRCode scanning was enabled
     */
    public boolean isQRCodeScanningEnabled() {
        return mQRCodeScanningEnabled;
    }

    /**
     * Find out whether scanning multi-page documents has been enabled.
     *
     * <p> Disabled by default
     *
     * @return {@code true} if multi-page is enabled
     */
    public boolean isMultiPageEnabled() {
        return mMultiPageEnabled;
    }

    /**
     * The custom Onboarding Screen pages, if configured.
     *
     * @return list of {@link OnboardingPage}s
     */
    @Nullable
    public ArrayList<OnboardingPage> getCustomOnboardingPages() { // NOPMD - ArrayList required (Bundle)
        return mCustomOnboardingPages;
    }

    /**
     * Screen API only
     *
     * <p> If set to {@code false}, the Onboarding Screen won't be shown on the first run.
     *
     * @return whether to show the Onboarding Screen or not
     */
    public boolean shouldShowOnboardingAtFirstRun() {
        return mShouldShowOnboardingAtFirstRun;
    }

    /**
     * Screen API only
     *
     * <p> If set to {@code true}, the Onboarding Screen will be shown every every time the
     * CameraActivity starts.
     *
     * <p> Default value is {@code false}.
     *
     * <p> You can change it on the existing GiniCapture instance with {@link
     * GiniCapture#setShouldShowOnboarding(boolean)}.
     *
     * @return whether to show the Onboarding Screen or not
     */
    public boolean shouldShowOnboarding() {
        return mShouldShowOnboarding;
    }

    /**
     * Screen API only
     *
     * <p> Set to {@code true} to show the Onboarding Screen every time the CameraActivity starts.
     *
     * <p> Default value is {@code false}.
     *
     * @param shouldShowOnboarding whether to show the onboarding on every launch
     */
    public void setShouldShowOnboarding(final boolean shouldShowOnboarding) {
        mShouldShowOnboarding = shouldShowOnboarding;
    }

    /**
     * Find out whether the Supported Formats help screen has been enabled.
     *
     * <p> Enabled by default.
     *
     * @return {@code true} if the Supported Formats help screen was enabled
     */
    public boolean isSupportedFormatsHelpScreenEnabled() {
        return mIsSupportedFormatsHelpScreenEnabled;
    }

    /**
     * Find out whether the flash button on the Camera Screen has been enabled.
     *
     * <p> Disabled by default.
     *
     * @return {@code true} if the flash button was enabled
     */
    public boolean isFlashButtonEnabled() {
        return mFlashButtonEnabled;
    }

    /**
     * Screen API only
     *
     * <p> Find out whether back buttons in all Activities have been enabled.
     * {@link ReviewActivity} and {@link AnalysisActivity} are not affected and always show back
     * buttons.
     *
     * <p> Enabled by default.
     *
     * @return {@code true} if the back buttons were enabled
     */
    public boolean areBackButtonsEnabled() {
        return mBackButtonsEnabled;
    }

    /**
     * Find out whether the camera flash is on or off by default.
     *
     * <p> If not changed, then flash is on by default.
     *
     * @return {@code true} if the flash is on by default
     */
    public boolean isFlashOnByDefault() {
        return mIsFlashOnByDefault;
    }

    /**
     * Screen API
     *
     * <p> If you have enabled the multi-page feature and your application receives one or multiple
     * files from another application you can use this method to create an Intent for launching the
     * Gini Capture SDK.
     *
     * <p> Importing the files is executed on a secondary thread as it can take several seconds for
     * the process to complete. The callback methods are invoked on the main thread.
     *
     * <p> In your callback's {@code onSuccess(Intent)} method start the Intent with {@link
     * android.app.Activity#startActivityForResult(Intent, int)} to receive the extractions or a
     * {@link GiniCaptureError} in case there was an error.
     *
     * @param intent   the Intent your app received
     * @param context  Android context
     * @param callback A {@link AsyncCallback} implementation
     *
     * @return a {@link CancellationToken} for cancelling the import process
     */
    @NonNull
    public CancellationToken createIntentForImportedFiles(@NonNull final Intent intent,
            @NonNull final Context context,
            @NonNull final AsyncCallback<Intent, ImportedFileValidationException> callback) {
        return mGiniCaptureFileImport.createIntentForImportedFiles(intent, context, callback);
    }

    /**
     * Component API
     *
     * <p> If you have enabled the multi-page feature and your application receives one or multiple
     * files from another application you can use this method to create a Document for launching the
     * Gini Capture SDK's {@link MultiPageReviewFragment} or the Analysis Fragment.
     *
     * <p> Importing the files is executed on a secondary thread as it can take several seconds for
     * the process to complete. The callback methods are invoked on the main thread.
     *
     * <p> If the Document can be reviewed ({@link Document#isReviewable()}) launch the {@link
     * MultiPageReviewFragment}.
     *
     * <p> If the Document cannot be reviewed you must launch the Analysis Fragment ({@link
     * net.gini.android.capture.analysis.AnalysisFragmentCompat}).
     *
     * @param intent   the Intent your app received
     * @param context  Android context
     * @param callback A {@link AsyncCallback} implementation
     *
     * @return a {@link CancellationToken} for cancelling the import process
     */
    @NonNull
    public CancellationToken createDocumentForImportedFiles(@NonNull final Intent intent,
            @NonNull final Context context,
            @NonNull final AsyncCallback<Document, ImportedFileValidationException> callback) {
        return mGiniCaptureFileImport.createDocumentForImportedFiles(intent, context, callback);
    }

    /**
     * Screen API
     *
     * <p> When your application receives a file from another application you can use this method to
     * create an Intent for launching the Gini Capture SDK.
     *
     * <p> Start the Intent with {@link android.app.Activity#startActivityForResult(Intent, int)} to
     * receive the extractions or a {@link GiniCaptureError} in case there was an error.
     *
     * @param intent                the Intent your app received
     * @param context               Android context
     * @param reviewActivityClass   (optional) the class of your application's {@link
     *                              ReviewActivity} subclass
     * @param analysisActivityClass (optional) the class of your application's {@link
     *                              AnalysisActivity} subclass
     *
     * @return an Intent for launching the Gini Capture SDK
     *
     * @throws ImportedFileValidationException if the file didn't pass validation
     * @throws IllegalArgumentException        if the Intent's data is not valid or the mime type is
     *                                         not supported
     **/
    @NonNull
    public static Intent createIntentForImportedFile(@NonNull final Intent intent,
            @NonNull final Context context,
            @Nullable final Class<? extends ReviewActivity> reviewActivityClass,
            @Nullable final Class<? extends AnalysisActivity> analysisActivityClass)
            throws ImportedFileValidationException {
        return GiniCaptureFileImport.createIntentForImportedFile(intent, context,
                reviewActivityClass, analysisActivityClass);
    }

    /**
     * Component API
     *
     * <p> When your application receives a file from another application you can use this method to
     * create a Document for launching the Gini Capture SDK's Review Fragment or Analysis
     * Fragment.
     *
     * <p> If the Document can be reviewed ({@link Document#isReviewable()}) launch the
     * Review Fragment ({@link net.gini.android.capture.review.ReviewFragmentCompat}).
     *
     * <p> If the Document cannot be reviewed you must launch the Analysis Fragment ({@link
     * net.gini.android.capture.analysis.AnalysisFragmentCompat}).
     *
     * @param intent  the Intent your app received
     * @param context Android context
     *
     * @return a Document for launching the Gini Capture SDK's Review Fragment or
     * Analysis Fragment
     *
     * @throws ImportedFileValidationException if the file didn't pass validation
     */
    @NonNull
    public static Document createDocumentForImportedFile(@NonNull final Intent intent,
            @NonNull final Context context) throws ImportedFileValidationException {
        return GiniCaptureFileImport.createDocumentForImportedFile(intent, context);
    }

    /**
     * The custom help items, if configured.
     *
     * @return list of {@link HelpItem.Custom} objects
     */
    @NonNull
    public List<HelpItem.Custom> getCustomHelpItems() {
        return mCustomHelpItems;
    }

    @NonNull
    ImageMultiPageDocumentMemoryStore getImageMultiPageDocumentMemoryStore() {
        return mImageMultiPageDocumentMemoryStore;
    }

    @Nullable
    GiniCaptureNetworkService getGiniCaptureNetworkService() {
        return mGiniCaptureNetworkService;
    }

    @Nullable
    NetworkRequestsManager getNetworkRequestsManager() {
        return mNetworkRequestsManager;
    }

    @NonNull
    DocumentDataMemoryCache getDocumentDataMemoryCache() {
        return mDocumentDataMemoryCache;
    }

    @NonNull
    PhotoMemoryCache getPhotoMemoryCache() {
        return mPhotoMemoryCache;
    }

    @NonNull
    ImageDiskStore getImageDiskStore() {
        return mImageDiskStore;
    }

    @NonNull
    EventTracker getEventTracker() {
        return mEventTracker;
    }

    @NonNull
    ErrorLogger getErrorLogger() { return mErrorLogger; }

    /**
     * Builder for {@link GiniCapture}. To get an instance call {@link #newInstance()}.
     */
    public static class Builder {

        private GiniCaptureNetworkService mGiniCaptureNetworkService;
        private GiniCaptureNetworkApi mGiniCaptureNetworkApi;
        private DocumentImportEnabledFileTypes mDocumentImportEnabledFileTypes =
                DocumentImportEnabledFileTypes.NONE;
        private boolean mFileImportEnabled;
        private boolean mQRCodeScanningEnabled;
        private ArrayList<OnboardingPage> mOnboardingPages; // NOPMD - ArrayList required (Bundle)
        private boolean mShouldShowOnboardingAtFirstRun = true;
        private boolean mShouldShowOnboarding;
        private boolean mMultiPageEnabled;
        private boolean mIsSupportedFormatsHelpScreenEnabled = true;
        private boolean mFlashButtonEnabled;
        private boolean mBackButtonsEnabled = true;
        private boolean mIsFlashOnByDefault = true;
        private EventTracker mEventTracker = new EventTracker() {
            @Override
            public void onOnboardingScreenEvent(@NotNull final Event<OnboardingScreenEvent> event) {
            }

            @Override
            public void onCameraScreenEvent(@NotNull final Event<CameraScreenEvent> event) {
            }

            @Override
            public void onReviewScreenEvent(@NotNull final Event<ReviewScreenEvent> event) {
            }

            @Override
            public void onAnalysisScreenEvent(@NotNull final Event<AnalysisScreenEvent> event) {
            }
        };
        private List<HelpItem.Custom> mCustomHelpItems = new ArrayList<>();
        private boolean mGiniErrorLoggerIsOn = true;
        private ErrorLoggerListener mCustomErrorLoggerListener;

        /**
         * Create a new {@link GiniCapture} instance.
         */
        public void build() {
            checkNetworkingImplementations();
            createInstance(this);
        }

        private void checkNetworkingImplementations() {
            if (mGiniCaptureNetworkService == null) {
                LOG.warn("GiniCaptureNetworkService instance not set. "
                        + "Relying on client to perform network calls."
                        + "You may provide a GiniCaptureNetworkService instance with "
                        + "GiniCapture.newInstance().setGiniCaptureNetworkService()");
            }
            if (mGiniCaptureNetworkApi == null) {
                LOG.warn("GiniCaptureNetworkApi instance not set. "
                        + "Relying on client to perform network calls."
                        + "You may provide a GiniCaptureNetworkApi instance with "
                        + "GiniCapture.newInstance().setGiniCaptureNetworkApi()");
            }
        }

        /**
         * Screen API only
         *
         * <p> Set to {@code false} to disable automatically showing the OnboardingActivity the
         * first time the CameraActivity is launched - we highly recommend letting the Gini Capture
         * SDK show the OnboardingActivity at first run.
         *
         * <p> Default value is {@code true}.
         *
         * @param shouldShowOnboardingAtFirstRun whether to show the onboarding on first run or not
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setShouldShowOnboardingAtFirstRun(
                final boolean shouldShowOnboardingAtFirstRun) {
            mShouldShowOnboardingAtFirstRun = shouldShowOnboardingAtFirstRun;
            return this;
        }

        /**
         * Set custom pages to be shown in the Onboarding Screen.
         *
         * @param onboardingPages an {@link ArrayList} of {@link OnboardingPage}s
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setCustomOnboardingPages(
                @NonNull final ArrayList<OnboardingPage> onboardingPages) { // NOPMD - ArrayList required (Bundle)
            mOnboardingPages = onboardingPages;
            return this;
        }

        /**
         * Screen API only
         *
         * <p> Set to {@code true} to show the Onboarding Screen every time the CameraActivity
         * starts.
         *
         * <p> Default value is {@code false}.
         *
         * @param shouldShowOnboarding whether to show the onboarding on every launch
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setShouldShowOnboarding(final boolean shouldShowOnboarding) {
            mShouldShowOnboarding = shouldShowOnboarding;
            return this;
        }

        boolean isMultiPageEnabled() {
            return mMultiPageEnabled;
        }

        /**
         * Enable/disable the multi-page feature.
         *
         * <p> Disabled by default.
         *
         * @param multiPageEnabled {@code true} to enable multi-page
         *
         * @return the {@link Builder} instance
         */
        public Builder setMultiPageEnabled(final boolean multiPageEnabled) {
            mMultiPageEnabled = multiPageEnabled;
            return this;
        }

        boolean shouldShowOnboardingAtFirstRun() {
            return mShouldShowOnboardingAtFirstRun;
        }

        @Nullable
        ArrayList<OnboardingPage> getOnboardingPages() { // NOPMD - ArrayList required (Bundle)
            return mOnboardingPages;
        }

        @Nullable
        GiniCaptureNetworkService getGiniCaptureNetworkService() {
            return mGiniCaptureNetworkService;
        }

        /**
         * Set the {@link GiniCaptureNetworkService} instance which will be used by the library to
         * request document related network calls (e.g. upload, analysis or deletion).
         *
         * @param giniCaptureNetworkService a {@link GiniCaptureNetworkService} instance
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setGiniCaptureNetworkService(
                @NonNull final GiniCaptureNetworkService giniCaptureNetworkService) {
            mGiniCaptureNetworkService = giniCaptureNetworkService;
            return this;
        }

        @Nullable
        GiniCaptureNetworkApi getGiniCaptureNetworkApi() {
            return mGiniCaptureNetworkApi;
        }

        /**
         * Set the {@link GiniCaptureNetworkApi} instance which clients can use to request network
         * calls (e.g. for sending feedback).
         *
         * @param giniCaptureNetworkApi a {@link GiniCaptureNetworkApi} instance
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setGiniCaptureNetworkApi(
                @NonNull final GiniCaptureNetworkApi giniCaptureNetworkApi) {
            mGiniCaptureNetworkApi = giniCaptureNetworkApi;
            return this;
        }

        @NonNull
        DocumentImportEnabledFileTypes getDocumentImportEnabledFileTypes() {
            return mDocumentImportEnabledFileTypes;
        }

        /**
         * Enable and configure the document import feature or disable it by passing in {@link
         * DocumentImportEnabledFileTypes#NONE}.
         *
         * <p> Disabled by default.
         *
         * @param documentImportEnabledFileTypes file types to be enabled for document import
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setDocumentImportEnabledFileTypes(
                @NonNull final DocumentImportEnabledFileTypes documentImportEnabledFileTypes) {
            mDocumentImportEnabledFileTypes = documentImportEnabledFileTypes;
            return this;
        }

        boolean isFileImportEnabled() {
            return mFileImportEnabled;
        }

        /**
         * Enable/disable the file import feature.
         *
         * <p> Disabled by default.
         *
         * @param fileImportEnabled {@code true} to enable file import
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setFileImportEnabled(final boolean fileImportEnabled) {
            mFileImportEnabled = fileImportEnabled;
            return this;
        }

        boolean isQRCodeScanningEnabled() {
            return mQRCodeScanningEnabled;
        }

        /**
         * Enable/disable the QRCode scanning feature.
         *
         * <p> Disabled by default.
         *
         * @param qrCodeScanningEnabled {@code true} to enable QRCode scanning
         *
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setQRCodeScanningEnabled(final boolean qrCodeScanningEnabled) {
            mQRCodeScanningEnabled = qrCodeScanningEnabled;
            return this;
        }

        boolean shouldShowOnboarding() {
            return mShouldShowOnboarding;
        }

        /**
         * Enable/disable the Supported Formats help screen.
         *
         * <p> Enabled by default.
         *
         * @param enabled {@code true} to show the Supported Formats help screen
         *
         * @return the {@link Builder} instance
         */
        public Builder setSupportedFormatsHelpScreenEnabled(final boolean enabled) {
            mIsSupportedFormatsHelpScreenEnabled = enabled;
            return this;
        }

        boolean isSupportedFormatsHelpScreenEnabled() {
            return mIsSupportedFormatsHelpScreenEnabled;
        }

        /**
         * Enable/disable the flash button in the Camera Screen.
         *
         * <p> Disabled by default.
         *
         * @param enabled {@code true} to show the flash button
         *
         * @return the {@link Builder} instance
         */
        public Builder setFlashButtonEnabled(final boolean enabled) {
            mFlashButtonEnabled = enabled;
            return this;
        }

        boolean isFlashButtonEnabled() {
            return mFlashButtonEnabled;
        }

        /**
         * Screen API only
         *
         * <p> Enable/disable back buttons in all Activities except {@link ReviewActivity} and
         * {@link AnalysisActivity}, which always show back buttons.
         *
         * <p> Enabled by default.
         *
         * @param enabled {@code true} to show back buttons
         *
         * @return the {@link Builder} instance
         */
        public Builder setBackButtonsEnabled(final boolean enabled) {
            mBackButtonsEnabled = enabled;
            return this;
        }

        boolean areBackButtonsEnabled() {
            return mBackButtonsEnabled;
        }

        /**
         * Set whether the camera flash is on or off by default.
         *
         * <p> If not changed, then flash is on by default.
         *
         * @param flashOn {@code true} to turn the flash on
         *
         * @return the {@link Builder} instance
         */
        public Builder setFlashOnByDefault(final boolean flashOn) {
            mIsFlashOnByDefault = flashOn;
            return this;
        }

        boolean isFlashOnByDefault() {
            return mIsFlashOnByDefault;
        }

        /**
         * Set the {@link EventTracker} instance which will be called from the different screens to inform you about the various events
         * which can occur during the usage of the Gini Capture SDK.
         *
         * @param eventTracker an {@link EventTracker} instance
         *
         * @return the {@link Builder} instance
         */
        public Builder setEventTracker(@NonNull final EventTracker eventTracker) {
            mEventTracker = eventTracker;
            return this;
        }

        EventTracker getEventTracker() {
            return mEventTracker;
        }

        @NonNull
        List<HelpItem.Custom> getCustomHelpItems() {
            return mCustomHelpItems;
        }

        /**
         * Set custom help items to be shown in the Help Screen.
         *
         * @param customHelpItems an {@link List} of {@link HelpItem.Custom} objects
         *
         * @return the {@link Builder} instance
         */
        public Builder setCustomHelpItems(@NonNull final List<HelpItem.Custom> customHelpItems) {
            this.mCustomHelpItems = customHelpItems;
            return this;
        }

        /**
         * Set whether the default Gini error logging implementation is on or not.
         *
         * <p> On by default.
         *
         * @param isOn pass {@code true} to turn on the error logger or {@code false} otherwise.
         * @return the {@link Builder} instance
         */
        public Builder setGiniErrorLoggerIsOn(final boolean isOn) {
            mGiniErrorLoggerIsOn = isOn;
            return this;
        }

        private boolean getGiniErrorLoggerIsOn() {
            return mGiniErrorLoggerIsOn;
        }

        /**
         * Set an {@link ErrorLoggerListener} to be notified of Gini Capture SDK errors.
         *
         * @param listener your {@link ErrorLoggerListener} implementation
         * @return the {@link Builder} instance
         */
        public Builder setCustomErrorLoggerListener(@NonNull final ErrorLoggerListener listener) {
            mCustomErrorLoggerListener = listener;
            return this;
        }

        @Nullable
        private ErrorLoggerListener getCustomErrorLoggerListener() {
            return mCustomErrorLoggerListener;
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static class Internal {

        private final GiniCapture mGiniCapture;

        private Throwable mReviewScreenAnalysisError;

        public Internal(@NonNull final GiniCapture giniCapture) {
            mGiniCapture = giniCapture;
        }

        @Nullable
        public GiniCaptureNetworkService getGiniCaptureNetworkService() {
            return mGiniCapture.getGiniCaptureNetworkService();
        }

        @Nullable
        public NetworkRequestsManager getNetworkRequestsManager() {
            return mGiniCapture.getNetworkRequestsManager();
        }

        @NonNull
        public DocumentDataMemoryCache getDocumentDataMemoryCache() {
            return mGiniCapture.getDocumentDataMemoryCache();
        }

        @NonNull
        public PhotoMemoryCache getPhotoMemoryCache() {
            return mGiniCapture.getPhotoMemoryCache();
        }

        public ImageDiskStore getImageDiskStore() {
            return mGiniCapture.getImageDiskStore();
        }

        public ImageMultiPageDocumentMemoryStore getImageMultiPageDocumentMemoryStore() {
            return mGiniCapture.getImageMultiPageDocumentMemoryStore();
        }

        public EventTracker getEventTracker() {
            return mGiniCapture.getEventTracker();
        }

        @Nullable
        public Throwable getReviewScreenAnalysisError() {
            return mReviewScreenAnalysisError;
        }

        public void setReviewScreenAnalysisError(@Nullable final Throwable analysisError) {
            mReviewScreenAnalysisError = analysisError;
        }

        public ErrorLogger getErrorLogger() {
            return mGiniCapture.getErrorLogger();
        }
    }

}
