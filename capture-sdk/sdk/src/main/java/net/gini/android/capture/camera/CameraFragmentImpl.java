package net.gini.android.capture.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDestination;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.EntryPoint;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.ImportImageFileUrisAsyncTask;
import net.gini.android.capture.ImportedFileValidationException;
import net.gini.android.capture.ProductTag;
import net.gini.android.capture.R;
import net.gini.android.capture.camera.view.CameraNavigationBarBottomAdapter;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.document.QRCodeDocument;
import net.gini.android.capture.error.ErrorFragment;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.camera.api.CameraException;
import net.gini.android.capture.internal.camera.api.CameraInterface;
import net.gini.android.capture.internal.camera.api.UIExecutor;
import net.gini.android.capture.internal.camera.api.camerax.CameraXController;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.camera.photo.PhotoEdit;
import net.gini.android.capture.internal.camera.view.QRCodePopup;
import net.gini.android.capture.internal.camera.view.education.qrcode.QRCodeEducationPopup;
import net.gini.android.capture.internal.fileimport.FileChooserFragment;
import net.gini.android.capture.internal.fileimport.FileChooserResult;
import net.gini.android.capture.internal.iban.IBANRecognizerFilter;
import net.gini.android.capture.internal.iban.IBANRecognizerImpl;
import net.gini.android.capture.internal.network.AnalysisNetworkRequestResult;
import net.gini.android.capture.internal.network.FailureException;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData;
import net.gini.android.capture.internal.qrcode.PaymentQRCodeReader;
import net.gini.android.capture.internal.qrcode.QRCodeDetectorTask;
import net.gini.android.capture.internal.qrcode.QRCodeDetectorTaskMLKit;
import net.gini.android.capture.internal.qreducation.model.FlowType;
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.internal.textrecognition.CropToCameraFrameTextRecognizer;
import net.gini.android.capture.internal.textrecognition.MLKitTextRecognizer;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.ui.IntervalToolbarMenuItemIntervalClickListener;
import net.gini.android.capture.internal.ui.ViewStubSafeInflater;
import net.gini.android.capture.internal.util.ApplicationHelper;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.internal.util.DeviceHelper;
import net.gini.android.capture.internal.util.LogSanitizer;
import net.gini.android.capture.internal.util.FileImportValidator;
import net.gini.android.capture.internal.util.MimeType;
import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.network.Error;
import net.gini.android.capture.network.model.GiniCaptureExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsFragment;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.CameraScreenEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty;
import net.gini.android.capture.util.IntentHelper;
import net.gini.android.capture.util.UriHelper;
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;

import static net.gini.android.capture.camera.CameraFragment.REQUEST_KEY;
import static net.gini.android.capture.camera.CameraFragment.RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE;
import static net.gini.android.capture.document.ImageDocument.ImportMethod;
import static net.gini.android.capture.internal.network.NetworkRequestsManager.isCancellation;
import static net.gini.android.capture.internal.qrcode.EPSPaymentParser.EXTRACTION_ENTITY_NAME;
import static net.gini.android.capture.internal.util.AndroidHelper.isMarshmallowOrLater;
import static net.gini.android.capture.internal.util.FeatureConfiguration.getDocumentImportEnabledFileTypes;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isMultiPageEnabled;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isQRCodeScanningEnabled;
import static net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackCameraScreenEvent;

/**
 * Internal use only.
 * <p>
 * Legacy class which was used to share camera fragment logic between support library (androidx) fragments and
 * native ones.
 * TODO: refactor this to use a modern architecture for the camera fragment
 */
class CameraFragmentImpl extends CameraFragmentExtension implements CameraFragmentInterface, PaymentQRCodeReader.Listener {

    @VisibleForTesting
    static final String GC_SHARED_PREFS = "GC_SHARED_PREFS";
    @VisibleForTesting
    static final int DEFAULT_ANIMATION_DURATION = 200;
    private static final long HIDE_QRCODE_DETECTED_POPUP_DELAY_MS = 2000;
    private static final long DIFFERENT_QRCODE_DETECTED_POPUP_DELAY_MS = 1000;
    private static final Logger LOG = LoggerFactory.getLogger(CameraFragmentImpl.class);

    private static final UserAnalyticsScreen.CameraAccess sScreenNamePermission =
            UserAnalyticsScreen.CameraAccess.INSTANCE;

    private static final CameraFragmentListener NO_OP_LISTENER = new CameraFragmentListener() {
        @Override
        public void onCheckImportedDocument(@NonNull final Document document,
                                            @NonNull final DocumentCheckResultCallback callback) {
            callback.documentAccepted();
        }

        @Override
        public void onError(@NonNull final GiniCaptureError error) {
            // No-op
        }

        @Override
        public void onExtractionsAvailable(
                @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {
            // No-op
        }
    };

    private static final String IN_MULTI_PAGE_STATE_KEY = "IN_MULTI_PAGE_STATE_KEY";
    private static final String IS_FLASH_ENABLED_KEY = "IS_FLASH_ENABLED_KEY";
    private static final String IS_NOT_AVAILABLE_DETECTION_POPUP_SHOWED_KEY = "IS_ARGS_NOT_AVAILABLE_DETECTION_POPUP_SHOWED_KEY";
    private static final String GENERIC_ERROR_SHOWING_STATE_KEY = "GENERIC_ERROR_SHOWING_STATE_KEY";
    private static final String GENERIC_ERROR_TYPE_KEY = "GENERIC_ERROR_TYPE_KEY";
    private static final String GENERIC_ERROR_MESSAGE_KEY = "GENERIC_ERROR_MESSAGE_KEY";
    private static final String ERROR_TYPE_MULTI_PAGE = "ERROR_TYPE_MULTI_PAGE";
    private static final String ERROR_TYPE_INVALID_FILE = "ERROR_TYPE_INVALID_FILE";
    private static final String ONLY_QR_SCANNING_OVERRIDE_IS_SET_KEY = "ONLY_QR_SCANNING_OVERRIDE_IS_SET_KEY";
    private static final String ONLY_QR_SCANNING_OVERRIDE_VALUE_KEY = "ONLY_QR_SCANNING_OVERRIDE_VALUE_KEY";
    private static final String QR_SCANNING_DISABLED_BY_USER_KEY = "QR_SCANNING_DISABLED_BY_USER_KEY";
    private static final String QR_CODE_READER_FAILED_KEY = "QR_CODE_READER_FAILED_KEY";
    private static final String UNSUPPORTED_QR_DIALOG_SHOWING_KEY = "UNSUPPORTED_QR_DIALOG_SHOWING_KEY";

    private final FragmentImplCallback mFragment;
    private final CancelListener mCancelListener;
    private final boolean addPages;
    private boolean isGenericErrorShowing = false;
    private String currentGenericErrorMessage = "";
    private String genericErrorType = "";

    @VisibleForTesting
    QRCodePopup<String> mUnsupportedQRCodePopup;

    // null = use GiniCapture setting; true = only-QR mode; false = document capture mode
    @VisibleForTesting
    @Nullable
    Boolean mOnlyQRCodeScanningRuntimeOverride = null;
    @VisibleForTesting
    boolean mQRCodeScanningDisabledByUser = false;
    @VisibleForTesting
    boolean mQRCodeReaderFailed = false;
    @VisibleForTesting
    boolean mIsUnsupportedQRDialogShowing = false;

    private View mImageCorners;
    private PhotoThumbnail mPhotoThumbnail;
    @VisibleForTesting
    boolean mInterfaceHidden;
    private boolean mInMultiPageState;
    private boolean mIsFlashEnabled = true;

    private final UIExecutor mUIExecutor = new UIExecutor();
    private CameraInterface mCameraController;
    private ImageMultiPageDocument mMultiPageDocument;
    private PaymentQRCodeReader mPaymentQRCodeReader;

    @VisibleForTesting
    UserAnalyticsEventTracker mUserAnalyticsEventTracker;


    private ConstraintLayout mLayoutRoot;
    private View mPoweredByGiniView;
    private ViewGroup mCameraPreviewContainer;
    private View mCameraPreview;
    private ImageView mCameraFocusIndicator;
    @VisibleForTesting
    ImageButton mButtonCameraTrigger;
    private ImageButton mButtonCameraFlash;
    private ViewGroup mButtonCameraFlashWrapper;
    private Button mButtonCameraFlashTrigger;
    private Group mCameraFlashButtonGroup;
    private TextView mCameraFlashButtonSubtitle;
    @VisibleForTesting
    ConstraintLayout mLayoutNoPermission;
    private ViewGroup mButtonImportDocumentWrapper;
    private Button mButtonImportDocument;
    private ConstraintLayout mCameraFrameWrapper;
    private View mActivityIndicatorBackground;
    @VisibleForTesting
    ImageView mImageFrame;
    private ViewStubSafeInflater mViewStubInflater;
    private ConstraintLayout mPaneWrapper;
    private ConstraintLayout mDetectionErrorLayout;
    private TextView mScanTextView;
    @VisibleForTesting
    TextView mIbanDetectedTextView;
    private boolean mIsTakingPicture;
    private boolean mIsDetectionErrorPopupShowed;

    private boolean mImportDocumentButtonEnabled;
    private ImportImageFileUrisAsyncTask mImportUrisAsyncTask;
    private Group mImportButtonGroup;
    @VisibleForTesting
    String mQRCodeContent;
    private boolean shouldSendUserAnalyticsTrackerForQrCodes = true;
    private boolean isIbanDetectedOnceForUserAnalytics = false;

    private InjectedViewContainer<NavigationBarTopAdapter> topAdapterInjectedViewContainer;
    private InjectedViewContainer<CustomLoadingIndicatorAdapter> mLoadingIndicator;
    private InjectedViewContainer<CameraNavigationBarBottomAdapter> mBottomInjectedContainer;

    private IBANRecognizerFilter ibanRecognizerFilter;
    private CropToCameraFrameTextRecognizer cropToCameraFrameTextRecognizer;
    private final UserAnalyticsScreen screenName = UserAnalyticsScreen.Camera.INSTANCE;
    private View mDetectionErrorDismissButton;

    CameraFragmentImpl(@NonNull final FragmentImplCallback fragment, @NonNull final CancelListener cancelListener, final boolean addPages) {
        mFragment = fragment;
        mCancelListener = cancelListener;
        fragmentListener = NO_OP_LISTENER;
        this.addPages = addPages;
    }

    @Override
    public void onPaymentQRCodeDataAvailable(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        if (mQRCodeScanningDisabledByUser) return;
        handleQRCodeDetected(paymentQRCodeData, paymentQRCodeData.getUnparsedContent());
    }

    @Override
    public void onNonPaymentQRCodeDetected(@NonNull String qrCodeContent) {
        if (mQRCodeScanningDisabledByUser) return;
        if (mIbanDetectedTextView.getVisibility() == View.VISIBLE) {
            return;
        }
        if (isQRCodeScanningEnabled()) {
            handleQRCodeDetected(null, qrCodeContent);
        }
    }

    @Override
    public void onQRCodeReaderFail() {
        LOG.warn(
                "QRCode detector dependencies are not yet available. QRCode detection is disabled.");

        mQRCodeReaderFailed = true;
        setQRDisabledTexts();
        if (!mIsDetectionErrorPopupShowed) {
            mIsDetectionErrorPopupShowed = true;
            mDetectionErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    private void handleQRCodeDetected(@Nullable final PaymentQRCodeData paymentQRCodeData,
                                      @NonNull final String qrCodeContent) {
        if (mInterfaceHidden) {
            return;
        }

        if (isPaymentQRCodeDetectionInProgress() || mUnsupportedQRCodePopup.isShown()) {
            return;
        }

        hideIBANsDetectedOnScreen();

        if (mQRCodeContent == null || !mQRCodeContent.equals(qrCodeContent)) {
            showQRCodeView(paymentQRCodeData, qrCodeContent);
        } else {
            showQRCodeViewWithDelay(paymentQRCodeData, qrCodeContent);
        }

        mInterfaceHidden = true;
    }

    private boolean isPaymentQRCodeDetectionInProgress() {
        return mPaymentQRCodePopup.isShown();
    }

    private void showQRCodeViewWithDelay(PaymentQRCodeData data, String qrCodeContent) {
        new Handler(Looper.getMainLooper())
                .postDelayed(() -> {
                    if (data == null) {
                        mQRCodeContent = qrCodeContent;
                        showUnsupportedQRCodePopup();
                    } else {
                        showQrCodePopup(data, () -> {
                            handlePaymentQRCodeData(data);
                            return Unit.INSTANCE;
                        });
                    }
                }, 1000);
    }

    private void showQRCodeView(PaymentQRCodeData data, String qrCodeContent) {
        if (data == null) {
            mQRCodeContent = qrCodeContent;
            showUnsupportedQRCodePopup();
        } else {
            showQrCodePopup(data, () -> {
                handlePaymentQRCodeData(data);
                return Unit.INSTANCE;
            });
        }
    }

    private void showUnsupportedQRCodePopup() {
        showUnsupportedQRCodePopup(true);
    }

    /**
     * @param trackScanEvent whether to report the scan to analytics. Pass {@code false} when the
     *                       popup is merely being re-shown after a configuration change (restore),
     *                       so the same unsupported scan is not tracked more than once.
     */
    private void showUnsupportedQRCodePopup(boolean trackScanEvent) {
        if (mIbanDetectedTextView.getVisibility() != View.VISIBLE) {
            // Session-pinned value; the popup resolves the same pin when showing, so this flag
            // always matches the warning type that is actually rendered.
            mIsUnsupportedQRDialogShowing = isUnsupportedQRCodeWarningEnabled();
            if (mIsUnsupportedQRDialogShowing) {
                // The dialog requires an explicit user choice; IBAN detection must not run while
                // it is visible, otherwise a detected IBAN would dismiss it. The dialog's button
                // handlers re-create the filter through updateCameraUIForCurrentMode().
                releaseIBANRecognizerFilter();
            }
            mUnsupportedQRCodePopup.show(null);
            if (trackScanEvent) {
                sendQRCodeScannedEventToUserAnalytics(false);
            }
        }
    }

    @VisibleForTesting
    void onUnsupportedQRCodePopupHidden() {
        mIsUnsupportedQRDialogShowing = false;
        mQRCodeContent = null;
        mInterfaceHidden = false;
    }

    @VisibleForTesting
    void enableOnlyQRScanning() {
        mIsUnsupportedQRDialogShowing = false;
        mQRCodeScanningDisabledByUser = false;
        mInterfaceHidden = false;
        mQRCodeContent = null;
        mOnlyQRCodeScanningRuntimeOverride = true;
        updateCameraUIForCurrentMode();
    }

    @VisibleForTesting
    void enableDocumentCapture() {
        // The runtime override must never win over the integrator's QR-only configuration:
        // with setOnlyQRCodeScanning(true) document capture is not available in this session.
        if (GiniCapture.hasInstance()
                && GiniCapture.getInstance().isOnlyQRCodeScanning()
                && GiniCapture.getInstance().isQRCodeScanningEnabled()) {
            return;
        }
        mIsUnsupportedQRDialogShowing = false;
        mQRCodeScanningDisabledByUser = true;
        mInterfaceHidden = false;
        mQRCodeContent = null;
        mOnlyQRCodeScanningRuntimeOverride = false;
        updateCameraUIForCurrentMode();
    }

    private void updateCameraUIForCurrentMode() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) return;

        if (isOnlyQRCodeScanningEnabled()) {
            initOnlyQRScanning();
            mBottomInjectedContainer.setInjectedViewAdapterHolder(null);
            mImportButtonGroup.setVisibility(View.GONE);
            mImportDocumentButtonEnabled = false;
            releaseIBANRecognizerFilter();
        } else {
            mPaneWrapper.setVisibility(View.VISIBLE);
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) mImageFrame.getLayoutParams();
            params.dimensionRatio = "1:1.414";
            params.leftMargin = (int) activity.getResources().getDimension(R.dimen.gc_medium);
            params.rightMargin = (int) activity.getResources().getDimension(R.dimen.gc_medium);
            mImageFrame.setLayoutParams(params);
            setBottomInjectedViewContainer();
            initViews();
            if (GiniCapture.hasInstance()
                    && GiniCapture.getInstance().getEntryPoint() == EntryPoint.FIELD) {
                initIBANRecognizerFilter();
            }
            if (!isQRCodeScanningAvailable()) {
                setQRDisabledTexts();
            }
        }
        setTopBarInjectedViewContainer();
    }

    @VisibleForTesting
    long getHideQRCodeDetectedPopupDelayMs() {
        return HIDE_QRCODE_DETECTED_POPUP_DELAY_MS;
    }

    @VisibleForTesting
    long getDifferentQRCodeDetectedPopupDelayMs() {
        return DIFFERENT_QRCODE_DETECTED_POPUP_DELAY_MS;
    }

    @Override
    public void setListener(@NonNull final CameraFragmentListener listener) {
        fragmentListener = listener;
    }

    public void onCreate(final Bundle savedInstanceState) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        initFlashState();
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }
    }

    private void initFlashState() {
        if (GiniCapture.hasInstance()) {
            mIsFlashEnabled = GiniCapture.getInstance().isFlashOnByDefault();
        }
    }

    private void restoreSavedState(@NonNull final Bundle savedInstanceState) {
        mInMultiPageState = savedInstanceState.getBoolean(IN_MULTI_PAGE_STATE_KEY);
        mIsFlashEnabled = savedInstanceState.getBoolean(IS_FLASH_ENABLED_KEY);
        mIsDetectionErrorPopupShowed = savedInstanceState.getBoolean(IS_NOT_AVAILABLE_DETECTION_POPUP_SHOWED_KEY);
        mQRCodeScanningDisabledByUser = savedInstanceState.getBoolean(QR_SCANNING_DISABLED_BY_USER_KEY);
        mQRCodeReaderFailed = savedInstanceState.getBoolean(QR_CODE_READER_FAILED_KEY);
        mIsUnsupportedQRDialogShowing = savedInstanceState.getBoolean(UNSUPPORTED_QR_DIALOG_SHOWING_KEY);
        if (savedInstanceState.getBoolean(ONLY_QR_SCANNING_OVERRIDE_IS_SET_KEY, false)) {
            mOnlyQRCodeScanningRuntimeOverride = savedInstanceState.getBoolean(ONLY_QR_SCANNING_OVERRIDE_VALUE_KEY);
        } else {
            mOnlyQRCodeScanningRuntimeOverride = null;
        }
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                      final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_camera, container, false);
        mUserAnalyticsEventTracker = UserAnalytics.INSTANCE.getAnalyticsEventTracker();

        bindViews(view);
        setContentDescriptions();
        preventPaneClickThrough();
        setCustomLoadingIndicator();
        setInputHandlers();

        initMultiPageDocument();

        setTopBarInjectedViewContainer();
        setBottomInjectedViewContainer();
        createPopups(view);
        initOnlyQRScanning();

        if (!isQRCodeScanningAvailable()) {
            setQRDisabledTexts();
        }

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        handleOnBackPressed();
        showGenericErrorIfNeeded(savedInstanceState);
        if (mOnlyQRCodeScanningRuntimeOverride != null) {
            updateCameraUIForCurrentMode();
        }
        if (mIsUnsupportedQRDialogShowing) {
            // Re-showing after a configuration change: do not re-track the scan event.
            showUnsupportedQRCodePopup(false);
        }
    }

    private void showGenericErrorIfNeeded(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isGenericErrorShowing = savedInstanceState.getBoolean(GENERIC_ERROR_SHOWING_STATE_KEY, false);
            currentGenericErrorMessage = savedInstanceState.getString(GENERIC_ERROR_MESSAGE_KEY, "");
            genericErrorType = savedInstanceState.getString(GENERIC_ERROR_TYPE_KEY, "");

            if (isGenericErrorShowing && !genericErrorType.isEmpty()) {
                if (genericErrorType.equalsIgnoreCase(ERROR_TYPE_INVALID_FILE) && !currentGenericErrorMessage.isEmpty()) {
                    showInvalidFileAlert(currentGenericErrorMessage);
                } else if (genericErrorType.equalsIgnoreCase(ERROR_TYPE_MULTI_PAGE)) {
                    showMultiPageLimitError();
                }
            }
        }
    }

    private void handleOnBackPressed() {
        final FragmentActivity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        activity.getOnBackPressedDispatcher().addCallback(mFragment.getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!addPages) {
                    trackCameraScreenCloseTappedEventIfNeeded();
                }
                trackCameraScreenEvent(CameraScreenEvent.EXIT);
                trackCameraAccessPermissionRequiredCloseClickedEventIfNeeded();
                onBackPressed();
            }
        });
    }

    private void addCameraPreviewView() {
        final Activity activity = mFragment
                .getActivity();
        if (activity == null) {
            return;
        }
        mCameraPreview = mCameraController.getPreviewView(activity);
        if (mCameraPreview.getParent() == null) {
            mCameraPreviewContainer.addView(mCameraPreview);
        }
    }

    private void createPopups(@NonNull View view) {
        mPaymentQRCodePopup =
                new QRCodePopup<>(mFragment, mCameraFrameWrapper, mActivityIndicatorBackground, mLoadingIndicator,
                        getDifferentQRCodeDetectedPopupDelayMs(), true,
                        paymentQRCodeData -> {
                            if (paymentQRCodeData == null) {
                                return null;
                            }
                            handlePaymentQRCodeData(paymentQRCodeData);
                            return null;
                        },
                        null, // onHide
                        null, // onScanAnotherQRCode
                        null, // onCaptureDocument
                        () -> false); // isNewWarningEnabled — unsupported QR codes only

        mUnsupportedQRCodePopup =
                new QRCodePopup<>(mFragment, mCameraFrameWrapper, mActivityIndicatorBackground, null,
                        getHideQRCodeDetectedPopupDelayMs(), false, null, () -> {
                    onUnsupportedQRCodePopupHidden();
                    return null;
                }, () -> {
                    enableOnlyQRScanning();
                    return null;
                }, () -> {
                    enableDocumentCapture();
                    return null;
                },
                        // Deliberately a supplier, not a captured value: the configuration may not
                        // be loaded yet at view creation, so the warning type is resolved (and
                        // pinned for the session) when the popup is first shown.
                        this::isUnsupportedQRCodeWarningEnabled);
        // Neither popup is handed the ingredient brand element. It is a single view shared by the
        // two mutually exclusive halves of the QR-code analysis step, so this fragment owns it
        // alone — see setPoweredByGiniVisible.
        qrCodeEducationPopup = new QRCodeEducationPopup<>(
                view.findViewById(R.id.gc_qr_code_education_compose_view));
    }

    /**
     * The single writer of {@link #mPoweredByGiniView}, the Gini ingredient brand element.
     *
     * <p>The badge is one view shared by both halves of the QR-code analysis step — invoice
     * retrieval ({@code QRCodePopup}) and QR-code education ({@code QRCodeEducationPopup}) — which
     * are mutually exclusive. While each popup wrote the view itself, whichever half reached its
     * own end first took the badge down while the other half was still on screen: the education
     * animation always ends after a fixed 4.5s, the retrieval popup is hidden when the backend
     * answers, and neither event marks the end of the step.
     *
     * <p>The badge tracks whichever surface is covering the live preview, because R13 forbids it
     * while the preview is up and the shutter usable. It is switched on in exactly two places:
     * {@code CameraFragmentExtension.showQrCodePopup}'s education branch, next to the full-screen
     * overlay that goes up in the same frame, and in {@link #analyzeQRCode} right after
     * {@code showActivityIndicatorAndDisableInteraction()}, which is the instant the retrieval half
     * dims the preview and disables interaction.
     *
     * <p>It is switched off where the covering surface goes away: next to
     * {@code mPaymentQRCodePopup.hide()} in {@link #analyzeQRCode}, but only when the education
     * half is not running (that overlay outlives the network call and is taken down by navigation
     * alone); unconditionally on the no-results branch and in {@link #handleAnalysisError}, which
     * both navigate away and therefore end both halves; and in {@link #onStop()}, the outermost
     * end. Nothing else may touch the view.
     */
    @Override
    protected void setPoweredByGiniVisible(final boolean visible) {
        if (mPoweredByGiniView != null) {
            mPoweredByGiniView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public void onStart() {
        getUpdateFlowTypeUseCase().execute(null);
        checkGiniCaptureInstance();
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        initViews();
        initCameraController(activity);
        addCameraPreviewView();
        initQRCodeReader();
        if (GiniCapture.hasInstance()
                && GiniCapture.getInstance().getEntryPoint() == EntryPoint.FIELD
                && !isOnlyQRCodeScanningEnabled()
                // Keep IBAN detection deactivated while the unsupported QR code dialog is
                // visible (e.g. restored after a configuration change).
                && !mIsUnsupportedQRDialogShowing) {
            initIBANRecognizerFilter();
        }

        if (isCameraPermissionGranted()) {
            openCamera().thenAccept(unused -> {
                enableTapToFocus();
                initFlashButton();
            });
        } else {
            showNoPermissionView();
        }

        setFileChooserFragmentResultListener();
    }

    private void setFileChooserFragmentResultListener() {
        mFragment.getParentFragmentManager().setFragmentResultListener(FileChooserFragment.REQUEST_KEY, mFragment.getViewLifecycleOwner(), (requestKey, result) -> {
            final FileChooserResult fileChooserResult = BundleCompat.getParcelable(result, FileChooserFragment.RESULT_KEY, FileChooserResult.class);
            if (fileChooserResult != null) {
                handleFileChooserResult(fileChooserResult);
            }
        });
    }

    public void handleFileChooserResult(@NonNull FileChooserResult result) {
        if (result instanceof FileChooserResult.FilesSelected) {
            importDocumentFromIntent(((FileChooserResult.FilesSelected) result).getDataIntent());
        } else if (result instanceof FileChooserResult.FilesSelectedUri) {
            importDocumentFromUriList(((FileChooserResult.FilesSelectedUri) result).getList());
        } else if (result instanceof FileChooserResult.Error) {
            final GiniCaptureError error = ((FileChooserResult.Error) result).getError();
            LOG.error("Document import failed: {}", LogSanitizer.sanitize(error.getMessage()));
            showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
        }
    }

    private void checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            mFragment.findNavController().navigate(CameraFragmentDirections.toErrorFragment(
                    ErrorType.GENERAL, mMultiPageDocument));
        }
    }

    private boolean isCameraPermissionGranted() {
        final Activity activity = mFragment.getActivity();
        return activity != null && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void initFlashButton() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (mCameraController.isFlashAvailable()) {
            if (GiniCapture.hasInstance() && GiniCapture.getInstance().isFlashButtonEnabled()) {
                mCameraFlashButtonGroup.setVisibility(View.VISIBLE);
            }
            updateCameraFlashState();
        }

    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public void onResume() {
        initMultiPageDocument();
    }

    private void initMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            final ImageMultiPageDocument multiPageDocument =
                    GiniCapture.getInstance().internal()
                            .getImageMultiPageDocumentMemoryStore().getMultiPageDocument();
            if (multiPageDocument != null && multiPageDocument.getDocuments().size() > 0) {
                mMultiPageDocument = multiPageDocument;
                mInMultiPageState = true;
                updatePhotoThumbnail();

                topAdapterInjectedViewContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                    final boolean isBottomNavigationBarEnabled = GiniCapture.getInstance().isBottomNavigationBarEnabled();
                    injectedViewAdapter.setNavButtonType(isBottomNavigationBarEnabled ? NavButtonType.NONE : NavButtonType.BACK);
                    return Unit.INSTANCE;
                });
                mBottomInjectedContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                    injectedViewAdapter.setBackButtonVisibility(View.VISIBLE);
                    return Unit.INSTANCE;
                });
            } else {
                mInMultiPageState = false;
                mMultiPageDocument = null;
                mPhotoThumbnail.removeImage();

                topAdapterInjectedViewContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                    injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);
                    return Unit.INSTANCE;
                });
                mBottomInjectedContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                    injectedViewAdapter.setBackButtonVisibility(View.GONE);
                    return Unit.INSTANCE;
                });
            }
        }
    }

    protected void initQRCodeReader() {

        final GiniCapture giniCapture = GiniCapture.hasInstance() ? GiniCapture.getInstance() : null;

        // Skip initialization for CxExtractions
        if (giniCapture != null
                && giniCapture.getProductTag() == ProductTag.CxExtractions.INSTANCE) {
            return;
        }
        if (mPaymentQRCodeReader != null) {
            return;
        }
        final QRCodeDetectorTask qrCodeDetectorTask =
                new QRCodeDetectorTaskMLKit();
        qrCodeDetectorTask.checkAvailability(new QRCodeDetectorTask.Callback() {
            @Override
            public void onResult(final boolean isAvailable) {
                if (isAvailable) {
                    mPaymentQRCodeReader = PaymentQRCodeReader.newInstance(qrCodeDetectorTask);
                    mPaymentQRCodeReader.setListener(CameraFragmentImpl.this);
                } else {
                    LOG.warn(
                            "QRCode detector dependencies are not yet available. QRCode detection is disabled.");
                }
            }

            @Override
            public void onInterrupted() {
                LOG.debug(
                        "Checking whether the QRCode detector task is operational was interrupted.");
            }
        });
    }

    private void initIBANRecognizerFilter() {
        if (ibanRecognizerFilter != null) {
            return;
        }
        cropToCameraFrameTextRecognizer = new CropToCameraFrameTextRecognizer(MLKitTextRecognizer.newInstance());
        ibanRecognizerFilter = new IBANRecognizerFilter(new IBANRecognizerImpl(cropToCameraFrameTextRecognizer), this::handleIBANsDetected);
    }

    private void releaseIBANRecognizerFilter() {
        if (ibanRecognizerFilter != null) {
            ibanRecognizerFilter.cleanup();
            ibanRecognizerFilter = null; // NOPMD
        }
    }

    private void enableTapToFocus() {
        mCameraController.enableTapToFocus(new CameraInterface.TapToFocusListener() {
            @Override
            public void onFocusing(@NonNull final Point point, @NonNull final Size previewViewSize) {
                showFocusIndicator(point, previewViewSize);
            }

            @Override
            public void onFocused(final boolean success) {
                hideFocusIndicator();
            }
        });
    }

    private void showFocusIndicator(@NonNull final Point point, @NonNull final Size previewViewSize) {
        final FragmentActivity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }

        final float offsetX = (float) (previewViewSize.width - mCameraPreviewContainer.getWidth()) / 2.0f;
        final float centerOffsetX = (float) mCameraFocusIndicator.getWidth() / 2.0f;
        final float offsetY = (float) (previewViewSize.height - mCameraPreviewContainer.getHeight()) / 2.0f;
        final float centerOffsetY = (float) mCameraFocusIndicator.getHeight() / 2.0f;

        mCameraFocusIndicator.setX(point.x - offsetX - centerOffsetX);
        mCameraFocusIndicator.setY(point.y - offsetY - centerOffsetY);

        mCameraFocusIndicator.animate().setDuration(DEFAULT_ANIMATION_DURATION).alpha(1.0f);
    }

    private void hideFocusIndicator() {
        mCameraFocusIndicator.animate().setDuration(DEFAULT_ANIMATION_DURATION).alpha(0.0f);
    }

    private CompletableFuture<Void> openCamera() {
        LOG.info("Opening camera");
        return mCameraController.open()
                .handle((aVoid, throwable) -> {
                    if (throwable != null) {
                        if (throwable.getCause() instanceof CameraException) {
                            final CameraException cameraException = (CameraException) throwable.getCause();
                            switch (cameraException.getType()) {
                                case NO_ACCESS:
                                    showNoPermissionView();
                                    break;
                                case NO_BACK_CAMERA:
                                case OPEN_FAILED:
                                    handleError(GiniCaptureError.ErrorCode.CAMERA_OPEN_FAILED,
                                            "Failed to open camera", cameraException);
                                    break;
                                case NO_PREVIEW:
                                    handleError(GiniCaptureError.ErrorCode.CAMERA_NO_PREVIEW,
                                            "Failed to open camera", cameraException);
                                    break;
                                case SHOT_FAILED:
                                    handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                            "Failed to open camera", cameraException);
                                    break;
                            }
                        } else {
                            handleError(GiniCaptureError.ErrorCode.CAMERA_UNKNOWN,
                                    "Failed to open camera", throwable.getCause());
                        }
                    } else {
                        LOG.info("Camera opened");
                        trackCameraScreenShownEvent();
                        hideNoPermissionView();
                    }
                    return null;
                });
    }

    void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putBoolean(IN_MULTI_PAGE_STATE_KEY, mInMultiPageState);
        outState.putBoolean(IS_FLASH_ENABLED_KEY, mIsFlashEnabled);
        outState.putBoolean(IS_NOT_AVAILABLE_DETECTION_POPUP_SHOWED_KEY, mIsDetectionErrorPopupShowed);
        outState.putString(GENERIC_ERROR_MESSAGE_KEY, currentGenericErrorMessage);
        outState.putBoolean(GENERIC_ERROR_SHOWING_STATE_KEY, isGenericErrorShowing);
        outState.putString(GENERIC_ERROR_TYPE_KEY, genericErrorType);
        outState.putBoolean(QR_SCANNING_DISABLED_BY_USER_KEY, mQRCodeScanningDisabledByUser);
        outState.putBoolean(QR_CODE_READER_FAILED_KEY, mQRCodeReaderFailed);
        outState.putBoolean(UNSUPPORTED_QR_DIALOG_SHOWING_KEY, mIsUnsupportedQRDialogShowing);
        outState.putBoolean(ONLY_QR_SCANNING_OVERRIDE_IS_SET_KEY, mOnlyQRCodeScanningRuntimeOverride != null);
        if (mOnlyQRCodeScanningRuntimeOverride != null) {
            outState.putBoolean(ONLY_QR_SCANNING_OVERRIDE_VALUE_KEY, mOnlyQRCodeScanningRuntimeOverride);
        }
    }

    void onStop() {
        closeCamera();
        if (mPaymentQRCodePopup != null) {
            mPaymentQRCodePopup.hide();
        }
        if (mUnsupportedQRCodePopup != null) {
            mUnsupportedQRCodePopup.hide();
        }
        // The QR-code analysis step cannot outlive the screen, so stopping is its outermost end.
        // It is the end that matters for the education half: its overlay is never hidden, so on
        // the success path that half's badge is deliberately left up until the navigation
        // triggered by onQrCodeRecognized stops this fragment. The retrieval half no longer
        // relies on this — it hides the badge the moment its popup goes away, because there the
        // live preview and the shutter come back immediately (R13). When the user returns to the
        // camera from a no-results or error destination the view is re-created with the badge
        // GONE, so the live preview never carries it either way.
        setPoweredByGiniVisible(false);
    }

    void onDestroy() {
        if (mImportUrisAsyncTask != null) {
            mImportUrisAsyncTask.cancel(true);
        }
    }

    private void closeCamera() {
        LOG.info("Closing camera");
        if (mPaymentQRCodeReader != null) {
            mPaymentQRCodeReader.release();
            mPaymentQRCodeReader = null; // NOPMD
        }
        releaseIBANRecognizerFilter();
        mCameraController.disableTapToFocus();
        mCameraController.setPreviewCallback(null);
        mCameraController.stopPreview();
        mCameraController.close();
        LOG.info("Camera closed");
    }

    private void bindViews(final View view) {
        mLayoutRoot = view.findViewById(R.id.gc_root);
        mCameraPreviewContainer = view.findViewById(R.id.gc_camera_preview_container);
        mImageCorners = view.findViewById(R.id.gc_camera_frame);
        mCameraFocusIndicator = view.findViewById(R.id.gc_camera_focus_indicator);
        mButtonCameraTrigger = view.findViewById(R.id.gc_button_camera_trigger);
        mButtonCameraFlash = view.findViewById(R.id.gc_button_camera_flash);
        mButtonCameraFlashWrapper = view.findViewById(R.id.gc_flash_group_wrapper);
        mButtonCameraFlashTrigger = view.findViewById(R.id.gc_button_flash);
        mCameraFlashButtonGroup = view.findViewById(R.id.gc_camera_flash_button_group);
        mCameraFlashButtonSubtitle = view.findViewById(R.id.gc_camera_flash_button_subtitle);
        final ViewStub stubNoPermission = view.findViewById(R.id.gc_stub_camera_no_permission);
        mViewStubInflater = new ViewStubSafeInflater(stubNoPermission);
        mButtonImportDocumentWrapper = view.findViewById(R.id.gc_button_import_wrapper);
        mButtonImportDocument = view.findViewById(R.id.gc_button_import);
        mImportButtonGroup = view.findViewById(R.id.gc_document_import_button_group);
        mActivityIndicatorBackground =
                view.findViewById(R.id.gc_activity_indicator_background);
        mPhotoThumbnail = view.findViewById(R.id.gc_photo_thumbnail);
        topAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
        mPoweredByGiniView = view.findViewById(R.id.gc_powered_by_gini);
        mBottomInjectedContainer = view.findViewById(R.id.gc_injected_navigation_bar_container_bottom);
        mImageFrame = view.findViewById(R.id.gc_camera_frame);
        mCameraFrameWrapper = view.findViewById(R.id.gc_camera_frame_wrapper);
        mPaneWrapper = view.findViewById(R.id.gc_pane_wrapper);
        mLoadingIndicator = view.findViewById(R.id.gc_injected_loading_indicator);
        mIbanDetectedTextView = view.findViewById(R.id.gc_iban_detected);
        mDetectionErrorLayout = view.findViewById(R.id.gc_detection_error_layout);
        mDetectionErrorDismissButton = mDetectionErrorLayout.findViewById(R.id.gc_detection_error_popup_dismiss_button);

        if (!ContextHelper.isTablet(mFragment.getActivity())) {
            mScanTextView = view.findViewById(R.id.gc_camera_title);
        }
        adjustHeightToErrorDetectionLayout();
    }

    private void adjustHeightToErrorDetectionLayout() {
        final Activity activity = mFragment.getActivity();
        if (activity != null && ContextHelper.isFontScaled(activity)) {
            NestedScrollView scrollView = mDetectionErrorLayout.findViewById(R.id.gc_scroll_container);
            ViewGroup.LayoutParams params = scrollView.getLayoutParams();
            params.height = (int) Objects.requireNonNull(mFragment.getActivity()).getResources().getDimension(R.dimen.gc_large_100);
            scrollView.setLayoutParams(params);
        }
    }

    private void setContentDescriptions() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }

        if (mPhotoThumbnail != null) {
            mPhotoThumbnail.setContentDescriptionThumbnail(activity.getString(R.string.gc_photo_review_content_description));
        }
    }

    private void preventPaneClickThrough() {
        mPaneWrapper.setEnabled(false);
        mPaneWrapper.setOnClickListener(v -> {
        });
    }

    private void setTopBarInjectedViewContainer() {
        if (GiniCapture.hasInstance()) {
            topAdapterInjectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(), injectedViewAdapter -> {
                if (mFragment.getActivity() == null)
                    return;

                boolean isBottomBarEnabled = GiniCapture.getInstance().isBottomNavigationBarEnabled();

                if (isOnlyQRCodeScanningEnabled()) {
                    injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);
                } else if (mMultiPageDocument != null && !mMultiPageDocument.getDocuments().isEmpty()) {
                    injectedViewAdapter.setNavButtonType(isBottomBarEnabled ? NavButtonType.NONE : NavButtonType.BACK);
                } else {
                    injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);
                }

                if (isOnlyQRCodeScanningEnabled()) {
                    injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_info_label_only_qr));
                } else {
                    if (ContextHelper.isTablet(mFragment.getActivity())) {
                        if (isQRCodeScanningAvailable()) {
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_info_label_invoice_and_qr));
                        } else {
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_info_label_only_invoice));
                        }
                    } else {
                        if (ContextHelper.isPortraitOrientation(mFragment.getActivity()))
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_title_camera));
                        else if (isQRCodeScanningAvailable())
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_top_bar_title_landscape));
                        else
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_info_label_only_invoice));
                    }
                }

                if (!isBottomBarEnabled && !isOnlyQRCodeScanningEnabled()) {
                    injectedViewAdapter.setMenuResource(R.menu.gc_camera);
                    injectedViewAdapter.setOnMenuItemClickListener(new IntervalToolbarMenuItemIntervalClickListener(item -> {
                        if (item.getItemId() == R.id.gc_action_show_onboarding) {
                            startHelpActivity();
                        } else {
                            throw new UnsupportedOperationException("Unknown menu item id. Please don't call our OnMenuItemClickListener for custom menu items.");
                        }
                        return true;
                    }));
                }

                injectedViewAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> {
                    trackCameraAccessPermissionRequiredCloseClickedEventIfNeeded();
                    trackCameraScreenCloseTappedEventIfNeeded();
                    onBackPressed();
                }));
            }));

        }
    }


    private void setBottomInjectedViewContainer() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled() && !isOnlyQRCodeScanningEnabled()) {
            mBottomInjectedContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getCameraNavigationBarBottomAdapterInstance(),
                    injectedViewAdapter -> {
                        boolean isEmpty = mMultiPageDocument == null || mMultiPageDocument.getDocuments().isEmpty();
                        injectedViewAdapter.setBackButtonVisibility(isEmpty ? View.GONE : View.VISIBLE);

                        injectedViewAdapter.setOnBackButtonClickListener(new IntervalClickListener(v -> {
                            trackCameraAccessPermissionRequiredCloseClickedEventIfNeeded();
                            trackCameraScreenCloseTappedEventIfNeeded();
                            onBackPressed();
                        }));

                        injectedViewAdapter.setOnHelpButtonClickListener(new IntervalClickListener(v -> {
                            startHelpActivity();
                        }));
                    }));
        }
    }

    private void setCustomLoadingIndicator() {
        if (GiniCapture.hasInstance()) {
//            mLoadingIndicator.invalidate();
            mLoadingIndicator.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(GiniCapture.getInstance().internal().getLoadingIndicatorAdapterInstance(), injectedViewAdapter -> {
            }));
//            mLoadingIndicator.setInjectedViewAdapter(GiniCapture.getInstance().getloadingIndicatorAdapter());

//            if (mLoadingIndicator.getInjectedViewAdapter() != null)
//                mLoadingIndicator.getInjectedViewAdapter().onHidden();
        }
    }

    private void setmIsTakingPicture(boolean mIsTakingPicture) {
        this.mIsTakingPicture = mIsTakingPicture;

        if (mIsTakingPicture) {
            disableInteraction();
        } else {
            enableInteraction();
        }
    }

    @VisibleForTesting
    void startHelpActivity() {
        if (mIsTakingPicture) {
            return;
        }

        mFragment.findNavController().navigate(CameraFragmentDirections.toHelpFragment());

        trackCameraScreenEvent(CameraScreenEvent.HELP);

        trackCameraScreenHelpTappedIfNeeded();
        trackCameraAccessPermissionRequiredHelpClickedEventIfNeeded();
    }

    private void initOnlyQRScanning() {
        if (isOnlyQRCodeScanningEnabled()) {

            mPaneWrapper.setVisibility(View.GONE);

            ConstraintLayout.LayoutParams params = ((ConstraintLayout.LayoutParams) mImageFrame.getLayoutParams());

            params.dimensionRatio = "1:1";
            params.leftMargin = (int) Objects.requireNonNull(mFragment.getActivity()).getResources().getDimension(R.dimen.gc_large_32);
            params.rightMargin = (int) Objects.requireNonNull(mFragment.getActivity()).getResources().getDimension(R.dimen.gc_large_32);
        }
    }

    @Override
    protected boolean isOnlyQRCodeScanningEnabled() {
        if (mOnlyQRCodeScanningRuntimeOverride != null) {
            return mOnlyQRCodeScanningRuntimeOverride;
        }
        if (!GiniCapture.hasInstance()) {
            return false;
        }
        return GiniCapture.getInstance().isOnlyQRCodeScanning() && GiniCapture.getInstance().isQRCodeScanningEnabled();
    }

    private boolean isQRCodeScanningAvailable() {
        return isQRCodeScanningEnabled()
                && !mQRCodeScanningDisabledByUser
                && !mQRCodeReaderFailed;
    }

    private void initViews() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (!mInterfaceHidden && isDocumentImportEnabled(activity)) {
            mImportDocumentButtonEnabled = true;
            mImportButtonGroup.setVisibility(View.VISIBLE);
            showImportDocumentButtonAnimated();
        }
    }

    private boolean isDocumentImportEnabled(@NonNull final Activity activity) {
//        return getDocumentImportEnabledFileTypes()
//                != DocumentImportEnabledFileTypes.NONE
//                && FileChooserActivity.canChooseFiles(activity);
        return getDocumentImportEnabledFileTypes()
                != DocumentImportEnabledFileTypes.NONE
                && FileChooserFragment.canChooseFiles(activity);
    }

    private void setInputHandlers() {
        ClickListenerExtKt.setIntervalClickListener(mButtonCameraTrigger, v -> {
            if (mUserAnalyticsEventTracker != null) {
                mUserAnalyticsEventTracker.trackEvent(
                        UserAnalyticsEvent.CAPTURE_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                                add(new UserAnalyticsEventProperty.IbanDetectionLayerVisible(isIbanDetectedOnceForUserAnalytics));
                            }
                        }
                );
            }
            onCameraTriggerClicked();
        });

        ClickListenerExtKt.setIntervalClickListener(mButtonCameraFlashTrigger, v -> {
            if (mUserAnalyticsEventTracker != null) {
                mUserAnalyticsEventTracker.trackEvent(
                        UserAnalyticsEvent.FLASH_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                                add(new UserAnalyticsEventProperty.FlashActive(mCameraController.isFlashEnabled()));
                            }
                        }
                );
            }
            mIsFlashEnabled = !mCameraController.isFlashEnabled();
            updateCameraFlashState();
        });

        ClickListenerExtKt.setIntervalClickListener(mButtonImportDocument, v -> {
            if (mUserAnalyticsEventTracker != null) {
                mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.IMPORT_FILES_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                            }
                        });
            }
            showFileChooser();
        });

        ClickListenerExtKt.setIntervalClickListener(mPhotoThumbnail, v -> {
            if (mUserAnalyticsEventTracker != null) {
                mUserAnalyticsEventTracker.trackEvent(
                        UserAnalyticsEvent.MULTIPLE_PAGES_CAPTURED_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                                add(new UserAnalyticsEventProperty.DocumentPageNumber(mMultiPageDocument.getDocuments().size()));
                            }
                        }
                );
            }
            onBackPressed();
        });

        ClickListenerExtKt.setIntervalClickListener(mDetectionErrorDismissButton, v -> {
            mDetectionErrorLayout.setVisibility(View.GONE);
        });
    }

    @VisibleForTesting
    void onCameraTriggerClicked() {
        LOG.info("Taking picture");
        if (exceedsMultiPageLimit()) {
            showMultiPageLimitError();
            return;
        }
        if (!mCameraController.isPreviewRunning()) {
            LOG.info("Will not take picture: preview must be running");
            return;
        }
        if (mIsTakingPicture) {
            LOG.info("Already taking a picture");
            return;
        }
        setmIsTakingPicture(true);
        mCameraController.takePicture()
                .handle((CompletableFuture.BiFun<Photo, Throwable, Void>) (photo, throwable) -> {
                    mUIExecutor.runOnUiThread(() -> {
                        trackCameraScreenEvent(CameraScreenEvent.TAKE_PICTURE);
                        onPictureTaken(photo, throwable);
                    });
                    return null;
                });
    }

    // Only the formats that go through analyzeQRCode raise the Gini ingredient brand element,
    // because that is where the preview is dimmed and interaction is disabled. EPS_PAYMENT and the
    // unknown-format default deliberately do not: EPS has no analysis step at all (it builds the
    // extraction locally and goes straight to onQrCodeRecognized), so per R12 there is no analysis
    // state to brand, and an unknown format is only logged. Before the badge had a single owner it
    // flashed for one frame on the EPS path via QRCodePopup.progressViews(); that was incidental,
    // not the requirement — please do not "restore" it.
    private void handlePaymentQRCodeData(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        switch (paymentQRCodeData.getFormat()) {
            case EPC069_12:
            case BEZAHL_CODE:
            case GINI_PAYMENT:
            case SPC:
            case SPD:
            case PAY_BY_SQUARE:
            case UPNQR:
            case HUB3:
                QRCodeDocument mQRCodeDocument = QRCodeDocument.fromPaymentQRCodeData(
                        paymentQRCodeData);
                sendQRCodeScannedEventToUserAnalytics(true);
                analyzeQRCode(mQRCodeDocument);
                break;
            case EPS_PAYMENT:
                sendQRCodeScannedEventToUserAnalytics(true);
                handleEPSPaymentQRCode(paymentQRCodeData);
                break;
            default:
                sendQRCodeScannedEventToUserAnalytics(false);
                LOG.error("Unknown payment QR Code format: {}", LogSanitizer.sanitize(paymentQRCodeData));
                break;
        }
    }

    private void sendQRCodeScannedEventToUserAnalytics(boolean validQRCode) {
        if (shouldSendUserAnalyticsTrackerForQrCodes) {
            if (mUserAnalyticsEventTracker != null) {
                mUserAnalyticsEventTracker.trackEvent(
                        UserAnalyticsEvent.QR_CODE_SCANNED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                                add(new UserAnalyticsEventProperty.QrCodeValid(validQRCode));
                            }
                        }
                );
            }
            shouldSendUserAnalyticsTrackerForQrCodes = false;
        }
    }

    private void handleEPSPaymentQRCode(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        final GiniCaptureExtraction extraction = new GiniCaptureExtraction(
                paymentQRCodeData.getUnparsedContent(), EXTRACTION_ENTITY_NAME,
                null);
        final GiniCaptureSpecificExtraction specificExtraction = new GiniCaptureSpecificExtraction(
                EXTRACTION_ENTITY_NAME,
                paymentQRCodeData.getUnparsedContent(),
                EXTRACTION_ENTITY_NAME,
                null,
                Collections.singletonList(extraction)
        );
        onQrCodeRecognized(Collections.singletonMap(EXTRACTION_ENTITY_NAME, specificExtraction));
    }

    private void updateCameraFlashState() {
        mCameraController.setFlashEnabled(mIsFlashEnabled);
        updateFlashButtonImageAndSubtitle();
    }

    private void updateFlashButtonImageAndSubtitle() {
        final int flashIconRes = mIsFlashEnabled ? R.drawable.gc_camera_flash_on
                : R.drawable.gc_camera_flash_off;
        mButtonCameraFlash.setImageResource(flashIconRes);
        final int flashSubtitleRes = mIsFlashEnabled ? R.string.gc_camera_flash_on_subtitle
                : R.string.gc_camera_flash_off_subtitle;
        mCameraFlashButtonSubtitle.setText(flashSubtitleRes);

        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }

        final int flashButtonContentDescription = mIsFlashEnabled ? R.string.gc_turn_flash_off_content_description : R.string.gc_turn_flash_on_content_description;
        mButtonCameraFlashTrigger.setContentDescription(activity.getString(flashButtonContentDescription));
    }

    @VisibleForTesting
    void analyzeQRCode(final QRCodeDocument qrCodeDocument) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager =
                    GiniCapture.getInstance().internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                showActivityIndicatorAndDisableInteraction();
                // This is the moment the invoice-retrieval half actually covers the preview: the
                // dim is up AND the shutter is disabled. Showing the brand element any earlier
                // (e.g. when the QR-code popup is shown) puts a screenReaderFocusable badge over a
                // still-usable shutter for the popup's ~1s delay, which violates R13 — on phone
                // portrait the badge overlaps the trigger's lower edge and wins explore-by-touch.
                // This is exactly where QRCodePopup.progressViews() used to raise it.
                // On the education half this runs too (the education flow calls back into
                // handlePaymentQRCodeData), where it is a harmless idempotent re-show: that half
                // already raised the badge together with its own full-screen overlay.
                if (isIngredientBrandVisible()) {
                    setPoweredByGiniVisible(true);
                }
                networkRequestsManager
                        .upload(activity, qrCodeDocument)
                        .handle((requestResult, throwable) -> {
                            if (throwable != null) {
                                hideActivityIndicatorAndEnableInteraction();
                                if (!isCancellation(throwable)) {
                                    handleAnalysisError(throwable, qrCodeDocument);
                                }
                            }
                            return requestResult;
                        })
                        .thenCompose(
                                requestResult -> {
                                    if (requestResult != null) {
                                        final GiniCaptureMultiPageDocument multiPageDocument =
                                                DocumentFactory.newMultiPageDocument(
                                                        qrCodeDocument);
                                        return networkRequestsManager.analyze(
                                                multiPageDocument);
                                    }
                                    return CompletableFuture.completedFuture(null);
                                })
                        .handle((CompletableFuture.BiFun<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>, Throwable, Void>) (requestResult, throwable) -> {
                            hideActivityIndicatorAndEnableInteraction();
                            if (throwable != null
                                    && !isCancellation(throwable)) {
                                handleAnalysisError(throwable, qrCodeDocument);
                            } else if (requestResult != null) {
                                mPaymentQRCodePopup.hide();
                                // hideActivityIndicatorAndEnableInteraction() above has already
                                // removed the dim and re-enabled the shutter, and hide() restores
                                // the live preview — so on the retrieval half the surface the badge
                                // belongs to is gone and it must go with it (R13). Navigation is
                                // asynchronous (onQrCodeRecognized hops to another dispatcher and
                                // waits on the education mutex), so "onStop will get it" is not
                                // soon enough.
                                // Not on the education half: there the education overlay is still
                                // covering the preview at this point — it is only ever taken down
                                // by navigation — so hiding here would strip the branding off the
                                // education content, which is the defect this ownership fix closes.
                                if (!isQrEducationStepRunning()) {
                                    setPoweredByGiniVisible(false);
                                }
                                if (requestResult.getAnalysisResult().getExtractions().isEmpty()) {
                                    //mListener.noExtractionsFromQRCode(qrCodeDocument);
                                    // The whole QR-code analysis step ends here without a result
                                    // and we navigate away, so the brand element comes down for
                                    // *both* halves — unconditionally, unlike the guarded hide
                                    // above. A no-op for the retrieval half, which already hid it.
                                    setPoweredByGiniVisible(false);
                                    NoResultsFragment.navigateToNoResultsFragment(mFragment.findNavController(), CameraFragmentDirections.toNoResultsFragment(qrCodeDocument));
                                    return null;
                                }
                                // Nothing else touches the badge here: the retrieval half already
                                // hid it above, and on the education half it must stay up until
                                // the navigation triggered below stops this fragment (onStop).
                                // onQrCodeRecognized runs on Dispatchers.IO and must not touch
                                // views at all.
                                onQrCodeRecognized(requestResult.getAnalysisResult().getExtractions());
                            }
                            return null;
                        });
            }
        }
    }

    private void handleAnalysisError(Throwable throwable, Document document) {

        if (mFragment.getActivity() == null)
            return;

        // A failed analysis ends the QR-code analysis step, so the brand element comes down before
        // we navigate away — otherwise it would still be visible when the user returns here.
        setPoweredByGiniVisible(false);

        final FailureException failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable);
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR);
        if (failureException != null) {
            ErrorFragment.Companion.navigateToErrorFragment(
                    mFragment.findNavController(),
                    CameraFragmentDirections.toErrorFragment(failureException.getErrorType(), document)
            );
        } else {
            ErrorFragment.Companion.navigateToErrorFragment(
                    mFragment.findNavController(),
                    CameraFragmentDirections.toErrorFragment(ErrorType.GENERAL, document)
            );
        }
    }

    private void showFileChooser() {
        LOG.info("Importing document");
        if (exceedsMultiPageLimit()) {
            showMultiPageLimitError();
            return;
        }
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final DocumentImportEnabledFileTypes enabledFileTypes;
        if (mInMultiPageState) {
            enabledFileTypes = DocumentImportEnabledFileTypes.IMAGES;
        } else {
            enabledFileTypes = getDocumentImportEnabledFileTypes();
        }
        // Make sure we are still at the camera fragment destination. Rarely, but it can happen that the user clicks
        // the "files" button twice very fast and the second click happens after the destination is already at the
        // file chooser fragment.
        if (isAtCameraFragmentDestination()) {
            mFragment.findNavController().navigate(CameraFragmentDirections.toFileChooserFragment(enabledFileTypes));
        }
    }

    private boolean isAtCameraFragmentDestination() {
        final NavDestination currentDestination = mFragment.findNavController().getCurrentDestination();
        if (currentDestination == null) {
            return false;
        }
        return currentDestination.getId() == R.id.gc_destination_camera_fragment;
    }

    private void importDocumentFromIntent(@NonNull final Intent data) {
        final Activity activity = mFragment
                .getActivity();
        if (activity == null) {
            return;
        }
        if (IntentHelper.hasMultipleUris(data)) {
            final List<Uri> uris = IntentHelper.getUris(data);
            if (uris == null) {
                LOG.error("Document import failed: Intent has no Uris");
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
                return;
            }
            handleMultiPageDocumentAndCallListener(activity, data, uris);
        } else {
            final Uri uri = IntentHelper.getUri(data);
            if (uri == null) {
                LOG.error("Document import failed: Intent has no Uri");
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
                return;
            }
            if (!UriHelper.isUriInputStreamAvailable(uri, activity)) {
                LOG.error("Document import failed: InputStream not available for the Uri");
                showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
                return;
            }

            if (isImage(data, activity)) {
                handleMultiPageDocumentAndCallListener(activity, data,
                        Collections.singletonList(uri));
            } else {
                final int fileSizeLimit;
                if (GiniCapture.hasInstance()) {
                    fileSizeLimit = GiniCapture.getInstance().getImportedFileSizeBytesLimit();
                } else {
                    fileSizeLimit = FILE_SIZE_LIMIT;
                }
                final FileImportValidator fileImportValidator = new FileImportValidator(activity, fileSizeLimit);
                if (fileImportValidator.matchesCriteria(data, uri)) {
                    createSinglePageDocumentAndCallListener(data, activity);
                } else {
                    final FileImportValidator.Error error = fileImportValidator.getError();
                    if (error != null) {
                        Error errorClass = new Error(error);
                        ErrorType errorType = ErrorType.typeFromError(errorClass, getGetEInvoiceFeatureEnabledUseCase().invoke());
                        showGenericInvalidFileError(errorType);
                    }
                }
            }
        }
    }

    private void importDocumentFromUriList(List<Uri> uriList) {
        if (mFragment.getActivity() == null)
            return;

        handleMultiPageDocumentAndCallListener(mFragment.getActivity(), new Intent(Intent.ACTION_PICK), uriList);
    }

    private boolean isImage(@NonNull final Intent data, @NonNull final Activity activity) {
        return IntentHelper.hasMimeTypeWithPrefix(data, activity, MimeType.IMAGE_PREFIX.asString());
    }

    private void createSinglePageDocumentAndCallListener(final Intent data,
                                                         final Activity activity) {
        try {
            final GiniCaptureDocument document = DocumentFactory.newDocumentFromIntent(data,
                    activity,
                    DeviceHelper.getDeviceOrientation(activity),
                    DeviceHelper.getDeviceType(activity),
                    ImportMethod.PICKER);
            LOG.info("Document imported: {}", LogSanitizer.sanitize(document));
            requestClientDocumentCheck(document);
        } catch (final IllegalArgumentException e) {
            LOG.error("Failed to import selected document", e);
            showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
        }
    }

    private void requestClientDocumentCheck(final GiniCaptureDocument document) {
        showActivityIndicatorAndDisableInteraction();
        LOG.debug("Requesting document check from client");
        fragmentListener.onCheckImportedDocument(document,
                new CameraFragmentListener.DocumentCheckResultCallback() {
                    @Override
                    public void documentAccepted() {
                        LOG.debug("Client accepted the document");
                        hideActivityIndicatorAndEnableInteraction();
                        if (document.getType() == Document.Type.IMAGE_MULTI_PAGE) {
                            final ImageMultiPageDocument multiPageDocument =
                                    (ImageMultiPageDocument) document;
                            addToMultiPageDocumentMemoryStore(multiPageDocument);
                            proceedToMultiPageReviewScreen(true);
                        } else {
                            if (document.isReviewable()) {
                                if (document.getType() == Document.Type.IMAGE &&
                                        document instanceof ImageDocument) {
                                    final ImageMultiPageDocument multiPageDocument = new ImageMultiPageDocument(
                                            document.getSource(), document.getImportMethod());
                                    addToMultiPageDocumentMemoryStore(multiPageDocument);
                                    multiPageDocument.addDocument(((ImageDocument) document));
                                    proceedToMultiPageReviewScreen(true);
                                }
                            } else {
                                mFragment.findNavController().navigate(CameraFragmentDirections.toAnalysisFragment(document, ""));
                            }
                        }
                    }

                    @Override
                    public void documentRejected(@NonNull final String messageForUser) {
                        LOG.debug("Client rejected the document: {}", LogSanitizer.sanitize(messageForUser));

                        hideActivityIndicatorAndEnableInteraction();

                        if (mFragment.getActivity() == null)
                            return;

                        showInvalidFileAlert(messageForUser);
                    }
                });
    }

    private void proceedToMultiPageReviewScreen(final boolean shouldScrollToLastPage) {
        if (mFragment.getActivity() == null) {
            return;
        }
        if (addPages) {
            final Bundle resultBundle = new Bundle();
            resultBundle.putBoolean(RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE, shouldScrollToLastPage);
            mFragment.getParentFragmentManager().setFragmentResult(REQUEST_KEY, resultBundle);
            mFragment.findNavController().popBackStack();
        } else {
            mFragment.safeNavigate(CameraFragmentDirections.toReviewFragment(shouldScrollToLastPage));
        }
    }

    private void addToMultiPageDocumentMemoryStore(final ImageMultiPageDocument multiPageDocument) {
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal()
                    .getImageMultiPageDocumentMemoryStore()
                    .setMultiPageDocument(multiPageDocument);
        }
    }

    private void handleMultiPageDocumentAndCallListener(@NonNull final Context context,
                                                        @NonNull final Intent intent, @NonNull final List<Uri> uris) {
        showActivityIndicatorAndDisableInteraction();
        if (mImportUrisAsyncTask != null) {
            mImportUrisAsyncTask.cancel(true);
        }
        if (!GiniCapture.hasInstance()) {
            LOG.error(
                    "Cannot import multi-page document. GiniCapture instance not available. Create it with GiniCapture.newInstance().");
            return;
        }
        if (exceedsMultiPageLimit()) {
            hideActivityIndicatorAndEnableInteraction();
            showMultiPageLimitError();
            return;
        }


        mImportUrisAsyncTask = new ImportImageFileUrisAsyncTask(
                context, intent, GiniCapture.getInstance(),
                Document.Source.newExternalSource(), ImportMethod.PICKER,
                new AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException>() {
                    @Override
                    public void onSuccess(final ImageMultiPageDocument multiPageDocument) {
                        hideActivityIndicatorAndEnableInteraction();
                        if (mMultiPageDocument == null) {
                            mInMultiPageState = true;
                            mMultiPageDocument = multiPageDocument;
                        } else {
                            mMultiPageDocument.addDocuments(multiPageDocument.getDocuments());
                        }
                        if (mMultiPageDocument.getDocuments().isEmpty()) {
                            LOG.error("Document import failed: Intent did not contain images");
                            showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
                            mMultiPageDocument = null; // NOPMD
                            mInMultiPageState = false;
                            return;
                        }
                        LOG.info("Document imported: {}", LogSanitizer.sanitize(mMultiPageDocument));
                        updatePhotoThumbnail();
                        requestClientDocumentCheck(mMultiPageDocument);
                    }

                    @Override
                    public void onError(final ImportedFileValidationException exception) {
                        LOG.error("Document import failed", exception);
                        hideActivityIndicatorAndEnableInteraction();
                        final FileImportValidator.Error error = exception.getValidationError();
                        if (error != null && mFragment.getActivity() != null) {
                            Error errorClass = new Error(error);
                            ErrorType errorType = ErrorType.typeFromError(errorClass, getGetEInvoiceFeatureEnabledUseCase().invoke());
                            showGenericInvalidFileError(errorType);
                        }
                    }

                    @Override
                    public void onCancelled() {
                        // No-op
                    }
                });
        mImportUrisAsyncTask.execute(uris.toArray(new Uri[uris.size()]));
    }

    private boolean exceedsMultiPageLimit() {
        return mInMultiPageState && mMultiPageDocument.getDocuments().size()
                >= FileImportValidator.DOCUMENT_PAGE_LIMIT;
    }

    public void showActivityIndicatorAndDisableInteraction() {
        if (mLoadingIndicator.getInjectedViewAdapterHolder() == null
                || mActivityIndicatorBackground == null) {
            return;
        }
        mActivityIndicatorBackground.setVisibility(View.VISIBLE);
        mActivityIndicatorBackground.setClickable(true);
        mLoadingIndicator.modifyAdapterIfOwned(adapter -> {
            adapter.onVisible();
            return Unit.INSTANCE;
        });
        disableInteraction();
    }

    public void hideActivityIndicatorAndEnableInteraction() {
        if (mLoadingIndicator.getInjectedViewAdapterHolder() == null
                || mActivityIndicatorBackground == null) {
            return;
        }
        mActivityIndicatorBackground.setVisibility(View.INVISIBLE);
        mActivityIndicatorBackground.setClickable(false);
        mLoadingIndicator.modifyAdapterIfOwned(adapter -> {
            adapter.onHidden();
            return Unit.INSTANCE;
        });
        enableInteraction();
    }

    private void updatePhotoThumbnail() {
        if (!GiniCapture.hasInstance()) {
            LOG.error(
                    "Cannot show photo thumbnail. GiniCapture instance not available. Create it with GiniCapture.newInstance().");
        }
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }

        final List<ImageDocument> documents = mMultiPageDocument.getDocuments();
        if (!documents.isEmpty()) {
            mPhotoThumbnail.removeImage();
        }
        final ImageDocument lastDocument = documents.get(documents.size() - 1);
        GiniCapture.getInstance().internal().getPhotoMemoryCache()
                .get(activity, lastDocument, new AsyncCallback<Photo, Exception>() { // NOPMD
                    @Override
                    public void onSuccess(final Photo result) {
                        mPhotoThumbnail.setImage(
                                new PhotoThumbnail.ThumbnailBitmap(result.getBitmapPreview(),
                                        lastDocument.getRotationForDisplay()));
                        mPhotoThumbnail.setImageCount(documents.size());
                        mPhotoThumbnail.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(final Exception exception) {
                        mPhotoThumbnail.setImage(null);
                        mPhotoThumbnail.setImageCount(documents.size());
                        if (!documents.isEmpty()) mPhotoThumbnail.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled() {
                        // Not used
                    }
                });
    }

    private void enableInteraction() {
        if (mCameraPreview == null
                || mButtonImportDocument == null
                || mButtonCameraFlashTrigger == null
                || mPhotoThumbnail == null
                || mButtonCameraTrigger == null) {
            return;
        }
        mCameraPreview.setEnabled(true);
        mButtonImportDocument.setEnabled(true);
        mButtonCameraFlashTrigger.setEnabled(true);
        mPhotoThumbnail.setEnabled(true);
        mButtonCameraTrigger.setEnabled(true);
    }

    private void disableInteraction() {
        if (mCameraPreview == null
                || mButtonImportDocument == null
                || mButtonCameraFlashTrigger == null
                || mPhotoThumbnail == null
                || mButtonCameraTrigger == null) {
            return;
        }
        mCameraPreview.setEnabled(false);
        mButtonImportDocument.setEnabled(false);
        mButtonCameraFlashTrigger.setEnabled(false);
        mPhotoThumbnail.setEnabled(false);
        mButtonCameraTrigger.setEnabled(false);
    }

    private void showGenericInvalidFileError(ErrorType errorType) {
        String errorMessage = mFragment.getActivity().getResources()
                .getString(errorType.getTitleTextResource());
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(
                    UserAnalyticsEvent.ERROR_DIALOG_SHOWN,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                            add(new UserAnalyticsEventProperty.ErrorMessage(errorMessage));
                        }
                    }
            );
        }
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        String message = activity.getString(errorType.getTitleTextResource());
        LOG.error("Invalid document {}", LogSanitizer.sanitize(message));
        showInvalidFileAlert(message);
    }

    private void showInvalidFileAlert(final String message) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        currentGenericErrorMessage = message;
        isGenericErrorShowing = true;
        genericErrorType = ERROR_TYPE_INVALID_FILE;
        mFragment.showAlertDialog(message,
                activity.getString(R.string.gc_document_import_close_error),
                (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    resetGenericDialogState();
                },
                null,
                null,
                (dialogInterface -> {
                    resetGenericDialogState();
                }));
    }

    private void resetGenericDialogState() {
        currentGenericErrorMessage = "";
        isGenericErrorShowing = false;
        genericErrorType = "";
    }

    @UiThread
    private void onPictureTaken(final Photo photo, final Throwable throwable) {
        if (throwable != null) {
            handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED, "Failed to take picture",
                    throwable);
            mCameraController.startPreview();
            setmIsTakingPicture(false);
        } else {
            if (photo != null) {
                LOG.info("Picture taken");
                getUpdateFlowTypeUseCase().execute(FlowType.Photo.INSTANCE);
                showActivityIndicatorAndDisableInteraction();
                photo.edit()
                        .crop(mCameraPreview, getRectForCroppingFromImageFrame())
                        .compressByDefault().applyAsync(new PhotoEdit.PhotoEditCallback() {
                            @Override
                            public void onDone(@NonNull final Photo result) {
                                hideActivityIndicatorAndEnableInteraction();
                                if (mInMultiPageState) {
                                    final ImageDocument document = createSavedDocument(result);
                                    if (document == null) {
                                        handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                                "Failed to take picture: could not save picture to disk",
                                                null);
                                        mCameraController.startPreview();
                                        setmIsTakingPicture(false);
                                        return;
                                    }
                                    mMultiPageDocument.addDocument(document);
                                    mPhotoThumbnail.setImage(new PhotoThumbnail.ThumbnailBitmap(result.getBitmapPreview(),
                                            document.getRotationForDisplay()));
                                    mPhotoThumbnail.setImageCount(mMultiPageDocument.getDocuments().size());
                                    proceedToMultiPageReviewScreen(true);
                                } else {
                                    if (isMultiPageEnabled()) {
                                        final ImageDocument document = createSavedDocument(result);
                                        if (document == null) {
                                            handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                                    "Failed to take picture: could not save picture to disk",
                                                    null);
                                            mCameraController.startPreview();
                                            setmIsTakingPicture(false);
                                            return;
                                        }
                                        mInMultiPageState = true;
                                        mMultiPageDocument = new ImageMultiPageDocument(
                                                Document.Source.newCameraSource(), ImportMethod.NONE);
                                        GiniCapture.getInstance().internal()
                                                .getImageMultiPageDocumentMemoryStore()
                                                .setMultiPageDocument(mMultiPageDocument);
                                        mMultiPageDocument.addDocument(document);
                                        mPhotoThumbnail.setImage(
                                                new PhotoThumbnail.ThumbnailBitmap(result.getBitmapPreview(),
                                                        document.getRotationForDisplay()));
                                        mPhotoThumbnail.setImageCount(mMultiPageDocument.getDocuments().size());
                                        proceedToMultiPageReviewScreen(true);
                                        setmIsTakingPicture(false);
                                    } else {
                                        final ImageDocument document = createSavedDocument(result);
                                        if (document == null) {
                                            handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                                    "Failed to take picture: could not save picture to disk",
                                                    null);
                                            mCameraController.startPreview();
                                            setmIsTakingPicture(false);
                                            return;
                                        }
                                        final ImageMultiPageDocument multiPageDocument = new ImageMultiPageDocument(
                                                Document.Source.newCameraSource(), ImportMethod.NONE);
                                        GiniCapture.getInstance().internal()
                                                .getImageMultiPageDocumentMemoryStore()
                                                .setMultiPageDocument(multiPageDocument);
                                        multiPageDocument.addDocument(document);
                                        proceedToMultiPageReviewScreen(false);
                                        setmIsTakingPicture(false);
                                    }
                                    mCameraController.startPreview();
                                }
                            }

                            @Override
                            public void onFailed() {
                                hideActivityIndicatorAndEnableInteraction();
                                handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                        "Failed to take picture: picture compression failed", null);
                                mCameraController.startPreview();
                                setmIsTakingPicture(false);
                            }
                        });
            } else {
                handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                        "Failed to take picture: no picture from the camera", null);
                mCameraController.startPreview();
                setmIsTakingPicture(false);
            }
        }
    }

    private Rect getRectForCroppingFromImageFrame() {
        final Rect frameHitRect = new Rect();
        mImageFrame.getHitRect(frameHitRect);

        final Rect cameraPreviewHitRect = new Rect();
        mCameraPreview.getHitRect(cameraPreviewHitRect);

        // The camera preview can be wider or taller than the screen
        // and we need the frame rect relative to the camera preview's origin
        frameHitRect.offset(-cameraPreviewHitRect.left, -cameraPreviewHitRect.top);

        return frameHitRect;
    }


    private void showMultiPageLimitError() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        isGenericErrorShowing = true;
        genericErrorType = ERROR_TYPE_MULTI_PAGE;
        mFragment.showAlertDialog(activity.getString(R.string.gc_document_error_too_many_pages),
                activity.getString(R.string.gc_document_error_multi_page_limit_review_pages_button),
                (dialogInterface, i) -> {
                    proceedToMultiPageReviewScreen(true);
                    dialogInterface.dismiss();
                    resetGenericDialogState();
                }, activity.getString(R.string.gc_document_error_multi_page_limit_cancel_button),
                (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    resetGenericDialogState();
                }, (dialogInterface) -> {
                    dialogInterface.dismiss();
                    resetGenericDialogState();
                });
    }

    @Nullable
    private ImageDocument createSavedDocument(@NonNull final Photo photo) {
        if (!GiniCapture.hasInstance()) {
            LOG.error(
                    "Cannot save document. GiniCapture instance not available. Create it with GiniCapture.newInstance().");
        }
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return null;
        }
        final ImageDiskStore imageDiskStore =
                GiniCapture.getInstance().internal().getImageDiskStore();
        final Uri savedAtUri = imageDiskStore.save(activity, photo.getData());
        return DocumentFactory.newImageDocumentFromPhoto(photo, savedAtUri);
    }

    private void showDocumentCornerGuidesAnimated() {
        mImageCorners.animate().alpha(1.0f);
    }

    private void hideDocumentCornerGuidesAnimated() {
        mImageCorners.animate().alpha(0.0f);
    }

    private void showCameraTriggerButtonAnimated() {
        enableCameraTriggerButtonAnimated();
    }

    private void hideCameraTriggerButtonAnimated() {
        disableCameraTriggerButtonAnimated(0.0f);
    }

    private void disableCameraTriggerButtonAnimated(final float alpha) {
        mButtonCameraTrigger.clearAnimation();
        mButtonCameraTrigger.animate().alpha(alpha).start();
        mButtonCameraTrigger.setEnabled(false);
    }

    private void enableCameraTriggerButtonAnimated() {
        mButtonCameraTrigger.clearAnimation();
        mButtonCameraTrigger.animate().alpha(1.0f).start();
        mButtonCameraTrigger.setEnabled(true);
    }

    private void showInterfaceAnimated() {
        showCameraTriggerButtonAnimated();
        showDocumentCornerGuidesAnimated();
        showPhotoThumbnailAnimated();
        showFlashButtonAnimated();
        if (mImportDocumentButtonEnabled) {
            showImportDocumentButtonAnimated();
        }
        showPaneAnimated();
    }

    private void showPhotoThumbnailAnimated() {
        mPhotoThumbnail.animate().alpha(1.0f).start();
    }

    private void showImportDocumentButtonAnimated() {
        mButtonImportDocumentWrapper.animate().alpha(1.0f);
        mButtonImportDocument.setEnabled(true);
    }

    private void showFlashButtonAnimated() {
        mButtonCameraFlashWrapper.animate().alpha(1.0f);
        mButtonCameraFlashTrigger.setEnabled(true);
    }

    private void showPaneAnimated() {
        mPaneWrapper.animate().alpha(1.0f);
    }

    private void hideInterfaceAnimated() {
        hideCameraTriggerButtonAnimated();
        hideDocumentCornerGuidesAnimated();
        hidePhotoThumbnailAnimated();
        if (mImportDocumentButtonEnabled) {
            hideImportDocumentButtonAnimated();
        }
        hideFlashButtonAnimated();
        hidePaneAnimated();
    }

    private void hidePhotoThumbnailAnimated() {
        mPhotoThumbnail.animate().alpha(0.0f).start();
    }

    private void hideImportDocumentButtonAnimated() {
        mButtonImportDocumentWrapper.animate().alpha(0.0f);
        mButtonImportDocument.setEnabled(false);
    }

    private void hidePaneAnimated() {
        mPaneWrapper.animate().alpha(0.0f);
    }

    private void showNoPermissionView() {
        hideCameraPreviewAnimated();
        hideInterfaceAnimated();
        inflateNoPermissionStub();
        setUpNoPermissionButton();
        if (mLayoutNoPermission != null) {
            trackCameraAccessPermissionRequiredShownEvent();
            mLayoutNoPermission.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNoPermissionViewVisible() {
        return mLayoutNoPermission != null
                && mLayoutNoPermission.getVisibility() == View.VISIBLE;
    }

    private void inflateNoPermissionStub() {
        if (mLayoutNoPermission == null) {
            LOG.debug("Inflating no permission view");
            mLayoutNoPermission = (ConstraintLayout) mViewStubInflater.inflate();
        }
    }

    private void hideNoPermissionView() {
        showCameraPreviewAnimated();
        if (!mInterfaceHidden) {
            showInterfaceAnimated();
        }
        if (mLayoutNoPermission != null) {
            mLayoutNoPermission.setVisibility(View.GONE);
        }
    }

    private void setUpNoPermissionButton() {
        if (isMarshmallowOrLater()) {
            handleNoPermissionButtonClick();
        } else {
            hideNoPermissionButton();
        }
    }

    private void hideCameraPreviewAnimated() {
        mCameraPreview.animate().alpha(0.0f);
        mCameraPreview.setEnabled(false);
    }

    private void showCameraPreviewAnimated() {
        mCameraPreview.animate().alpha(1.0f);
        mCameraPreview.setEnabled(true);
    }

    private void handleNoPermissionButtonClick() {
        final View view = mFragment.getView();
        if (view == null) {
            return;
        }
        final Button button = view.findViewById(R.id.gc_button_camera_no_permission);
        button.setOnClickListener(v -> {
            trackCameraAccessPermissionRequiredGetAccessClickedEvent();
            startApplicationDetailsSettings();
        });
    }

    private void hideNoPermissionButton() {
        final View view = mFragment.getView();
        if (view == null) {
            return;
        }
        final Button button = (Button) view.findViewById(R.id.gc_button_camera_no_permission);
        button.setVisibility(View.GONE);
    }

    private void hideFlashButtonAnimated() {
        mButtonCameraFlashWrapper.animate().alpha(0.0f);
        mButtonCameraFlashTrigger.setEnabled(false);
    }

    private void startApplicationDetailsSettings() {
        LOG.debug("Starting Application Details");
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        ApplicationHelper.startApplicationDetailsSettings(activity);
    }

    private void setQRDisabledTexts() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }

        if (ContextHelper.isTablet(mFragment.getActivity())) {
            if (isOnlyQRCodeScanningEnabled()) {
                // TODO: Decide how to properly handle this case when only qr code scanning is enabled
                topAdapterInjectedViewContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                    injectedViewAdapter.setTitle(activity.getString(R.string.gc_title_camera));
                    return Unit.INSTANCE;
                });
            } else {
                topAdapterInjectedViewContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                    injectedViewAdapter.setTitle(activity.getString(R.string.gc_camera_info_label_only_invoice));
                    return Unit.INSTANCE;
                });
            }
        } else {
            if (!isOnlyQRCodeScanningEnabled()) {
                mScanTextView.setText(mFragment.getActivity().getResources().getString(R.string.gc_camera_info_label_only_invoice));
                // In landscape the below-frame hint is hidden and the scan hint lives in the top bar,
                // so update it live (a new holder alone only reconfigures on the next bind/rotation).
                if (!ContextHelper.isPortraitOrientation(activity)) {
                    topAdapterInjectedViewContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                        injectedViewAdapter.setTitle(activity.getString(R.string.gc_camera_info_label_only_invoice));
                        return Unit.INSTANCE;
                    });
                }
            }
        }
    }

    @VisibleForTesting
    void initCameraController(final Activity activity) {
        if (mCameraController == null) {
            LOG.debug("CameraController created");
            mCameraController = createCameraController(activity);
        }
        mCameraController.setPreviewCallback(new CameraInterface.PreviewCallback() {
            @Override
            public void onPreviewFrame(@NonNull Image image, @NonNull Size imageSize, int rotation, @NonNull CameraInterface.PreviewFrameCallback previewFrameCallback) {
                AtomicInteger previewFrameReferenceCount = new AtomicInteger();
                if (ibanRecognizerFilter != null) {
                    previewFrameReferenceCount.getAndIncrement();
                }
                if (mPaymentQRCodeReader != null) {
                    previewFrameReferenceCount.getAndIncrement();
                }

                if (ibanRecognizerFilter != null) {
                    try {
                        if (cropToCameraFrameTextRecognizer != null) {
                            cropToCameraFrameTextRecognizer.setCameraPreviewSize(new Size(mCameraPreview.getWidth(), mCameraPreview.getHeight()));
                            cropToCameraFrameTextRecognizer.setImageSizeAndRotation(imageSize, rotation);
                            cropToCameraFrameTextRecognizer.setCameraFrameRect(getRectForCroppingFromImageFrame());
                        }

                        ibanRecognizerFilter.processImage(image, imageSize.width, imageSize.height, rotation, () -> {
                            previewFrameReferenceCount.getAndDecrement();
                            if (previewFrameReferenceCount.get() == 0) {
                                previewFrameCallback.onReleaseFrame();
                            }
                        });
                    } catch (Exception e) {
                        LOG.error("Failed to process image for IBAN recognition", e);
                        previewFrameReferenceCount.getAndDecrement();
                        if (previewFrameReferenceCount.get() == 0) {
                            previewFrameCallback.onReleaseFrame();
                        }
                    }
                }

                if (mPaymentQRCodeReader != null) {
                    mPaymentQRCodeReader.readFromImage(image, imageSize, rotation, () -> {
                        previewFrameReferenceCount.getAndDecrement();
                        if (previewFrameReferenceCount.get() == 0) {
                            previewFrameCallback.onReleaseFrame();
                        }
                    });
                }
            }

            @Override
            public void onPreviewFrame(@NonNull byte[] image, @NonNull Size imageSize, int rotation) {
                if (mPaymentQRCodeReader != null) {
                    mPaymentQRCodeReader.readFromByteArray(image, imageSize, rotation);
                }
                if (ibanRecognizerFilter != null) {
                    try {
                        ibanRecognizerFilter.processByteArray(image, imageSize.width, imageSize.height, rotation, () -> {
                        });
                    } catch (Exception e) {
                        LOG.error("Failed to process image for IBAN recognition", e);
                    }
                }
            }
        });
    }

    @VisibleForTesting
    void handleIBANsDetected(List<String> ibans) {
        // mIsUnsupportedQRDialogShowing: the filter is released while the unsupported QR code
        // dialog is visible, but an in-flight recognition result may still arrive after the
        // dialog is shown — it must not dismiss a dialog that awaits an explicit user choice.
        if (!ibans.isEmpty() && !isPaymentQRCodeDetectionInProgress() && !mIsUnsupportedQRDialogShowing) {
            mUnsupportedQRCodePopup.hide();
            showIBANsDetectedOnScreen(ibans);
        } else {
            hideIBANsDetectedOnScreen();
        }
    }

    private void showIBANsDetectedOnScreen(List<String> ibans) {
        isIbanDetectedOnceForUserAnalytics = true;
        mIbanDetectedTextView.setVisibility(View.VISIBLE);
        mImageFrame.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(
                                mFragment.getActivity(),
                                R.color.gc_success_05
                        )
                )
        );
        if (ibans.size() == 1) {
            mIbanDetectedTextView.setText(String.format("%s%s", ibans.get(0), mFragment.getActivity().getString(R.string.gc_iban_detected_please_take_picture)));
        } else {
            mIbanDetectedTextView.setText(String.format("%s%s", mFragment.getActivity().getString(R.string.gc_iban_detected), mFragment.getActivity().getString(R.string.gc_iban_detected_please_take_picture)));
        }
    }

    private void hideIBANsDetectedOnScreen() {
        // Don't reset the frame color while a QR code popup owns it (e.g. the red frame of the
        // unsupported QR code warning restored after a configuration change), otherwise the first
        // no-IBAN camera frame would overwrite it with the default color.
        if (!mUnsupportedQRCodePopup.isShown() && !isPaymentQRCodeDetectionInProgress()) {
            mImageFrame.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    mFragment.getActivity(),
                                    R.color.gc_light_01
                            )
                    )
            );
        }
        mIbanDetectedTextView.setVisibility(View.GONE);
        mIbanDetectedTextView.setText("");
    }

    @NonNull
    protected CameraInterface createCameraController(final Activity activity) {
        return new CameraXController(activity);
    }

    private void handleError(final GiniCaptureError.ErrorCode errorCode,
                             @NonNull final String message,
                             @Nullable final Throwable throwable) {
        ErrorLogger.log(new ErrorLog(errorCode.toString() + ": " + message, throwable));
        String errorMessage = message;
        if (throwable != null) {
            LOG.error("{}", LogSanitizer.sanitize(message), throwable);
            // Add error info to the message to help clients, if they don't have logging enabled
            errorMessage = errorMessage + ": " + throwable.getMessage();
        } else {
            LOG.error("{}", LogSanitizer.sanitize(message));
        }
        fragmentListener.onError(new GiniCaptureError(errorCode, errorMessage));
    }

    private void onBackPressed() {
        boolean popSuccess = mFragment.findNavController().popBackStack();
        if (!popSuccess) {
            mCancelListener.onCancelFlow();
        }
    }

    private void trackCameraScreenCloseTappedEventIfNeeded() {
        if ((mLayoutNoPermission == null || mLayoutNoPermission.getVisibility() != View.VISIBLE) && mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.CLOSE_TAPPED,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                        }
                    });
        }

    }

    private void trackCameraScreenShownEvent() {
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.SCREEN_SHOWN,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                        }
                    });
        }
    }

    private void trackCameraScreenHelpTappedIfNeeded() {
        if ((mLayoutNoPermission == null || mLayoutNoPermission.getVisibility() != View.VISIBLE) && mUserAnalyticsEventTracker != null)
            mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.HELP_TAPPED,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                        }
                    });

    }

    private void trackCameraAccessPermissionRequiredShownEvent() {
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(
                    UserAnalyticsEvent.SCREEN_SHOWN,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(sScreenNamePermission));
                        }
                    }
            );
        }
    }

    private void trackCameraAccessPermissionRequiredGetAccessClickedEvent() {
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(
                    UserAnalyticsEvent.GIVE_ACCESS_TAPPED, new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(sScreenNamePermission));
                        }
                    });
        }
    }

    private void trackCameraAccessPermissionRequiredHelpClickedEventIfNeeded() {
        if (mLayoutNoPermission != null && mLayoutNoPermission.getVisibility() == View.VISIBLE && mUserAnalyticsEventTracker != null)
            mUserAnalyticsEventTracker.trackEvent(
                    UserAnalyticsEvent.HELP_TAPPED, new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(sScreenNamePermission));
                        }
                    });

    }

    private void trackCameraAccessPermissionRequiredCloseClickedEventIfNeeded() {
        if (mLayoutNoPermission != null && mLayoutNoPermission.getVisibility() == View.VISIBLE && mUserAnalyticsEventTracker != null)
            mUserAnalyticsEventTracker.trackEvent(
                    UserAnalyticsEvent.CLOSE_TAPPED, new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(sScreenNamePermission));
                        }
                    });

    }

    @Override
    public void hideImageCorners() {
        hideDocumentCornerGuidesAnimated();
    }
}
