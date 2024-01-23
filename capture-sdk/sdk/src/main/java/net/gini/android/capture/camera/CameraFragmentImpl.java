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
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.EntryPoint;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.ImportImageFileUrisAsyncTask;
import net.gini.android.capture.ImportedFileValidationException;
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
import net.gini.android.capture.internal.camera.api.OldCameraController;
import net.gini.android.capture.internal.camera.api.UIExecutor;
import net.gini.android.capture.internal.camera.api.camerax.CameraXController;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.camera.photo.PhotoEdit;
import net.gini.android.capture.internal.camera.view.QRCodePopup;
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
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.internal.textrecognition.CropToCameraFrameTextRecognizer;
import net.gini.android.capture.internal.textrecognition.MLKitTextRecognizer;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.ui.IntervalToolbarMenuItemIntervalClickListener;
import net.gini.android.capture.internal.ui.ViewStubSafeInflater;
import net.gini.android.capture.internal.util.ApplicationHelper;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.internal.util.DeviceHelper;
import net.gini.android.capture.internal.util.FileImportValidator;
import net.gini.android.capture.internal.util.MimeType;
import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.network.Error;
import net.gini.android.capture.network.model.GiniCaptureExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsFragment;
import net.gini.android.capture.requirements.CameraHolder;
import net.gini.android.capture.requirements.CameraResolutionRequirement;
import net.gini.android.capture.requirements.CameraXHolder;
import net.gini.android.capture.requirements.RequirementReport;
import net.gini.android.capture.review.multipage.MultiPageReviewFragmentDirections;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.CameraScreenEvent;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;

import static net.gini.android.capture.GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE;
import static net.gini.android.capture.camera.CameraFragment.REQUEST_KEY;
import static net.gini.android.capture.camera.CameraFragment.RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE;
import static net.gini.android.capture.document.ImageDocument.ImportMethod;
import static net.gini.android.capture.internal.network.NetworkRequestsManager.isCancellation;
import static net.gini.android.capture.internal.qrcode.EPSPaymentParser.EXTRACTION_ENTITY_NAME;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
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
class CameraFragmentImpl implements CameraFragmentInterface, PaymentQRCodeReader.Listener {

    @VisibleForTesting
    static final String GC_SHARED_PREFS = "GC_SHARED_PREFS";
    @VisibleForTesting
    static final int DEFAULT_ANIMATION_DURATION = 200;
    private static final long HIDE_QRCODE_DETECTED_POPUP_DELAY_MS = 2000;
    private static final long DIFFERENT_QRCODE_DETECTED_POPUP_DELAY_MS = 1000;
    private static final Logger LOG = LoggerFactory.getLogger(CameraFragmentImpl.class);

    private static final CameraFragmentListener NO_OP_LISTENER = new CameraFragmentListener() {
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

    private static final String IN_MULTI_PAGE_STATE_KEY = "IN_MULTI_PAGE_STATE_KEY";
    private static final String IS_FLASH_ENABLED_KEY = "IS_FLASH_ENABLED_KEY";

    private final FragmentImplCallback mFragment;
    private final boolean addPages;

    @VisibleForTesting
    QRCodePopup<PaymentQRCodeData> mPaymentQRCodePopup;
    private QRCodePopup<String> mUnsupportedQRCodePopup;

    private View mImageCorners;
    private PhotoThumbnail mPhotoThumbnail;
    private boolean mInterfaceHidden;
    private boolean mInMultiPageState;
    private boolean mIsFlashEnabled = true;
    private CameraFragmentListener mListener = NO_OP_LISTENER;
    private final UIExecutor mUIExecutor = new UIExecutor();
    private CameraInterface mCameraController;
    private ImageMultiPageDocument mMultiPageDocument;
    private PaymentQRCodeReader mPaymentQRCodeReader;

    private ConstraintLayout mLayoutRoot;
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
    private ConstraintLayout mLayoutNoPermission;
    private ViewGroup mButtonImportDocumentWrapper;
    private Button mButtonImportDocument;
    private ConstraintLayout mCameraFrameWrapper;
    private View mActivityIndicatorBackground;
    private ImageView mImageFrame;
    private ViewStubSafeInflater mViewStubInflater;
    private ConstraintLayout mPaneWrapper;
    private TextView mScanTextView;
    private TextView mIbanDetectedTextView;
    private boolean mIsTakingPicture;

    private boolean mImportDocumentButtonEnabled;
    private ImportImageFileUrisAsyncTask mImportUrisAsyncTask;
    private Group mImportButtonGroup;
    private String mQRCodeContent;

    private InjectedViewContainer<NavigationBarTopAdapter> topAdapterInjectedViewContainer;
    private InjectedViewContainer<CustomLoadingIndicatorAdapter> mLoadingIndicator;
    private InjectedViewContainer<CameraNavigationBarBottomAdapter> mBottomInjectedContainer;

    private IBANRecognizerFilter ibanRecognizerFilter;
    private CropToCameraFrameTextRecognizer cropToCameraFrameTextRecognizer;

    CameraFragmentImpl(@NonNull final FragmentImplCallback fragment, final boolean addPages) {
        mFragment = fragment;
        this.addPages = addPages;
    }

    @Override
    public void onPaymentQRCodeDataAvailable(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        handleQRCodeDetected(paymentQRCodeData, paymentQRCodeData.getUnparsedContent());
    }

    @Override
    public void onNonPaymentQRCodeDetected(@NonNull String qrCodeContent) {
        if (mIbanDetectedTextView.getVisibility() == View.VISIBLE) {
            return;
        }
        handleQRCodeDetected(null, qrCodeContent);
    }

    @Override
    public void onQRCodeReaderFail() {
        LOG.warn(
                "QRCode detector dependencies are not yet available. QRCode detection is disabled.");

        setQRDisabledTexts();
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
                        mPaymentQRCodePopup.show(data);
                    }
                }, 1000);
    }

    private void showQRCodeView(PaymentQRCodeData data, String qrCodeContent) {
        if (data == null) {
            mQRCodeContent = qrCodeContent;
            showUnsupportedQRCodePopup();
        } else {
            mPaymentQRCodePopup.show(data);
        }
    }

    private void showUnsupportedQRCodePopup() {
        if (mIbanDetectedTextView.getVisibility() != View.VISIBLE) {
            mUnsupportedQRCodePopup.show(null);
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
        preventPaneClickThrough();
        setCustomLoadingIndicator();
        setInputHandlers();

        initMultiPageDocument();

        setTopBarInjectedViewContainer();
        setBottomInjectedViewContainer();
        createPopups();
        initOnlyQRScanning();

        if (!GiniCapture.getInstance().isQRCodeScanningEnabled()) {
            setQRDisabledTexts();
        }
        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        final FragmentActivity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        activity.getOnBackPressedDispatcher().addCallback(mFragment.getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                trackCameraScreenEvent(CameraScreenEvent.EXIT);
                setEnabled(false);
                remove();
                mFragment.getActivity().getOnBackPressedDispatcher().onBackPressed();
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

    private void createPopups() {
        mPaymentQRCodePopup =
                new QRCodePopup<>(mFragment, mCameraFrameWrapper, mActivityIndicatorBackground, mLoadingIndicator,
                        getDifferentQRCodeDetectedPopupDelayMs(), true,
                        paymentQRCodeData -> {
                            if (paymentQRCodeData == null) {
                                return null;
                            }
                            handlePaymentQRCodeData(paymentQRCodeData);
                            return null;
                        });

        mUnsupportedQRCodePopup =
                new QRCodePopup<>(mFragment, mCameraFrameWrapper, mActivityIndicatorBackground, null,
                        getHideQRCodeDetectedPopupDelayMs(), false, null, () -> {
                    mQRCodeContent = null;
                    return null;
                });
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public void onStart() {
        checkGiniCaptureInstance();
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        initViews();
        initCameraController(activity);
        addCameraPreviewView();
        if (isQRCodeScanningEnabled()) {
            initQRCodeReader();
        }
        if (GiniCapture.hasInstance()
                && GiniCapture.getInstance().getEntryPoint() == EntryPoint.FIELD
                && !isOnlyQRCodeScanningEnabled()) {
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
            final FileChooserResult fileChooserResult = result.getParcelable(FileChooserFragment.RESULT_KEY);
            if (fileChooserResult != null) {
                handleFileChooserResult(fileChooserResult);
            }
        });
    }

    public void handleFileChooserResult(@NonNull FileChooserResult result) {
        if (result instanceof FileChooserResult.FilesSelected) {
            importDocumentFromIntent(((FileChooserResult.FilesSelected) result).getDataIntent());
        } else if (result instanceof FileChooserResult.Error) {
            final GiniCaptureError error = ((FileChooserResult.Error) result).getError();
            final String message = "Document import failed: " + error.getMessage();
            LOG.error(message);
            showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
        }
    }

    private void checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            mFragment.findNavController().navigate(MultiPageReviewFragmentDirections.toErrorFragment(ErrorType.GENERAL, mMultiPageDocument));
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

    private void initQRCodeReader() {
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

    private void enableTapToFocus() {
        mCameraController.enableTapToFocus(new CameraInterface.TapToFocusListener() {
            @Override
            public void onFocusing(@NonNull final Point point, @NonNull final Size previewViewSize) {
                showFocusIndicator(point);
            }

            @Override
            public void onFocused(final boolean success) {
                hideFocusIndicator();
            }
        });
    }

    private void showFocusIndicator(@NonNull final Point point) {
        mCameraFocusIndicator.setX((float) (point.x - (mCameraFocusIndicator.getWidth() / 2.0)));
        mCameraFocusIndicator.setY(point.y);
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
                        hideNoPermissionView();
                    }
                    return null;
                });
    }

    void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putBoolean(IN_MULTI_PAGE_STATE_KEY, mInMultiPageState);
        outState.putBoolean(IS_FLASH_ENABLED_KEY, mIsFlashEnabled);
    }

    void onStop() {
        closeCamera();
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
    }

    private void closeCamera() {
        LOG.info("Closing camera");
        if (mPaymentQRCodeReader != null) {
            mPaymentQRCodeReader.release();
            mPaymentQRCodeReader = null; // NOPMD
        }
        if (ibanRecognizerFilter != null) {
            ibanRecognizerFilter.cleanup();
            ibanRecognizerFilter = null; // NOPMD
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
        mBottomInjectedContainer = view.findViewById(R.id.gc_injected_navigation_bar_container_bottom);
        mImageFrame = view.findViewById(R.id.gc_camera_frame);
        mCameraFrameWrapper = view.findViewById(R.id.gc_camera_frame_wrapper);
        mPaneWrapper = view.findViewById(R.id.gc_pane_wrapper);
        mLoadingIndicator = view.findViewById(R.id.gc_injected_loading_indicator);
        mIbanDetectedTextView = view.findViewById(R.id.gc_iban_detected);

        if (!ContextHelper.isTablet(mFragment.getActivity())) {
            mScanTextView = view.findViewById(R.id.gc_camera_title);
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
                        if (GiniCapture.getInstance().isQRCodeScanningEnabled()) {
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_info_label_invoice_and_qr));
                        } else {
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_info_label_only_invoice));
                        }
                    } else {
                        injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_title_camera));
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
                    if (mFragment.getActivity() != null) {
                        mFragment.getActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
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
                            if (mFragment.getActivity() != null) {
                                mFragment.getActivity().getOnBackPressedDispatcher().onBackPressed();
                            }
                        }));

                        injectedViewAdapter.setOnHelpButtonClickListener(new IntervalClickListener(v -> startHelpActivity()));
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

    private boolean isOnlyQRCodeScanningEnabled() {
        if (!GiniCapture.hasInstance()) {
            return false;
        }

        return GiniCapture.getInstance().isOnlyQRCodeScanning() && GiniCapture.getInstance().isQRCodeScanningEnabled();
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
        ClickListenerExtKt.setIntervalClickListener(mButtonCameraTrigger, v -> onCameraTriggerClicked());

        ClickListenerExtKt.setIntervalClickListener(mButtonCameraFlashTrigger, v -> {
            mIsFlashEnabled = !mCameraController.isFlashEnabled();
            updateCameraFlashState();
        });

        ClickListenerExtKt.setIntervalClickListener(mButtonImportDocument, v -> showFileChooser());

        ClickListenerExtKt.setIntervalClickListener(mPhotoThumbnail, v -> {
            if (mFragment.getActivity() != null) {
                mFragment.getActivity().getOnBackPressedDispatcher().onBackPressed();
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

    private void handlePaymentQRCodeData(@NonNull final PaymentQRCodeData paymentQRCodeData) {
        switch (paymentQRCodeData.getFormat()) {
            case EPC069_12:
            case BEZAHL_CODE:
                QRCodeDocument mQRCodeDocument = QRCodeDocument.fromPaymentQRCodeData(
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
                                if (requestResult.getAnalysisResult().getExtractions().isEmpty()) {
                                    //mListener.noExtractionsFromQRCode(qrCodeDocument);
                                    NoResultsFragment.navigateToNoResultsFragment(mFragment.findNavController(), CameraFragmentDirections.toNoResultsFragment(qrCodeDocument));
                                    return null;
                                }
                                mListener.onExtractionsAvailable(
                                        requestResult.getAnalysisResult().getExtractions());
                            }
                            return null;
                        });
            }
        }
    }

    private void handleAnalysisError(Throwable throwable, Document document) {

        if (mFragment.getActivity() == null)
            return;

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
                        ErrorType errorType = ErrorType.typeFromError(errorClass);
                        showGenericInvalidFileError(errorType);
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
            showGenericInvalidFileError(ErrorType.FILE_IMPORT_GENERIC);
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
                                mFragment.findNavController().navigate(CameraFragmentDirections.toAnalysisFragment(document , ""));
                            }
                        }
                    }

                    @Override
                    public void documentRejected(@NonNull final String messageForUser) {
                        LOG.debug("Client rejected the document: {}", messageForUser);

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
            mFragment.getActivity().getOnBackPressedDispatcher().onBackPressed();
        } else {
            mFragment.findNavController().navigate(CameraFragmentDirections.toReviewFragment(shouldScrollToLastPage));
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
                        LOG.info("Document imported: {}", mMultiPageDocument);
                        updatePhotoThumbnail();
                        hideActivityIndicatorAndEnableInteraction();
                        requestClientDocumentCheck(mMultiPageDocument);
                    }

                    @Override
                    public void onError(final ImportedFileValidationException exception) {
                        LOG.error("Document import failed", exception);
                        hideActivityIndicatorAndEnableInteraction();
                        final FileImportValidator.Error error = exception.getValidationError();
                        if (error != null && mFragment.getActivity() != null) {
                            Error errorClass = new Error(error);
                            ErrorType errorType = ErrorType.typeFromError(errorClass);
                            showGenericInvalidFileError(errorType);
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
                    }

                    @Override
                    public void onError(final Exception exception) {
                        mPhotoThumbnail.setImage(null);
                        mPhotoThumbnail.setImageCount(documents.size());
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
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        String message = activity.getString(errorType.getTitleTextResource());
        LOG.error("Invalid document {}", message);
        showInvalidFileAlert(message);
    }

    private void showInvalidFileAlert(final String message) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }

        mFragment.showAlertDialog(message,
                activity.getString(R.string.gc_document_import_close_error),
                (dialogInterface, i) -> dialogInterface.dismiss(), null, null, null);
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
        mFragment.showAlertDialog(activity.getString(R.string.gc_document_error_too_many_pages),
                activity.getString(R.string.gc_document_error_multi_page_limit_review_pages_button),
                (dialogInterface, i) -> {
                    proceedToMultiPageReviewScreen(true);
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
        button.setOnClickListener(v -> startApplicationDetailsSettings());
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
            }
        }
    }

    @VisibleForTesting
    void initCameraController(final Activity activity) {
        if (mCameraController == null) {
            LOG.debug("CameraController created");
            mCameraController = createCameraController(activity);
        }
        if (isQRCodeScanningEnabled()) {
            mCameraController.setPreviewCallback(new CameraInterface.PreviewCallback() {
                @Override
                public void onPreviewFrame(@NonNull Image image, @NonNull Size imageSize, int rotation, @NonNull CameraInterface.PreviewFrameCallback previewFrameCallback) {
                    AtomicInteger previewFrameReferenceCount = new AtomicInteger();
                    if (ibanRecognizerFilter != null) {
                        try {
                            previewFrameReferenceCount.getAndIncrement();

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
                        previewFrameReferenceCount.getAndIncrement();

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
    }

    private void handleIBANsDetected(List<String> ibans) {
        if (!ibans.isEmpty() && !isPaymentQRCodeDetectionInProgress()) {
            mUnsupportedQRCodePopup.hide();
            showIBANsDetectedOnScreen(ibans);
        } else {
            hideIBANsDetectedOnScreen();
        }
    }

    private void showIBANsDetectedOnScreen(List<String> ibans) {
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
        mImageFrame.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(
                                mFragment.getActivity(),
                                R.color.gc_light_01
                        )
                )
        );
        mIbanDetectedTextView.setVisibility(View.GONE);
        mIbanDetectedTextView.setText("");
    }

    @NonNull
    protected CameraInterface createCameraController(final Activity activity) {
        if (canUseCameraX(activity)) {
            LOG.info("Using CameraX");
            return new CameraXController(activity);
        }
        LOG.info("Using old camera api");
        return new OldCameraController(activity);
    }

    private boolean canUseCameraX(@NonNull final Context context) {
        final CameraHolder cameraHolder = new CameraXHolder(context);
        final RequirementReport requirementReport = new CameraResolutionRequirement(cameraHolder).check();
        cameraHolder.closeCamera();
        return requirementReport.isFulfilled();
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
