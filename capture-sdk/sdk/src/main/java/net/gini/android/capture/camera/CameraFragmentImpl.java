package net.gini.android.capture.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListenerAdapter;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.ImportedFileValidationException;
import net.gini.android.capture.R;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.document.QRCodeDocument;
import net.gini.android.capture.internal.camera.api.CameraController;
import net.gini.android.capture.internal.camera.api.CameraException;
import net.gini.android.capture.internal.camera.api.CameraInterface;
import net.gini.android.capture.internal.camera.api.camerax.CameraXController;
import net.gini.android.capture.internal.camera.api.UIExecutor;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.camera.photo.PhotoEdit;
import net.gini.android.capture.internal.camera.view.FlashButtonHelper.FlashButtonPosition;
import net.gini.android.capture.internal.camera.view.HintPopup;
import net.gini.android.capture.internal.camera.view.QRCodePopup;
import net.gini.android.capture.internal.fileimport.FileChooserActivity;
import net.gini.android.capture.internal.network.AnalysisNetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData;
import net.gini.android.capture.internal.qrcode.PaymentQRCodeReader;
import net.gini.android.capture.internal.qrcode.QRCodeDetectorTask;
import net.gini.android.capture.internal.qrcode.QRCodeDetectorTaskGoogleVision;
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.internal.ui.ErrorSnackbar;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.ViewStubSafeInflater;
import net.gini.android.capture.internal.util.ApplicationHelper;
import net.gini.android.capture.internal.util.DeviceHelper;
import net.gini.android.capture.internal.util.FileImportValidator;
import net.gini.android.capture.internal.util.MimeType;
import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.network.model.GiniCaptureExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.tracking.CameraScreenEvent;
import net.gini.android.capture.util.IntentHelper;
import net.gini.android.capture.util.UriHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static net.gini.android.capture.GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE;
import static net.gini.android.capture.document.ImageDocument.ImportMethod;
import static net.gini.android.capture.internal.camera.view.FlashButtonHelper.getFlashButtonPosition;
import static net.gini.android.capture.internal.network.NetworkRequestsManager.isCancellation;
import static net.gini.android.capture.internal.qrcode.EPSPaymentParser.EXTRACTION_ENTITY_NAME;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.internal.util.AndroidHelper.isMarshmallowOrLater;
import static net.gini.android.capture.internal.util.ContextHelper.isTablet;
import static net.gini.android.capture.internal.util.FeatureConfiguration.getDocumentImportEnabledFileTypes;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isMultiPageEnabled;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isQRCodeScanningEnabled;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackCameraScreenEvent;

class CameraFragmentImpl implements CameraFragmentInterface, PaymentQRCodeReader.Listener {

    @VisibleForTesting
    static final String GC_SHARED_PREFS = "GC_SHARED_PREFS";
    @VisibleForTesting
    static final int DEFAULT_ANIMATION_DURATION = 200;
    private static final long HIDE_QRCODE_DETECTED_POPUP_DELAY_MS = 10000;
    private static final long DIFFERENT_QRCODE_DETECTED_POPUP_DELAY_MS = 200;
    private static final Logger LOG = LoggerFactory.getLogger(CameraFragmentImpl.class);

    private static final CameraFragmentListener NO_OP_LISTENER = new CameraFragmentListener() {
        @Override
        public void onDocumentAvailable(@NonNull final Document document) {
        }

        @Override
        public void onProceedToMultiPageReviewScreen(
                @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        }

        @Override
        public void onCheckImportedDocument(@NonNull final Document document,
                @NonNull final DocumentCheckResultCallback callback) {
            callback.documentAccepted();
        }

        @Override
        public void onError(@NonNull final GiniCaptureError error) {
        }

        @Override
        public void onExtractionsAvailable(
                @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {

        }
    };

    private static final int REQ_CODE_CHOOSE_FILE = 1;
    @VisibleForTesting
    static final String SHOW_UPLOAD_HINT_POP_UP = "SHOW_HINT_POP_UP";
    @VisibleForTesting
    static final String SHOW_QRCODE_SCANNER_HINT_POP_UP = "SHOW_QR_CODE_SCANNER_HINT_POP_UP";
    private static final String IN_MULTI_PAGE_STATE_KEY = "IN_MULTI_PAGE_STATE_KEY";
    private static final String IS_FLASH_ENABLED_KEY = "IS_FLASH_ENABLED_KEY";

    private final FragmentImplCallback mFragment;

    @VisibleForTesting
    QRCodePopup<PaymentQRCodeData> mPaymentQRCodePopup;
    private QRCodePopup<String> mUnsupportedQRCodePopup;

    private View mImageCorners;
    private ImageStack mImageStack;
    private boolean mInterfaceHidden;
    private boolean mInMultiPageState;
    private boolean mIsFlashEnabled = true;
    private CameraFragmentListener mListener = NO_OP_LISTENER;
    private final UIExecutor mUIExecutor = new UIExecutor();
    private CameraInterface mCameraController;
    private ImageMultiPageDocument mMultiPageDocument;
    private PaymentQRCodeReader mPaymentQRCodeReader;

    private RelativeLayout mLayoutRoot;
    private ViewGroup mCameraPreviewContainer;
    private View mCameraPreview;
    private ImageView mCameraFocusIndicator;
    @VisibleForTesting
    ImageButton mButtonCameraTrigger;
    private ImageButton mButtonCameraFlash;
    private LinearLayout mLayoutNoPermission;
    private ImageButton mButtonImportDocument;
    private View mQRCodeDetectedPopupContainer;
    private View mUnsupportedQRCodeDetectedPopupContainer;
    private View mUploadHintCloseButton;
    private View mUploadHintContainer;
    private View mUploadHintContainerArrow;
    private View mQRCodeScannerHintCloseButton;
    private View mQRCodeScannerHintContainer;
    private View mQRCodeScannerHintContainerArrow;
    private View mCameraPreviewShade;
    private View mActivityIndicatorBackground;
    private ProgressBar mActivityIndicator;
    private ViewPropertyAnimatorCompat mCameraPreviewShadeAnimation;

    private HintPopup mUploadHintPopup;
    private HintPopup mQRCodeScannerHintPopup;

    private ViewStubSafeInflater mViewStubInflater;

    private boolean mIsTakingPicture;

    private boolean mImportDocumentButtonEnabled;
    private ImportImageDocumentUrisAsyncTask mImportUrisAsyncTask;
    private boolean mProceededToMultiPageReview;
    private boolean mQRCodeAnalysisCompleted;
    private QRCodeDocument mQRCodeDocument;
    private LinearLayout mImportButtonContainer;
    private boolean mInstanceStateSaved;

    CameraFragmentImpl(@NonNull final FragmentImplCallback fragment) {
        mFragment = fragment;
    }

    @Override
     public void onPaymentQRCodeDataAvailable(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        handleQRCodeDetected(paymentQRCodeData, paymentQRCodeData.getUnparsedContent());
    }

    @Override
    public void onNonPaymentQRCodeDetected(@NonNull String qrCodeContent) {
        handleQRCodeDetected(null, qrCodeContent);
    }

    private void handleQRCodeDetected(@Nullable final PaymentQRCodeData paymentQRCodeData,
                                      @NonNull final String qrCodeContent) {
         if (mUploadHintContainer.getVisibility() == View.VISIBLE
                 || mInterfaceHidden
                 || mActivityIndicator.getVisibility() == View.VISIBLE) {
            mPaymentQRCodePopup.hide();
            mUnsupportedQRCodePopup.hide();
             return;
         }

        if (paymentQRCodeData == null) {
            final boolean showWithDelay = mPaymentQRCodePopup.isShown();
            mPaymentQRCodePopup.hide(new ViewPropertyAnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(View view) {
                    mUnsupportedQRCodePopup.show(qrCodeContent,
                            showWithDelay ? getDifferentQRCodeDetectedPopupDelayMs() : 0);
                }
            });
         } else {
            final boolean showWithDelay = mUnsupportedQRCodePopup.isShown();
            mUnsupportedQRCodePopup.hide(new ViewPropertyAnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(View view) {
                    mPaymentQRCodePopup.show(paymentQRCodeData,
                            showWithDelay ? getDifferentQRCodeDetectedPopupDelayMs() : 0);
                }
            });
         }
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
        mListener = listener;
    }

    public void onCreate(final Bundle savedInstanceState) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        forcePortraitOrientationOnPhones(activity);
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
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_camera, container, false);
        bindViews(view);
        setInputHandlers();
        createPopups();
        return view;
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

    private void createPopups() {
        mPaymentQRCodePopup =
                new QRCodePopup<>(mFragment, mQRCodeDetectedPopupContainer,
                        DEFAULT_ANIMATION_DURATION, getHideQRCodeDetectedPopupDelayMs(),
                        getDifferentQRCodeDetectedPopupDelayMs(),
                        new Function1<PaymentQRCodeData, Unit>() {
                            @Override
                            public Unit invoke(@Nullable PaymentQRCodeData paymentQRCodeData) {
                                if (paymentQRCodeData == null) {
                                    return null;
                                }
                                handlePaymentQRCodeData(paymentQRCodeData);
                                return null;
                            }
                        });

        mUnsupportedQRCodePopup =
                new QRCodePopup<>(mFragment, mUnsupportedQRCodeDetectedPopupContainer,
                        DEFAULT_ANIMATION_DURATION, getHideQRCodeDetectedPopupDelayMs(),
                        getDifferentQRCodeDetectedPopupDelayMs());

        mUploadHintPopup = new HintPopup(mUploadHintContainer, mUploadHintContainerArrow,
                mUploadHintCloseButton, DEFAULT_ANIMATION_DURATION,
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        closeUploadHintPopUp();
                        return null;
                    }
                });

        mQRCodeScannerHintPopup = new HintPopup(mQRCodeScannerHintContainer,
                mQRCodeScannerHintContainerArrow, mQRCodeScannerHintCloseButton,
                DEFAULT_ANIMATION_DURATION, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                closeQRCodeScannerHintPopUp();
                return null;
            }
        });
    }

    public void onStart() {
        checkGiniCaptureInstance();
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        mInstanceStateSaved = false;
        mProceededToMultiPageReview = false;
        initViews();
        initCameraController(activity);
        addCameraPreviewView();
        if (isQRCodeScanningEnabled()) {
            initQRCodeReader(activity);
        }

        if (isCameraPermissionGranted()) {
            openCamera().thenAccept(new CompletableFuture.Action<Void>() {
                @Override
                public void accept(Void unused) {
                    enableTapToFocus();
                    showHintPopUpsOnFirstExecution();
                    initFlashButton();
                }
            });
        } else {
            showNoPermissionView();
        }
    }

    private void checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            mListener.onError(new GiniCaptureError(MISSING_GINI_CAPTURE_INSTANCE,
                    "Missing GiniCapture instance. It was not created or there was an application process restart."));
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
                mButtonCameraFlash.setVisibility(View.VISIBLE);
            }
            updateCameraFlashState();
        }

    }

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
                updateImageStack();
            } else {
                mInMultiPageState = false;
                mMultiPageDocument = null;
                mImageStack.removeImages();
            }
        }
    }

    private void initQRCodeReader(final Activity activity) {
        if (mPaymentQRCodeReader != null) {
            return;
        }
        final QRCodeDetectorTaskGoogleVision qrCodeDetectorTask =
                new QRCodeDetectorTaskGoogleVision(activity);
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

    @VisibleForTesting
    PaymentQRCodeReader getPaymentQRCodeReader() {
        return mPaymentQRCodeReader;
    }


     private void showUploadHintPopUpOnFirstExecution() {
        if (mInterfaceHidden) {
            return;
        }
        if (shouldShowUploadHintPopUp()) {
            showUploadHintPopUp();
        }
    }

    private void showQrcodeScannerHintPopUpOnFirstExecution() {
        if (mInterfaceHidden) {
            return;
        }
        if (shouldShowQRCodeScannerHintPopup()) {
            showQRCodeScannerHintPopUp();
        }
    }

    private void showHintPopUpsOnFirstExecution() {
        if (mInterfaceHidden) {
            return;
        }
        if (shouldShowUploadHintPopUp()) {
             showUploadHintPopUp();
        } else if (shouldShowQRCodeScannerHintPopup()) {
            showQRCodeScannerHintPopUp();
         }
     }

    @VisibleForTesting
    void showUploadHintPopUp() {
        disableCameraTriggerButtonAnimated(0.3f);
        disableFlashButtonAnimated(0.3f);
        showHintPopup(mUploadHintPopup);
    }

    private void showQRCodeScannerHintPopUp() {
        disableImportButtonAnimated(0.3f);
        disableFlashButtonAnimated(0.3f);
        showHintPopup(mQRCodeScannerHintPopup);
    }

    private void showHintPopup(@NonNull final HintPopup hintPopup) {
        clearHintPopupRelatedAnimations();
        hintPopup.show();
        mCameraPreviewShade.setVisibility(View.VISIBLE);
        mCameraPreviewShade.setClickable(true);
        mCameraPreviewShadeAnimation = ViewCompat.animate(
                mCameraPreviewShade)
                .alpha(1)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mCameraPreviewShadeAnimation.start();
    }

    private void clearHintPopupRelatedAnimations() {
        if (mCameraPreviewShadeAnimation != null) {
            mCameraPreviewShadeAnimation.cancel();
            mCameraPreviewShade.clearAnimation();
            mCameraPreviewShadeAnimation.setListener(null);
        }
    }

    private boolean shouldShowUploadHintPopUp() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return false;
        }
        if (!isDocumentImportEnabled(activity) || mInterfaceHidden) {
            return false;
        }
        final Context context = mFragment.getActivity();
        if (context != null) {
            final SharedPreferences gcSharedPrefs = context.getSharedPreferences(GC_SHARED_PREFS,
                    Context.MODE_PRIVATE);
            return gcSharedPrefs.getBoolean(SHOW_UPLOAD_HINT_POP_UP, true);
        }
        return false;
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
        final int top = Math.round((mLayoutRoot.getHeight() - previewViewSize.height) / 2.0f);
        final int left = Math.round((mLayoutRoot.getWidth() - previewViewSize.width) / 2.0f);
        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mCameraFocusIndicator.getLayoutParams();
        layoutParams.leftMargin = (int) Math.round(
                left + point.x - (mCameraFocusIndicator.getWidth() / 2.0));
        layoutParams.topMargin = (int) Math.round(
                top + point.y - (mCameraFocusIndicator.getHeight() / 2.0));
        mCameraFocusIndicator.setLayoutParams(layoutParams);
        mCameraFocusIndicator.animate().setDuration(DEFAULT_ANIMATION_DURATION).alpha(1.0f);
    }

    private void hideFocusIndicator() {
        mCameraFocusIndicator.animate().setDuration(DEFAULT_ANIMATION_DURATION).alpha(0.0f);
    }

    private CompletableFuture<Void> openCamera() {
        LOG.info("Opening camera");
        return mCameraController.open()
                .handle(new CompletableFuture.BiFun<Void, Throwable, Void>() {
                    @Override
                    public Void apply(final Void aVoid, final Throwable throwable) {
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
                            hideNoPermissionView();
                        }
                        return null;
                    }
                });
    }

    void onSaveInstanceState(@NonNull final Bundle outState) {
        mInstanceStateSaved = true;
        outState.putBoolean(IN_MULTI_PAGE_STATE_KEY, mInMultiPageState);
        outState.putBoolean(IS_FLASH_ENABLED_KEY, mIsFlashEnabled);
    }

    void onStop() {
        closeCamera();
        clearHintPopupRelatedAnimations();
        mUploadHintPopup.hide(null);
        if (mPaymentQRCodePopup != null) {
            mPaymentQRCodePopup.hide();
        }
        if (mUnsupportedQRCodePopup != null) {
            mUnsupportedQRCodePopup.hide();
        }
    }

    void onDestroy() {
        if (mImportUrisAsyncTask != null) {
            mImportUrisAsyncTask.cancel(true);
        }

        if (!mInstanceStateSaved) {
            if (!mProceededToMultiPageReview) {
                deleteUploadedMultiPageDocuments();
                clearMultiPageDocument();
            }
            if (!mQRCodeAnalysisCompleted) {
                deleteUploadedQRCodeDocument();
            }
        }
    }

    private void clearMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            mMultiPageDocument = null; // NOPMD
            GiniCapture.getInstance().internal()
                    .getImageMultiPageDocumentMemoryStore().clear();
        }
    }

    private void deleteUploadedMultiPageDocuments() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (mMultiPageDocument == null) {
            return;
        }

        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(mMultiPageDocument);
                networkRequestsManager.delete(mMultiPageDocument)
                        .handle(new CompletableFuture.BiFun<NetworkRequestResult<
                                GiniCaptureDocument>, Throwable, Void>() {
                            @Override
                            public Void apply(
                                    final NetworkRequestResult<GiniCaptureDocument> requestResult,
                                    final Throwable throwable) {
                                for (final Object document : mMultiPageDocument.getDocuments()) {
                                    final GiniCaptureDocument giniCaptureDocument =
                                            (GiniCaptureDocument) document;
                                    networkRequestsManager.cancel(giniCaptureDocument);
                                    networkRequestsManager.delete(giniCaptureDocument);
                                }
                                return null;
                            }
                        });
            }
        }
    }

    private void deleteUploadedQRCodeDocument() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (mQRCodeDocument == null) {
            return;
        }
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(mQRCodeDocument);
                networkRequestsManager.delete(mQRCodeDocument);
            }
        }
    }

    private void closeCamera() {
        LOG.info("Closing camera");
        if (mPaymentQRCodeReader != null) {
            mPaymentQRCodeReader.release();
            mPaymentQRCodeReader = null; // NOPMD
        }
        mCameraController.disableTapToFocus();
        mCameraController.setPreviewCallback(null);
        mCameraController.stopPreview();
        mCameraController.close();
        LOG.info("Camera closed");
    }

    private void bindViews(final View view) {
        mLayoutRoot = view.findViewById(R.id.gc_root);
        mCameraPreviewContainer = view.findViewById(R.id.gc_camera_preview_container);
        mImageCorners = view.findViewById(R.id.gc_image_corners);
        mCameraFocusIndicator = view.findViewById(R.id.gc_camera_focus_indicator);
        mButtonCameraTrigger = view.findViewById(R.id.gc_button_camera_trigger);
        bindFlashButtonView(view);
        final ViewStub stubNoPermission = view.findViewById(R.id.gc_stub_camera_no_permission);
        mViewStubInflater = new ViewStubSafeInflater(stubNoPermission);
        mButtonImportDocument = view.findViewById(R.id.gc_button_import_document);
        mImportButtonContainer = view.findViewById(R.id.gc_document_import_button_container);
        mUploadHintContainer = view.findViewById(R.id.gc_document_import_hint_container);
        mUploadHintContainerArrow = view.findViewById(R.id.gc_document_import_hint_container_arrow);
        mUploadHintCloseButton = view.findViewById(R.id.gc_document_import_hint_close_button);
        mQRCodeScannerHintContainer = view.findViewById(R.id.gc_qr_code_scanner_hint_container);
        mQRCodeScannerHintContainerArrow = view.findViewById(R.id.gc_qr_code_scanner_hint_container_arrow);
        mQRCodeScannerHintCloseButton = view.findViewById(R.id.gc_qr_code_scanner_hint_close_button);
        mCameraPreviewShade = view.findViewById(R.id.gc_camera_preview_shade);
        mActivityIndicatorBackground =
                view.findViewById(R.id.gc_activity_indicator_background);
        mActivityIndicator = view.findViewById(R.id.gc_activity_indicator);
        mQRCodeDetectedPopupContainer = view.findViewById(
                R.id.gc_qrcode_detected_popup_container);
        mUnsupportedQRCodeDetectedPopupContainer = view.findViewById(
                R.id.gc_unsupported_qrcode_detected_popup_container);
        mImageStack = view.findViewById(R.id.gc_image_stack);
    }

    private void bindFlashButtonView(final View view) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (isTablet(activity)) {
            mButtonCameraFlash = view.findViewById(R.id.gc_button_camera_flash);
            if (mButtonCameraFlash != null) {
                return;
            }
        }
        final FlashButtonPosition flashButtonPosition = getFlashButtonPosition(
                isDocumentImportEnabled(activity), isMultiPageEnabled());
        switch (flashButtonPosition) {
            case LEFT_OF_CAMERA_TRIGGER:
                mButtonCameraFlash = view.findViewById(R.id.gc_button_camera_flash_left_of_trigger);
                break;
            case BOTTOM_LEFT:
                mButtonCameraFlash = view.findViewById(R.id.gc_button_camera_flash_bottom_left);
                break;
            case BOTTOM_RIGHT:
                mButtonCameraFlash = view.findViewById(R.id.gc_button_camera_flash_bottom_right);
                break;
            default:
                throw new UnsupportedOperationException("Unknown flash button position: "
                        + flashButtonPosition);
        }
    }

    private void initViews() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (!mInterfaceHidden && isDocumentImportEnabled(activity)) {
            mImportDocumentButtonEnabled = true;
            mImportButtonContainer.setVisibility(View.VISIBLE);
            showImportDocumentButtonAnimated();
        }
    }

    private boolean isDocumentImportEnabled(@NonNull final Activity activity) {
        return getDocumentImportEnabledFileTypes()
                != DocumentImportEnabledFileTypes.NONE
                && FileChooserActivity.canChooseFiles(activity);
    }

    private void setInputHandlers() {
        mCameraPreviewShade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                closeUploadHintPopUp();
                closeQRCodeScannerHintPopUp();
            }
        });
        mButtonCameraTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mQRCodeScannerHintPopup.isShown()) {
                    closeQRCodeScannerHintPopUp();
                } else {
                    onCameraTriggerClicked();
                }
            }
        });
        mButtonCameraFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mIsFlashEnabled = !mCameraController.isFlashEnabled();
                updateCameraFlashState();
            }
        });
        mImportButtonContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mUploadHintPopup.setIsLastPopup(true);
                closeUploadHintPopUp();
                showFileChooser();
            }
        });
        mImageStack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mProceededToMultiPageReview = true;
                mListener.onProceedToMultiPageReviewScreen(mMultiPageDocument);
            }
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
        mIsTakingPicture = true;
        mCameraController.takePicture()
                .handle(new CompletableFuture.BiFun<Photo, Throwable, Void>() {
                    @Override
                    public Void apply(final Photo photo, final Throwable throwable) {
                        mUIExecutor.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                trackCameraScreenEvent(CameraScreenEvent.TAKE_PICTURE);
                                onPictureTaken(photo, throwable);
                            }
                        });
                        return null;
                    }
                });
    }

    private void handlePaymentQRCodeData(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        switch (paymentQRCodeData.getFormat()) {
            case EPC069_12:
            case BEZAHL_CODE:
                mQRCodeDocument = QRCodeDocument.fromPaymentQRCodeData(
                        paymentQRCodeData);
                analyzeQRCode(mQRCodeDocument);
                break;
            case EPS_PAYMENT:
                handleEPSPaymentQRCode(paymentQRCodeData);
                break;
            default:
                LOG.error("Unknown payment QR Code format: {}", paymentQRCodeData);
                break;
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
        mListener.onExtractionsAvailable(
                Collections.singletonMap(EXTRACTION_ENTITY_NAME, specificExtraction));
    }

    private void updateCameraFlashState() {
        mCameraController.setFlashEnabled(mIsFlashEnabled);
        updateFlashButtonImage();
    }

    private void updateFlashButtonImage() {
        final int flashIconRes = mIsFlashEnabled ? R.drawable.gc_camera_flash_on
                : R.drawable.gc_camera_flash_off;
        mButtonCameraFlash.setImageResource(flashIconRes);
    }

    @VisibleForTesting
    void analyzeQRCode(final QRCodeDocument qrCodeDocument) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        mQRCodeAnalysisCompleted = false;
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager =
                    GiniCapture.getInstance().internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                showActivityIndicatorAndDisableInteraction();
                networkRequestsManager
                        .upload(activity, qrCodeDocument)
                        .handle(new CompletableFuture.BiFun<NetworkRequestResult<
                                GiniCaptureDocument>, Throwable,
                                NetworkRequestResult<GiniCaptureDocument>>() {
                            @Override
                            public NetworkRequestResult<GiniCaptureDocument> apply(
                                    final NetworkRequestResult<GiniCaptureDocument> requestResult,
                                    final Throwable throwable) {
                                if (throwable != null) {
                                    hideActivityIndicatorAndEnableInteraction();
                                    if (!isCancellation(throwable)) {
                                        handleAnalysisError();
                                    }
                                }
                                return requestResult;
                            }
                        })
                        .thenCompose(
                                new CompletableFuture.Fun<NetworkRequestResult<GiniCaptureDocument>,
                                        CompletableFuture<AnalysisNetworkRequestResult<
                                                GiniCaptureMultiPageDocument>>>() {
                                    @Override
                                    public CompletableFuture<AnalysisNetworkRequestResult<
                                            GiniCaptureMultiPageDocument>> apply(
                                            final NetworkRequestResult<GiniCaptureDocument>
                                                    requestResult) {
                                        if (requestResult != null) {
                                            final GiniCaptureMultiPageDocument multiPageDocument =
                                                    DocumentFactory.newMultiPageDocument(
                                                            qrCodeDocument);
                                            return networkRequestsManager.analyze(
                                                    multiPageDocument);
                                        }
                                        return CompletableFuture.completedFuture(null);
                                    }
                                })
                        .handle(new CompletableFuture.BiFun<AnalysisNetworkRequestResult<
                                GiniCaptureMultiPageDocument>, Throwable, Void>() {
                            @Override
                            public Void apply(
                                    final AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>
                                            requestResult,
                                    final Throwable throwable) {
                                hideActivityIndicatorAndEnableInteraction();
                                if (throwable != null
                                        && !isCancellation(throwable)) {
                                    handleAnalysisError();
                                } else if (requestResult != null) {
                                    mQRCodeAnalysisCompleted = true;
                                    mListener.onExtractionsAvailable(
                                            requestResult.getAnalysisResult().getExtractions());
                                }
                                return null;
                            }
                        });
            }
        }
    }

    private void handleAnalysisError() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        showError(activity.getString(R.string.gc_document_analysis_error), 3000);
    }

    private void closeUploadHintPopUp() {
        if (!mUploadHintPopup.isShown()) {
            return;
        }
        hideUploadHintPopup(new ViewPropertyAnimatorListenerAdapter() {
             @Override
             public void onAnimationEnd(final View view) {
                 final Context context = view.getContext();
                saveUploadHintPopUpShown(context);
                if (shouldShowQRCodeScannerHintPopup()) {
                    showQRCodeScannerHintPopUp();
                }
             }
         });
     }

    private void hideUploadHintPopup(@Nullable final ViewPropertyAnimatorListenerAdapter
                                             animatorListener) {
         if (!mInterfaceHidden) {
             enableCameraTriggerButtonAnimated();
             enableFlashButtonAnimated();
         }
        hideHintPopup(mUploadHintPopup, animatorListener);
    }

    private void hideHintPopup(@NonNull final HintPopup hintPopup,
                               @Nullable final ViewPropertyAnimatorListenerAdapter
            animatorListener) {
        clearHintPopupRelatedAnimations();
        hintPopup.hide(new ViewPropertyAnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(View view) {
                mCameraPreviewShade.setVisibility(View.GONE);
                mCameraPreviewShade.setClickable(false);
                if (animatorListener != null) {
                    animatorListener.onAnimationEnd(view);
                }
            }
        });
        mCameraPreviewShadeAnimation = ViewCompat.animate(mCameraPreviewShade)
                .alpha(0)
                .setDuration(DEFAULT_ANIMATION_DURATION);
        mCameraPreviewShadeAnimation.start();
    }

    private void closeQRCodeScannerHintPopUp() {
        if (!mQRCodeScannerHintPopup.isShown()) {
            return;
        }
        hideQRCodeScannerHintPopup(new ViewPropertyAnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final View view) {
                final Context context = view.getContext();
                saveQRCodeScannerHintPopUpShown(context);
            }
        });
    }

    private boolean shouldShowQRCodeScannerHintPopup() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return false;
        }
        if (!isQRCodeScanningEnabled()
                || mInterfaceHidden
                || mUploadHintPopup.isLastPopup()) {
            return false;
        }
        final Context context = mFragment.getActivity();
        if (context != null) {
            final SharedPreferences gcSharedPrefs = context.getSharedPreferences(GC_SHARED_PREFS,
                    Context.MODE_PRIVATE);
            return gcSharedPrefs.getBoolean(SHOW_QRCODE_SCANNER_HINT_POP_UP, true);
        }
        return false;
    }

    private void hideQRCodeScannerHintPopup(@Nullable final ViewPropertyAnimatorListenerAdapter
                                             animatorListener) {
        if (!mInterfaceHidden) {
            enableFlashButtonAnimated();
            enableImportButtonAnimated();
        }
        hideHintPopup(mQRCodeScannerHintPopup, animatorListener);
    }

    private void saveUploadHintPopUpShown(final Context context) {
         final SharedPreferences gcSharedPrefs = context.getSharedPreferences(GC_SHARED_PREFS,
                 Context.MODE_PRIVATE);
        gcSharedPrefs.edit().putBoolean(SHOW_UPLOAD_HINT_POP_UP, false).apply();
    }

    private void saveQRCodeScannerHintPopUpShown(final Context context) {
        final SharedPreferences gcSharedPrefs = context.getSharedPreferences(GC_SHARED_PREFS,
                Context.MODE_PRIVATE);
        gcSharedPrefs.edit().putBoolean(SHOW_QRCODE_SCANNER_HINT_POP_UP, false).apply();
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
        final Intent fileChooserIntent = FileChooserActivity.createIntent(activity);
        final DocumentImportEnabledFileTypes enabledFileTypes;
        if (mInMultiPageState) {
            enabledFileTypes = DocumentImportEnabledFileTypes.IMAGES;
        } else {
            enabledFileTypes = getDocumentImportEnabledFileTypes();
        }
        fileChooserIntent.putExtra(FileChooserActivity.EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES,
                enabledFileTypes);
        fileChooserIntent.setExtrasClassLoader(CameraFragmentImpl.class.getClassLoader());
        mFragment.startActivityForResult(fileChooserIntent, REQ_CODE_CHOOSE_FILE);
    }

    boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQ_CODE_CHOOSE_FILE) {
            if (resultCode == RESULT_OK) {
                importDocumentFromIntent(data);
            } else if (resultCode != RESULT_CANCELED) {
                final String message;
                if (resultCode == FileChooserActivity.RESULT_ERROR) {
                    final GiniCaptureError error = data.getParcelableExtra(
                            FileChooserActivity.EXTRA_OUT_ERROR);
                    message = "Document import failed: " + error.getMessage();
                } else {
                    message = "Document import failed: unknown result code " + resultCode;
                }
                LOG.error(message);
                showGenericInvalidFileError();
            }
            return true;
        }
        return false;
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
                showGenericInvalidFileError();
                return;
            }
            handleMultiPageDocumentAndCallListener(activity, data, uris);
        } else {
            final Uri uri = IntentHelper.getUri(data);
            if (uri == null) {
                LOG.error("Document import failed: Intent has no Uri");
                showGenericInvalidFileError();
                return;
            }
            if (!UriHelper.isUriInputStreamAvailable(uri, activity)) {
                LOG.error("Document import failed: InputStream not available for the Uri");
                showGenericInvalidFileError();
                return;
            }

            if (isMultiPageEnabled() && isImage(data, activity)) {
                handleMultiPageDocumentAndCallListener(activity, data,
                        Collections.singletonList(uri));
            } else {
                final FileImportValidator fileImportValidator = new FileImportValidator(activity);
                if (fileImportValidator.matchesCriteria(data, uri)) {
                    createSinglePageDocumentAndCallListener(data, activity);
                } else {
                    final FileImportValidator.Error error = fileImportValidator.getError();
                    if (error != null) {
                        showInvalidFileError(error);
                    } else {
                        showGenericInvalidFileError();
                    }
                }
            }
        }
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
            LOG.info("Document imported: {}", document);
            requestClientDocumentCheck(document);
        } catch (final IllegalArgumentException e) {
            LOG.error("Failed to import selected document", e);
            showGenericInvalidFileError();
        }
    }

    private void requestClientDocumentCheck(final GiniCaptureDocument document) {
        showActivityIndicatorAndDisableInteraction();
        LOG.debug("Requesting document check from client");
        mListener.onCheckImportedDocument(document,
                new CameraFragmentListener.DocumentCheckResultCallback() {
                    @Override
                    public void documentAccepted() {
                        LOG.debug("Client accepted the document");
                        hideActivityIndicatorAndEnableInteraction();
                        if (document.getType() == Document.Type.IMAGE_MULTI_PAGE) {
                            mProceededToMultiPageReview = true;
                            final ImageMultiPageDocument multiPageDocument =
                                    (ImageMultiPageDocument) document;
                            addToMultiPageDocumentMemoryStore(multiPageDocument);
                            mListener.onProceedToMultiPageReviewScreen(
                                    multiPageDocument);
                        } else {
                            mListener.onDocumentAvailable(document);
                        }
                    }

                    @Override
                    public void documentRejected(@NonNull final String messageForUser) {
                        LOG.debug("Client rejected the document: {}", messageForUser);
                        hideActivityIndicatorAndEnableInteraction();
                        showInvalidFileAlert(messageForUser);
                    }
                });
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
        mImportUrisAsyncTask = new ImportImageDocumentUrisAsyncTask(
                context, intent, GiniCapture.getInstance(),
                Document.Source.newExternalSource(), ImportMethod.PICKER,
                new AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException>() {
                    @Override
                    public void onSuccess(final ImageMultiPageDocument multiPageDocument) {
                        if (mMultiPageDocument == null) {
                            mInMultiPageState = true;
                            mMultiPageDocument = multiPageDocument;
                        } else {
                            mMultiPageDocument.addDocuments(multiPageDocument.getDocuments());
                        }
                        if (mMultiPageDocument.getDocuments().isEmpty()) {
                            LOG.error("Document import failed: Intent did not contain images");
                            showGenericInvalidFileError();
                            mMultiPageDocument = null; // NOPMD
                            mInMultiPageState = false;
                            return;
                        }
                        LOG.info("Document imported: {}", mMultiPageDocument);
                        updateImageStack();
                        hideActivityIndicatorAndEnableInteraction();
                        requestClientDocumentCheck(mMultiPageDocument);
                    }

                    @Override
                    public void onError(final ImportedFileValidationException exception) {
                        LOG.error("Document import failed", exception);
                        hideActivityIndicatorAndEnableInteraction();
                        final FileImportValidator.Error error = exception.getValidationError();
                        if (error != null) {
                            showInvalidFileError(error);
                        } else {
                            showGenericInvalidFileError();
                        }
                    }

                    @Override
                    public void onCancelled() {

                    }
                });
        mImportUrisAsyncTask.execute(uris.toArray(new Uri[uris.size()]));
    }

    private boolean exceedsMultiPageLimit() {
        return mInMultiPageState && mMultiPageDocument.getDocuments().size()
                >= FileImportValidator.DOCUMENT_PAGE_LIMIT;
    }

    @Override
    public void showActivityIndicatorAndDisableInteraction() {
        if (mActivityIndicator == null
                || mActivityIndicatorBackground == null) {
            return;
        }
        mActivityIndicatorBackground.setVisibility(View.VISIBLE);
        mActivityIndicatorBackground.setClickable(true);
        mActivityIndicator.setVisibility(View.VISIBLE);
        disableInteraction();
    }

    @Override
    public void hideActivityIndicatorAndEnableInteraction() {
        if (mActivityIndicator == null
                || mActivityIndicatorBackground == null) {
            return;
        }
        mActivityIndicatorBackground.setVisibility(View.INVISIBLE);
        mActivityIndicatorBackground.setClickable(false);
        mActivityIndicator.setVisibility(View.INVISIBLE);
        enableInteraction();
    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        if (mFragment.getActivity() == null || mLayoutRoot == null) {
            return;
        }
        ErrorSnackbar.make(mFragment.getActivity(), mLayoutRoot, message, null, null,
                duration).show();
    }

    private void updateImageStack() {
        final List<ImageDocument> documents = mMultiPageDocument.getDocuments();
        if (!documents.isEmpty()) {
            mImageStack.removeImages();
        }
        final int size = documents.size();
        if (size >= 3) {
            showImageDocumentsInStack(
                    Arrays.asList(
                            documents.get(size - 1),
                            documents.get(size - 2),
                            documents.get(size - 3)),
                    Arrays.asList(
                            ImageStack.Position.TOP,
                            ImageStack.Position.MIDDLE,
                            ImageStack.Position.BOTTOM));
        } else if (size == 2) {
            showImageDocumentsInStack(
                    Arrays.asList(
                            documents.get(size - 1),
                            documents.get(size - 2)),
                    Arrays.asList(
                            ImageStack.Position.TOP,
                            ImageStack.Position.MIDDLE));
        } else if (size == 1) {
            showImageDocumentsInStack(
                    Collections.singletonList(
                            documents.get(size - 1)),
                    Collections.singletonList(
                            ImageStack.Position.TOP));
        }
    }

    private void showImageDocumentsInStack(@NonNull final List<ImageDocument> documents,
            @NonNull final List<ImageStack.Position> positions) {
        if (!GiniCapture.hasInstance()) {
            LOG.error(
                    "Cannot show images in stack. GiniCapture instance not available. Create it with GiniCapture.newInstance().");
        }
        if (documents.size() != positions.size()) {
            return;
        }
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final int imagesToLoadCount = documents.size();
        final AtomicInteger imagesLoadedCounter = new AtomicInteger();
        for (int i = 0; i < documents.size(); i++) {
            final ImageDocument document = documents.get(i);
            final ImageStack.Position position = positions.get(i);
            GiniCapture.getInstance().internal().getPhotoMemoryCache()
                    .get(activity, document, new AsyncCallback<Photo, Exception>() { // NOPMD
                        @Override
                        public void onSuccess(final Photo result) {
                            mImageStack.setImage(
                                    new ImageStack.StackBitmap(result.getBitmapPreview(),
                                            document.getRotationForDisplay()), position);
                            imagesLoadedCounter.incrementAndGet();
                            if (imagesToLoadCount == imagesLoadedCounter.get()) {
                                mImageStack.setImageCount(mMultiPageDocument.getDocuments().size());
                            }
                        }

                        @Override
                        public void onError(final Exception exception) {
                            mImageStack.setImage(null, position);
                            imagesLoadedCounter.incrementAndGet();
                            if (imagesToLoadCount == imagesLoadedCounter.get()) {
                                mImageStack.setImageCount(mMultiPageDocument.getDocuments().size());
                            }
                        }

                        @Override
                        public void onCancelled() {
                            // Not used
                        }
                    });
        }
    }

    private void enableInteraction() {
        if (mCameraPreview == null
                || mButtonImportDocument == null
                || mImportButtonContainer == null
                || mButtonCameraTrigger == null) {
            return;
        }
        mCameraPreview.setEnabled(true);
        mButtonImportDocument.setEnabled(true);
        mImportButtonContainer.setEnabled(true);
        mButtonCameraTrigger.setEnabled(true);
    }

    private void disableInteraction() {
        if (mCameraPreview == null
                || mButtonImportDocument == null
                || mImportButtonContainer == null
                || mButtonCameraTrigger == null) {
            return;
        }
        mCameraPreview.setEnabled(false);
        mButtonImportDocument.setEnabled(false);
        mImportButtonContainer.setEnabled(false);
        mButtonCameraTrigger.setEnabled(false);
    }

    private void showInvalidFileError(@NonNull final FileImportValidator.Error error) {
        LOG.error("Invalid document {}", error.toString());
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        showInvalidFileAlert(activity.getString(error.getTextResource()));
    }

    private void showGenericInvalidFileError() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final String message = activity.getString(R.string.gc_document_import_invalid_document);
        LOG.error("Invalid document {}", message);
        showInvalidFileAlert(message);
    }

    private void showInvalidFileAlert(final String message) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        mFragment.showAlertDialog(message,
                activity.getString(R.string.gc_document_import_pick_another_document),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            final DialogInterface dialogInterface,
                            final int i) {
                        showFileChooser();
                    }
                }, activity.getString(R.string.gc_document_import_close_error), null, null);
    }

    @UiThread
    private void onPictureTaken(final Photo photo, final Throwable throwable) {
        if (throwable != null) {
            handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED, "Failed to take picture",
                    throwable);
            mCameraController.startPreview();
            mIsTakingPicture = false;
        } else {
            if (photo != null) {
                LOG.info("Picture taken");
                showActivityIndicatorAndDisableInteraction();
                photo.edit().compressByDefault().applyAsync(new PhotoEdit.PhotoEditCallback() {
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
                                mIsTakingPicture = false;
                                return;
                            }
                            mMultiPageDocument.addDocument(document);
                            mImageStack.addImage(
                                    new ImageStack.StackBitmap(result.getBitmapPreview(),
                                            document.getRotationForDisplay()),
                                    new TransitionListenerAdapter() {
                                        @Override
                                        public void onTransitionEnd(
                                                @NonNull final Transition transition) {
                                            mIsTakingPicture = false;
                                        }
                                    });
                            mCameraController.startPreview();
                        } else {
                            if (isMultiPageEnabled()) {
                                final ImageDocument document = createSavedDocument(result);
                                if (document == null) {
                                    handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                            "Failed to take picture: could not save picture to disk",
                                            null);
                                    mCameraController.startPreview();
                                    mIsTakingPicture = false;
                                    return;
                                }
                                mInMultiPageState = true;
                                mMultiPageDocument = new ImageMultiPageDocument(
                                        Document.Source.newCameraSource(), ImportMethod.NONE);
                                GiniCapture.getInstance().internal()
                                        .getImageMultiPageDocumentMemoryStore()
                                        .setMultiPageDocument(mMultiPageDocument);
                                mMultiPageDocument.addDocument(document);
                                mImageStack.addImage(
                                        new ImageStack.StackBitmap(result.getBitmapPreview(),
                                                document.getRotationForDisplay()),
                                        new TransitionListenerAdapter() {
                                            @Override
                                            public void onTransitionEnd(
                                                    @NonNull final Transition transition) {
                                                mListener.onProceedToMultiPageReviewScreen(
                                                        mMultiPageDocument);
                                                mIsTakingPicture = false;
                                            }
                                        });
                            } else {
                                final ImageDocument document =
                                        DocumentFactory.newImageDocumentFromPhoto(
                                                result);
                                mListener.onDocumentAvailable(document);
                                mIsTakingPicture = false;
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
                        mIsTakingPicture = false;
                    }
                });
            } else {
                handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                        "Failed to take picture: no picture from the camera", null);
                mCameraController.startPreview();
                mIsTakingPicture = false;
            }
        }
    }

    private void showMultiPageLimitError() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        mFragment.showAlertDialog(activity.getString(R.string.gc_document_error_too_many_pages),
                activity.getString(R.string.gc_document_error_multi_page_limit_review_pages_button),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            final DialogInterface dialogInterface,
                            final int i) {
                        mProceededToMultiPageReview = true;
                        mListener.onProceedToMultiPageReviewScreen(mMultiPageDocument);
                    }
                }, activity.getString(R.string.gc_document_error_multi_page_limit_cancel_button),
                null, null);
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

    private void disableFlashButtonAnimated(final float alpha) {
        mButtonCameraFlash.clearAnimation();
        mButtonCameraFlash.animate().alpha(alpha).start();
        mButtonCameraFlash.setEnabled(false);
    }

    private void enableFlashButtonAnimated() {
        mButtonCameraFlash.clearAnimation();
        mButtonCameraFlash.animate().alpha(1.0f).start();
        mButtonCameraFlash.setEnabled(true);
    }

    private void enableImportButtonAnimated() {
        mImportButtonContainer.clearAnimation();
        mImportButtonContainer.animate().alpha(1.0f).start();
        mButtonImportDocument.setEnabled(true);
        mImportButtonContainer.setEnabled(true);
    }

    private void disableImportButtonAnimated(final float alpha) {
        mImportButtonContainer.clearAnimation();
        mImportButtonContainer.animate().alpha(alpha).start();
        mButtonImportDocument.setEnabled(false);
        mImportButtonContainer.setEnabled(false);
    }

    @Override
    public void showInterface() {
        if (!mInterfaceHidden || isNoPermissionViewVisible()) {
            return;
        }
        mInterfaceHidden = false;
        showInterfaceAnimated();
    }

    private void showInterfaceAnimated() {
        showCameraTriggerButtonAnimated();
        showDocumentCornerGuidesAnimated();
        showImageStackAnimated();
        showFlashButtonAnimated();
        if (mImportDocumentButtonEnabled) {
            showUploadHintPopUpOnFirstExecution();
            showImportDocumentButtonAnimated();
        } else {
            showQrcodeScannerHintPopUpOnFirstExecution();
        }
    }

    private void showImageStackAnimated() {
        mImageStack.animate().alpha(1.0f).start();
    }

    private void showImportDocumentButtonAnimated() {
        mImportButtonContainer.animate().alpha(1.0f);
        mButtonImportDocument.setEnabled(true);
        mImportButtonContainer.setEnabled(true);
    }

    private void showFlashButtonAnimated() {
        mButtonCameraFlash.animate().alpha(1.0f);
        mButtonCameraFlash.setEnabled(true);
    }

    @Override
    public void hideInterface() {
        if (mInterfaceHidden || isNoPermissionViewVisible()) {
            return;
        }
        mInterfaceHidden = true;
        hideInterfaceAnimated();
    }

    private void hideInterfaceAnimated() {
        hideCameraTriggerButtonAnimated();
        hideDocumentCornerGuidesAnimated();
        hideImageStackAnimated();
        if (mImportDocumentButtonEnabled) {
            hideUploadHintPopup(null);
            hideImportDocumentButtonAnimated();
        }
        hideQRCodeScannerHintPopup(null);
        hideFlashButtonAnimated();
    }

    private void hideImageStackAnimated() {
        mImageStack.animate().alpha(0.0f).start();
    }

    private void hideImportDocumentButtonAnimated() {
        mImportButtonContainer.animate().alpha(0.0f);
        mButtonImportDocument.setEnabled(false);
        mImportButtonContainer.setEnabled(false);
    }

    private void showNoPermissionView() {
        hideCameraPreviewAnimated();
        hideInterfaceAnimated();
        inflateNoPermissionStub();
        setUpNoPermissionButton();
        if (mLayoutNoPermission != null) {
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
            mLayoutNoPermission = (LinearLayout) mViewStubInflater.inflate();
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
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startApplicationDetailsSettings();
            }
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
        mButtonCameraFlash.animate().alpha(0.0f);
        mButtonCameraFlash.setEnabled(false);
    }

    private void startApplicationDetailsSettings() {
        LOG.debug("Starting Application Details");
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        ApplicationHelper.startApplicationDetailsSettings(activity);
    }

    @VisibleForTesting
    void initCameraController(final Activity activity) {
        if (mCameraController == null) {
            LOG.debug("CameraController created");
            mCameraController = createCameraController(activity);
        }
        if (isQRCodeScanningEnabled()) {
            mCameraController.setPreviewCallback((image, imageSize, rotation) -> {
                if (mPaymentQRCodeReader == null) {
                    return;
                }
                mPaymentQRCodeReader.readFromImage(image, imageSize, rotation);
            });
        }
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
            LOG.error(message, throwable);
            // Add error info to the message to help clients, if they don't have logging enabled
            errorMessage = errorMessage + ": " + throwable.getMessage();
        } else {
            LOG.error(message);
        }
        mListener.onError(new GiniCaptureError(errorCode, errorMessage));
    }
}
