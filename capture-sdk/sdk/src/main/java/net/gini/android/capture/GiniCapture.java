package net.gini.android.capture;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.gini.android.capture.camera.view.CameraNavigationBarBottomAdapter;
import net.gini.android.capture.camera.view.DefaultCameraNavigationBarBottomAdapter;
import net.gini.android.capture.help.HelpItem;
import net.gini.android.capture.help.view.DefaultHelpNavigationBarBottomAdapter;
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter;
import net.gini.android.capture.internal.cache.DocumentDataMemoryCache;
import net.gini.android.capture.internal.cache.PhotoMemoryCache;
import net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.logging.ErrorLoggerListener;
import net.gini.android.capture.network.Error;
import net.gini.android.capture.network.GiniCaptureNetworkCallback;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.onboarding.OnboardingPage;
import net.gini.android.capture.onboarding.view.DefaultOnboardingNavigationBarBottomAdapter;
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter;
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter;
import net.gini.android.capture.review.multipage.view.DefaultReviewNavigationBarBottomAdapter;
import net.gini.android.capture.review.multipage.view.ReviewNavigationBarBottomAdapter;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.CameraScreenEvent;
import net.gini.android.capture.tracking.Event;
import net.gini.android.capture.tracking.EventTracker;
import net.gini.android.capture.tracking.OnboardingScreenEvent;
import net.gini.android.capture.tracking.ReviewScreenEvent;
import net.gini.android.capture.util.CancellationToken;
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter;
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter;
import net.gini.android.capture.view.DefaultNavigationBarTopAdapter;
import net.gini.android.capture.view.DefaultOnButtonLoadingIndicatorAdapter;
import net.gini.android.capture.view.InjectedViewAdapterInstance;
import net.gini.android.capture.view.NavigationBarTopAdapter;
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT;

/**
 * Created by Alpar Szotyori on 22.02.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Single entry point for the Gini Capture SDK for configuration and interaction.
 *
 * <p> To create and configure a singleton instance use the {@link #newInstance(Context)} method and the
 * returned {@link Builder}.
 *
 * <p> Use the {@link #cleanup(Context)} method to clean up the SDK
 * Please follow the recommendations below:
 *
 * <ul>
 *     <li>Do cleanup after TAN verification.to clean up.</li>
 * </ul>
 *
 * <p> Use the {@link #transferSummary(String, String, String, String, String, Amount)}  method to
 * provide the required extraction feedback to improve the future extraction accuracy.
 * Please follow the recommendations below:
 *
 * <ul>
 *     <li>Please provide values for all necessary fields, including those that were not extracted.</li>
 *     <li>Provide the final data approved by the user (and not the initially extracted only).</li>
 * </ul>
 */
public class GiniCapture {

    private static final Logger LOG = LoggerFactory.getLogger(GiniCapture.class);
    private static GiniCapture sInstance;
    private final GiniCaptureNetworkService mGiniCaptureNetworkService;
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
    private final boolean mIsOnlyQRCodeScanning;
    private final ArrayList<OnboardingPage> mCustomOnboardingPages; // NOPMD - Bundle req. ArrayList
    private final boolean mShouldShowOnboardingAtFirstRun;
    private final boolean mMultiPageEnabled;
    private boolean mShouldShowOnboarding;
    private final boolean mIsSupportedFormatsHelpScreenEnabled;
    private final boolean mFlashButtonEnabled;
    private final boolean mIsFlashOnByDefault;
    private final EventTracker mEventTracker;
    private final List<HelpItem.Custom> mCustomHelpItems;
    private final ErrorLogger mErrorLogger;
    private final int mImportedFileSizeBytesLimit;
    private final InjectedViewAdapterInstance<NavigationBarTopAdapter> navigationBarTopAdapterInstance;
    private final InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> onboardingNavigationBarBottomAdapterInstance;
    private final InjectedViewAdapterInstance<HelpNavigationBarBottomAdapter> helpNavigationBarBottomAdapterInstance;
    private final InjectedViewAdapterInstance<CameraNavigationBarBottomAdapter> cameraNavigationBarBottomAdapterInstance;
    private final boolean isBottomNavigationBarEnabled;
    private final InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingAlignCornersIllustrationAdapterInstance;
    private final InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingLightingIllustrationAdapterInstance;
    private final InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingMultiPageIllustrationAdapterInstance;
    private final InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingQRCodeIllustrationAdapterInstance;
    private final InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter> loadingIndicatorAdapterInstance;
    private final InjectedViewAdapterInstance<ReviewNavigationBarBottomAdapter> reviewNavigationBarBottomAdapterInstance;
    private final InjectedViewAdapterInstance<OnButtonLoadingIndicatorAdapter> onButtonLoadingIndicatorAdapterInstance;
    private final EntryPoint entryPoint;

    /**
     * Retrieve the current instance.
     *
     * @return {@link GiniCapture} instance
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
     * @throws IllegalStateException when an instance already exists. Call {@link #cleanup(Context)}
     *                               before trying to create a new instance
     * @deprecated Please use {@link #newInstance(Context)} which allows instance recreation without having to
     * call {@link #cleanup(Context)} first.
     */
    @NonNull
    @Deprecated
    public static synchronized Builder newInstance() {
        if (sInstance != null) {
            throw new IllegalStateException("An instance was already created. "
                    + "Call GiniCapture.cleanup() before creating a new instance.");
        }
        return new Builder();
    }

    /**
     * Configure and create a new instance using the returned {@link Builder}.
     *
     * @return a new {@link Builder}
     * @param context Android context
     */
    @NonNull
    public static synchronized Builder newInstance(final Context context) {
        if (sInstance != null) {
            if (sInstance.mNetworkRequestsManager != null) {
                sInstance.mNetworkRequestsManager.cleanup();
            }
            cleanup(context);
        }
        return new Builder();
    }

    /**
     * Provides transfer summary to Gini.
     *
     * <p>Please provide the required extraction feedback to improve the future extraction accuracy.
     * Please follow the recommendations below:
     *
     * <ul>
     *     <li>Please provide values for all necessary fields, including those that were not extracted.</li>
     *     <li>Provide the final data approved by the user (and not the initially extracted only).</li>
     * </ul>
     *
     * @param paymentRecipient payment receiver
     * @param paymentReference ID based on Client ID (Kundennummer) and invoice ID (Rechnungsnummer)
     * @param paymentPurpose statement what this payment is for
     * @param iban international bank account
     * @param bic bank identification code
     * @param amount accepts extracted amount and currency
     *
     */
    public static synchronized void transferSummary(
                                            @NonNull final String paymentRecipient,
                                            @NonNull final String paymentReference,
                                            @NonNull final String paymentPurpose,
                                            @NonNull final String iban,
                                            @NonNull final String bic,
                                            @NonNull final Amount amount) {

        if (sInstance == null) {
            return;
        }

        Map<String, GiniCaptureSpecificExtraction> extractionMap = new HashMap<>();

        extractionMap.put("amountToPay", new GiniCaptureSpecificExtraction("amountToPay", amount.amountToPay(),
                "amount", null, emptyList()));

        extractionMap.put("paymentRecipient", new GiniCaptureSpecificExtraction("paymentRecipient", paymentRecipient,
                "companyname", null, emptyList()));

        extractionMap.put("paymentReference", new GiniCaptureSpecificExtraction("paymentReference", paymentReference,
                "reference", null, emptyList()));

        extractionMap.put("paymentPurpose", new GiniCaptureSpecificExtraction("paymentPurpose", paymentPurpose,
                "reference", null, emptyList()));

        extractionMap.put("iban", new GiniCaptureSpecificExtraction("iban", iban,
                "iban", null, emptyList()));

        extractionMap.put("bic", new GiniCaptureSpecificExtraction("bic", bic,
                "bic", null, emptyList()));


        // Test fails here if for some reason mGiniCaptureNetworkService is null
        // Added null checking to fix test fail -> or figure out something else
        final GiniCapture oldInstance = sInstance;
        if (oldInstance.mGiniCaptureNetworkService != null)
            oldInstance.mGiniCaptureNetworkService.sendFeedback(extractionMap,
                    oldInstance.mInternal.getCompoundExtractions(), new GiniCaptureNetworkCallback<Void, Error>() {
                        @Override
                        public void failure(Error error) {
                            if (oldInstance.mNetworkRequestsManager != null) {
                                oldInstance.mNetworkRequestsManager.cleanup();
                            }
                        }

                        @Override
                        public void success(Void result) {
                            if (oldInstance.mNetworkRequestsManager != null) {
                                oldInstance.mNetworkRequestsManager.cleanup();
                            }
                        }

                        @Override
                        public void cancelled() {
                            if (oldInstance.mNetworkRequestsManager != null) {
                                oldInstance.mNetworkRequestsManager.cleanup();
                            }
                        }
                    });
    }


    /**
     * Destroys the {@link GiniCapture} instance and frees up used resources.
     *
     * <p>Please provide the required extraction feedback to improve the future extraction accuracy.
     * Please follow the recommendations below:
     *
     * <ul>
     *     <li>Please provide values for all necessary fields, including those that were not extracted.</li>
     *     <li>Provide the final data approved by the user (and not the initially extracted only).</li>
     *     <li>Do cleanup after TAN verification.to clean up and provide the extraction values the user has used.</li>
     * </ul>
     *
     * @param context Android context
     * @param paymentRecipient payment receiver
     * @param paymentReference ID based on Client ID (Kundennummer) and invoice ID (Rechnungsnummer)
     * @param paymentPurpose statement what this payment is for
     * @param iban international bank account
     * @param bic bank identification code
     * @param amount accepts extracted amount and currency
     *
     * @deprecated Please use {@link #cleanup(Context)} which does not require transfer summary parameters.
     */

    @Deprecated
    public static synchronized void cleanup(@NonNull final Context context,
                                            @NonNull final String paymentRecipient,
                                            @NonNull final String paymentReference,
                                            @NonNull final String paymentPurpose,
                                            @NonNull final String iban,
                                            @NonNull final String bic,
                                            @NonNull final Amount amount) {

        if (sInstance == null) {
            return;
        }

        Map<String, GiniCaptureSpecificExtraction> extractionMap = new HashMap<>();

        extractionMap.put("amountToPay", new GiniCaptureSpecificExtraction("amountToPay", amount.amountToPay(),
                "amount", null, emptyList()));

        extractionMap.put("paymentRecipient", new GiniCaptureSpecificExtraction("paymentRecipient", paymentRecipient,
                "companyname", null, emptyList()));

        extractionMap.put("paymentReference", new GiniCaptureSpecificExtraction("paymentReference", paymentReference,
                "reference", null, emptyList()));

        extractionMap.put("paymentPurpose", new GiniCaptureSpecificExtraction("paymentPurpose", paymentPurpose,
                "reference", null, emptyList()));

        extractionMap.put("iban", new GiniCaptureSpecificExtraction("iban", iban,
                "iban", null, emptyList()));

        extractionMap.put("bic", new GiniCaptureSpecificExtraction("bic", bic,
                "bic", null, emptyList()));


        // Test fails here if for some reason mGiniCaptureNetworkService is null
        // Added null checking to fix test fail -> or figure out something else
        final GiniCapture oldInstance = sInstance;
        if (oldInstance.mGiniCaptureNetworkService != null)
            oldInstance.mGiniCaptureNetworkService.sendFeedback(extractionMap,
                    oldInstance.mInternal.getCompoundExtractions(), new GiniCaptureNetworkCallback<Void, Error>() {
                        @Override
                        public void failure(Error error) {
                            if (oldInstance.mNetworkRequestsManager != null) {
                                oldInstance.mNetworkRequestsManager.cleanup();
                            }
                        }

                        @Override
                        public void success(Void result) {
                            if (oldInstance.mNetworkRequestsManager != null) {
                                oldInstance.mNetworkRequestsManager.cleanup();
                            }
                        }

                        @Override
                        public void cancelled() {
                            if (oldInstance.mNetworkRequestsManager != null) {
                                oldInstance.mNetworkRequestsManager.cleanup();
                            }
                        }
                    });

        cleanup(context);
    }

    /**
     * Destroys the {@link GiniCapture} instance and frees up used resources.
     */
    public static void cleanup(Context context) {
        sInstance.mDocumentDataMemoryCache.clear();
        sInstance.mPhotoMemoryCache.clear();
        sInstance.mInternal.setUpdatedCompoundExtractions(emptyMap());
        sInstance.mImageMultiPageDocumentMemoryStore.clear();
        sInstance.internal().setReviewScreenAnalysisError(null);
        sInstance = null; // NOPMD
        ImageDiskStore.clear(context);
    }

    private static synchronized void createInstance(@NonNull final Builder builder) {
        sInstance = new GiniCapture(builder);
    }

    private GiniCapture(@NonNull final Builder builder) {
        mGiniCaptureNetworkService = builder.getGiniCaptureNetworkService();
        mDocumentImportEnabledFileTypes = builder.getDocumentImportEnabledFileTypes();
        mFileImportEnabled = builder.isFileImportEnabled();
        mQRCodeScanningEnabled = builder.isQRCodeScanningEnabled();
        mIsOnlyQRCodeScanning = builder.isOnlyQRCodeScanningEnabled();
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
        mIsFlashOnByDefault = builder.isFlashOnByDefault();
        mEventTracker = builder.getEventTracker();
        mCustomHelpItems = builder.getCustomHelpItems();
        mErrorLogger = new ErrorLogger(builder.getGiniErrorLoggerIsOn(),
                builder.getGiniCaptureNetworkService(),
                builder.getCustomErrorLoggerListener());
        mImportedFileSizeBytesLimit = builder.getImportedFileSizeBytesLimit();
        navigationBarTopAdapterInstance = builder.getNavigationBarTopAdapterInstance();
        onboardingNavigationBarBottomAdapterInstance = builder.getOnboardingNavigationBarBottomAdapterInstance();
        helpNavigationBarBottomAdapterInstance = builder.getHelpNavigationBarBottomAdapterInstance();
        isBottomNavigationBarEnabled = builder.isBottomNavigationBarEnabled();
        onboardingAlignCornersIllustrationAdapterInstance = builder.getOnboardingAlignCornersIllustrationAdapterInstance();
        onboardingLightingIllustrationAdapterInstance = builder.getOnboardingLightingIllustrationAdapterInstance();
        onboardingMultiPageIllustrationAdapterInstance = builder.getOnboardingMultiPageIllustrationAdapterInstance();
        onboardingQRCodeIllustrationAdapterInstance = builder.getOnboardingQRCodeIllustrationAdapterInstance();
        cameraNavigationBarBottomAdapterInstance = builder.getCameraNavigationBarBottomAdapterInstance();
        loadingIndicatorAdapterInstance = builder.getLoadingIndicatorAdapterInstance();
        reviewNavigationBarBottomAdapterInstance = builder.getReviewNavigationBarBottomAdapterInstance();
        onButtonLoadingIndicatorAdapterInstance = builder.getOnButtonLoadingIndicatorAdapterInstance();
        entryPoint = builder.getEntryPoint();
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
     * Find out whether only QRCode scanning has been enabled.
     *
     * <p> Disabled by default.
     *
     * @return {@code true} if only QRCode scanning was enabled
     */
    public boolean isOnlyQRCodeScanning() {
        return mIsOnlyQRCodeScanning;
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
     * When your application receives one or multiple
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
     * @return a {@link CancellationToken} for cancelling the import process
     */
    @NonNull
    public CancellationToken createIntentForImportedFiles(@NonNull final Intent intent,
                                                          @NonNull final Context context,
                                                          @NonNull final AsyncCallback<Intent, ImportedFileValidationException> callback) {
        return mGiniCaptureFileImport.createIntentForImportedFiles(intent, context, callback);
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
    ErrorLogger getErrorLogger() {
        return mErrorLogger;
    }

    @NonNull
    public NavigationBarTopAdapter getNavigationBarTopAdapter() {
        return navigationBarTopAdapterInstance.getViewAdapter();
    }

    @NonNull
    public OnboardingNavigationBarBottomAdapter getOnboardingNavigationBarBottomAdapter() {
        return onboardingNavigationBarBottomAdapterInstance.getViewAdapter();
    }

    @NonNull
    public HelpNavigationBarBottomAdapter getHelpNavigationBarBottomAdapter() {
        return helpNavigationBarBottomAdapterInstance.getViewAdapter();
    }

    @NonNull
    public CameraNavigationBarBottomAdapter getCameraNavigationBarBottomAdapter() {
        return cameraNavigationBarBottomAdapterInstance.getViewAdapter();
    }

    public boolean isBottomNavigationBarEnabled() {
        return isBottomNavigationBarEnabled;
    }

    @Nullable
    public OnboardingIllustrationAdapter getOnboardingAlignCornersIllustrationAdapter() {
        if (onboardingAlignCornersIllustrationAdapterInstance == null) {
            return null;
        }
        return onboardingAlignCornersIllustrationAdapterInstance.getViewAdapter();
    }

    @Nullable
    public OnboardingIllustrationAdapter getOnboardingLightingIllustrationAdapter() {
        if (onboardingLightingIllustrationAdapterInstance == null) {
            return null;
        }
        return onboardingLightingIllustrationAdapterInstance.getViewAdapter();
    }

    @Nullable
    public OnboardingIllustrationAdapter getOnboardingMultiPageIllustrationAdapter() {
        if (onboardingMultiPageIllustrationAdapterInstance == null) {
            return null;
        }
        return onboardingMultiPageIllustrationAdapterInstance.getViewAdapter();
    }

    @Nullable
    public OnboardingIllustrationAdapter getOnboardingQRCodeIllustrationAdapter() {
        if (onboardingQRCodeIllustrationAdapterInstance == null) {
            return null;
        }
        return onboardingQRCodeIllustrationAdapterInstance.getViewAdapter();
    }

    @Nullable
    public CustomLoadingIndicatorAdapter getLoadingIndicatorAdapter() {
        return loadingIndicatorAdapterInstance.getViewAdapter();
    }

    @NonNull
    public ReviewNavigationBarBottomAdapter getReviewNavigationBarBottomAdapter() {
        return reviewNavigationBarBottomAdapterInstance.getViewAdapter();
    }

    @Nullable
    public OnButtonLoadingIndicatorAdapter getOnButtonLoadingIndicatorAdapter() {
        return onButtonLoadingIndicatorAdapterInstance.getViewAdapter();
    }

    /**
     * The size limit in bytes for imported files.
     *
     * @return file size limit in bytes
     */
    public int getImportedFileSizeBytesLimit() {
        return mImportedFileSizeBytesLimit;
    }

    /**
     * The entry point used for launching the SDK.
     *
     * <p> Default value is {@code EntryPoint.BUTTON}.
     *
     * @return the {@link EntryPoint}
     */
    @NonNull
    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    /**
     * Builder for {@link GiniCapture}. To get an instance call {@link #newInstance(Context)}.
     */
    public static class Builder {

        private GiniCaptureNetworkService mGiniCaptureNetworkService;
        private DocumentImportEnabledFileTypes mDocumentImportEnabledFileTypes =
                DocumentImportEnabledFileTypes.NONE;
        private boolean mFileImportEnabled;
        private boolean mQRCodeScanningEnabled;
        private boolean mOnlyQRCodeScanningEnabled;
        private ArrayList<OnboardingPage> mOnboardingPages; // NOPMD - ArrayList required (Bundle)
        private boolean mShouldShowOnboardingAtFirstRun = true;
        private boolean mShouldShowOnboarding;
        private boolean mMultiPageEnabled;
        private boolean mIsSupportedFormatsHelpScreenEnabled = true;
        private boolean mFlashButtonEnabled;
        
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
        private int mImportedFileSizeBytesLimit = FILE_SIZE_LIMIT;
        private InjectedViewAdapterInstance<NavigationBarTopAdapter> navigationBarTopAdapterInstance = new InjectedViewAdapterInstance<>(new DefaultNavigationBarTopAdapter());
        private InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> navigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(new DefaultOnboardingNavigationBarBottomAdapter());
        private InjectedViewAdapterInstance<HelpNavigationBarBottomAdapter> helpNavigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(new DefaultHelpNavigationBarBottomAdapter());
        private InjectedViewAdapterInstance<CameraNavigationBarBottomAdapter> cameraNavigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(new DefaultCameraNavigationBarBottomAdapter());
        private boolean isBottomNavigationBarEnabled = false;
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingAlignCornersIllustrationAdapterInstance;
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingLightingIllustrationAdapterInstance;
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingMultiPageIllustrationAdapterInstance;
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> onboardingQRCodeIllustrationAdapterInstance;
        private InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter> loadingIndicatorAdapter = new InjectedViewAdapterInstance<>(new DefaultLoadingIndicatorAdapter());
        private InjectedViewAdapterInstance<ReviewNavigationBarBottomAdapter> reviewNavigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(new DefaultReviewNavigationBarBottomAdapter());

        private InjectedViewAdapterInstance<OnButtonLoadingIndicatorAdapter> onButtonLoadingIndicatorAdapterInstance = new InjectedViewAdapterInstance<>(new DefaultOnButtonLoadingIndicatorAdapter());
        private EntryPoint entryPoint = Internal.DEFAULT_ENTRY_POINT;

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
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setGiniCaptureNetworkService(
                @NonNull final GiniCaptureNetworkService giniCaptureNetworkService) {
            mGiniCaptureNetworkService = giniCaptureNetworkService;
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
         * @return the {@link Builder} instance
         */
        @NonNull
        public Builder setQRCodeScanningEnabled(final boolean qrCodeScanningEnabled) {
            mQRCodeScanningEnabled = qrCodeScanningEnabled;
            return this;
        }


        boolean isOnlyQRCodeScanningEnabled() {
            return mOnlyQRCodeScanningEnabled;
        }


        /**
         * Enable/disable only the QRCode scanning feature.
         *
         * <p> Disabled by default.
         *
         * @param onlyQRCodeScanningEnabled {@code true} to enable only QRCode scanning
         * @return the {@link Builder} instance
         */
        public Builder setOnlyQRCodeScanning(final boolean onlyQRCodeScanningEnabled) {
            mOnlyQRCodeScanningEnabled = onlyQRCodeScanningEnabled;
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
         * Set whether the camera flash is on or off by default.
         *
         * <p> If not changed, then flash is on by default.
         *
         * @param flashOn {@code true} to turn the flash on
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

        /**
         * Set a custom imported file size limit in bytes.
         *
         * @param fileSizeBytesLimit file size limit in bytes
         * @return the {@link Builder} instance
         */
        public Builder setImportedFileSizeBytesLimit(final int fileSizeBytesLimit) {
            mImportedFileSizeBytesLimit = fileSizeBytesLimit;
            return this;
        }

        public int getImportedFileSizeBytesLimit() {
            return mImportedFileSizeBytesLimit;
        }

        /**
         * Set an adapter implementation to show a custom top navigation bar.
         *
         * @param adapter a {@link NavigationBarTopAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setNavigationBarTopAdapter(@NonNull final NavigationBarTopAdapter adapter) {
            navigationBarTopAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<NavigationBarTopAdapter> getNavigationBarTopAdapterInstance() {
            return navigationBarTopAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom bottom navigation bar on the onboarding screen.
         *
         * @param adapter an {@link OnboardingNavigationBarBottomAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setOnboardingNavigationBarBottomAdapter(@NonNull final OnboardingNavigationBarBottomAdapter adapter) {
            navigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> getOnboardingNavigationBarBottomAdapterInstance() {
            return navigationBarBottomAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom bottom navigation bar on the help screen.
         *
         * @param adapter a {@link HelpNavigationBarBottomAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setHelpNavigationBarBottomAdapter(@NonNull final HelpNavigationBarBottomAdapter adapter) {
            helpNavigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<HelpNavigationBarBottomAdapter> getHelpNavigationBarBottomAdapterInstance() {
            return helpNavigationBarBottomAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom bottom navigation bar on the camera screen.
         *
         * @param adapter a {@link CameraNavigationBarBottomAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setCameraNavigationBarBottomAdapter(@NonNull final CameraNavigationBarBottomAdapter adapter) {
            cameraNavigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        private InjectedViewAdapterInstance<CameraNavigationBarBottomAdapter> getCameraNavigationBarBottomAdapterInstance() {
            return cameraNavigationBarBottomAdapterInstance;
        }

        /**
         * Enable/disable the bottom navigation bar.
         * <p>
         * Disabled by default.
         *
         * @return the {@link Builder} instance
         */
        public Builder setBottomNavigationBarEnabled(final Boolean enabled) {
            isBottomNavigationBarEnabled = enabled;
            return this;
        }

        private boolean isBottomNavigationBarEnabled() {
            return isBottomNavigationBarEnabled;
        }

        @NonNull
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> getOnboardingAlignCornersIllustrationAdapterInstance() {
            return onboardingAlignCornersIllustrationAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom illustration on the "align corners" onboarding page.
         *
         * @param adapter an {@link OnboardingIllustrationAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setOnboardingAlignCornersIllustrationAdapter(@NonNull final OnboardingIllustrationAdapter adapter) {
            onboardingAlignCornersIllustrationAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> getOnboardingLightingIllustrationAdapterInstance() {
            return onboardingLightingIllustrationAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom illustration on the "lighting" onboarding page.
         *
         * @param adapter an {@link OnboardingIllustrationAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setOnboardingLightingIllustrationAdapter(@NonNull final OnboardingIllustrationAdapter adapter) {
            onboardingLightingIllustrationAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> getOnboardingMultiPageIllustrationAdapterInstance() {
            return onboardingMultiPageIllustrationAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom illustration on the "multi-page" onboarding page.
         *
         * @param adapter an {@link OnboardingIllustrationAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setOnboardingMultiPageIllustrationAdapter(@NonNull final OnboardingIllustrationAdapter adapter) {
            onboardingMultiPageIllustrationAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<OnboardingIllustrationAdapter> getOnboardingQRCodeIllustrationAdapterInstance() {
            return onboardingQRCodeIllustrationAdapterInstance;
        }

        /**
         * Set an adapter implementation to show a custom illustration on the "QR code" onboarding page.
         *
         * @param adapter an {@link OnboardingIllustrationAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setOnboardingQRCodeIllustrationAdapter(@NonNull final OnboardingIllustrationAdapter adapter) {
            onboardingQRCodeIllustrationAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter> getLoadingIndicatorAdapterInstance() {
            return loadingIndicatorAdapter;
        }

        /**
         * Set an adapter implementation to show a custom loading indicator.
         *
         * @param adapter an {@link CustomLoadingIndicatorAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setLoadingIndicatorAdapter(@NonNull final CustomLoadingIndicatorAdapter adapter) {
            loadingIndicatorAdapter = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        @NonNull
        private InjectedViewAdapterInstance<OnButtonLoadingIndicatorAdapter> getOnButtonLoadingIndicatorAdapterInstance() {
            return onButtonLoadingIndicatorAdapterInstance;
        }

        public Builder setOnButtonLoadingIndicatorAdapter(@NonNull final OnButtonLoadingIndicatorAdapter adapter) {
            onButtonLoadingIndicatorAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        /**
         * Set an adapter implementation to show a custom bottom navigation bar on the review screen.
         *
         * @param adapter a {@link ReviewNavigationBarBottomAdapter} interface implementation
         * @return the {@link Builder} instance
         */
        public Builder setReviewBottomBarNavigationAdapter(@NonNull final ReviewNavigationBarBottomAdapter adapter) {
            reviewNavigationBarBottomAdapterInstance = new InjectedViewAdapterInstance<>(adapter);
            return this;
        }

        private InjectedViewAdapterInstance<ReviewNavigationBarBottomAdapter> getReviewNavigationBarBottomAdapterInstance() {
            return reviewNavigationBarBottomAdapterInstance;
        }

        /**
         * Set the entry point used for launching the SDK. See {@link EntryPoint} for possible values.
         *
         * <p> Default value is {@code EntryPoint.BUTTON}.
         *
         * @param entryPoint an {@link EntryPoint} enum value
         * @return the {@link Builder} instance
         */
        public Builder setEntryPoint(@NonNull final EntryPoint entryPoint) {
            this.entryPoint = entryPoint;
            return this;
        }

        private EntryPoint getEntryPoint() {
            return entryPoint;
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static class Internal {

        public static final EntryPoint DEFAULT_ENTRY_POINT = EntryPoint.BUTTON;

        private final GiniCapture mGiniCapture;

        private Throwable mReviewScreenAnalysisError;

        private Map<String, GiniCaptureCompoundExtraction> mCompoundExtractions = new HashMap<>();

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


        public void setUpdatedCompoundExtractions(@NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions) {
            mCompoundExtractions = compoundExtractions;
        }

        public Map<String, GiniCaptureCompoundExtraction> getCompoundExtractions() {
            return mCompoundExtractions;
        }

        public InjectedViewAdapterInstance<NavigationBarTopAdapter> getNavigationBarTopAdapterInstance() {
            return mGiniCapture.navigationBarTopAdapterInstance;
        }

        public InjectedViewAdapterInstance<CameraNavigationBarBottomAdapter> getCameraNavigationBarBottomAdapterInstance() {
            return mGiniCapture.cameraNavigationBarBottomAdapterInstance;
        }

        public InjectedViewAdapterInstance<HelpNavigationBarBottomAdapter> getHelpNavigationBarBottomAdapterInstance() {
            return mGiniCapture.helpNavigationBarBottomAdapterInstance;
        }

        public InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter> getLoadingIndicatorAdapterInstance() {
            return mGiniCapture.loadingIndicatorAdapterInstance;
        }

        public InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> getOnboardingNavigationBarBottomAdapterInstance() {
            return mGiniCapture.onboardingNavigationBarBottomAdapterInstance;
        }

        public InjectedViewAdapterInstance<ReviewNavigationBarBottomAdapter> getReviewNavigationBarBottomAdapterInstance() {
            return mGiniCapture.reviewNavigationBarBottomAdapterInstance;
        }

        public InjectedViewAdapterInstance<OnButtonLoadingIndicatorAdapter> getOnButtonLoadingIndicatorAdapterInstance() {
            return mGiniCapture.onButtonLoadingIndicatorAdapterInstance;
        }
    }

}
