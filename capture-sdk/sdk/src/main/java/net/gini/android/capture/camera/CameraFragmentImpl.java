package net.gini.android.capture.camera;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
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
import net.gini.android.capture.ProductTag;
import net.gini.android.capture.R;
import net.gini.android.capture.camera.view.CameraNavigationBarBottomAdapter;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.error.ErrorFragment;
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
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsFragment;
import net.gini.android.capture.tracking.CameraScreenEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty;
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;

import static net.gini.android.capture.camera.CameraFragment.REQUEST_KEY;
import static net.gini.android.capture.camera.CameraFragment.RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE;
import static net.gini.android.capture.internal.util.AndroidHelper.isMarshmallowOrLater;
import static net.gini.android.capture.internal.util.FeatureConfiguration.getDocumentImportEnabledFileTypes;
import static net.gini.android.capture.internal.util.FeatureConfiguration.isQRCodeScanningEnabled;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackCameraScreenEvent;

/**
 * Internal use only.
 * <p>
 * View layer of the Camera screen: binds the views, wires the CameraX controller, the QR code
 * reader and the IBAN recognizer to the camera preview, runs the animations and executes the
 * one-shot commands emitted by {@link CameraViewModel}. All non-view-bound presentation logic
 * lives in the {@link CameraViewModel}.
 */
class CameraFragmentImpl implements CameraFragmentInterface, PaymentQRCodeReader.Listener {

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

    private final FragmentImplCallback mFragment;
    private final CancelListener mCancelListener;
    private final boolean addPages;

    private CameraViewModel mViewModel;
    CameraFragmentListener fragmentListener = NO_OP_LISTENER;

    private QRCodePopup<PaymentQRCodeData> mPaymentQRCodePopup;
    private QRCodePopup<String> mUnsupportedQRCodePopup;
    @VisibleForTesting
    QRCodeEducationPopup<PaymentQRCodeData> qrCodeEducationPopup;

    private View mImageCorners;
    private PhotoThumbnail mPhotoThumbnail;
    private boolean mIsFlashEnabled = true;

    private final UIExecutor mUIExecutor = new UIExecutor();
    private CameraInterface mCameraController;
    private PaymentQRCodeReader mPaymentQRCodeReader;

    @VisibleForTesting
    UserAnalyticsEventTracker mUserAnalyticsEventTracker;


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
    @VisibleForTesting
    ConstraintLayout mLayoutNoPermission;
    private ViewGroup mButtonImportDocumentWrapper;
    private Button mButtonImportDocument;
    private ConstraintLayout mCameraFrameWrapper;
    private View mActivityIndicatorBackground;
    private ImageView mImageFrame;
    private ViewStubSafeInflater mViewStubInflater;
    private ConstraintLayout mPaneWrapper;
    private ConstraintLayout mDetectionErrorLayout;
    private TextView mScanTextView;
    private TextView mIbanDetectedTextView;
    private boolean mIsTakingPicture;
    private boolean mIsDetectionErrorPopupShowed;

    private boolean mImportDocumentButtonEnabled;
    private Group mImportButtonGroup;
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
        this.addPages = addPages;
    }

    void setViewModel(@NonNull final CameraViewModel viewModel) {
        mViewModel = viewModel;
    }

    @VisibleForTesting
    CameraViewModel getViewModel() {
        return mViewModel;
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
        if (isQRCodeScanningEnabled()) {
            handleQRCodeDetected(null, qrCodeContent);
        }
    }

    @Override
    public void onQRCodeReaderFail() {
        LOG.warn(
                "QRCode detector dependencies are not yet available. QRCode detection is disabled.");

        setQRDisabledTexts();
        if (!mIsDetectionErrorPopupShowed) {
            mIsDetectionErrorPopupShowed = true;
            mDetectionErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    private void handleQRCodeDetected(@Nullable final PaymentQRCodeData paymentQRCodeData,
                                      @NonNull final String qrCodeContent) {
        mViewModel.onQRCodeDetected(paymentQRCodeData, qrCodeContent,
                isPaymentQRCodeDetectionInProgress(), mUnsupportedQRCodePopup.isShown());
    }

    private boolean isPaymentQRCodeDetectionInProgress() {
        return mPaymentQRCodePopup.isShown();
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
        mViewModel.restoreInMultiPageState(
                savedInstanceState.getBoolean(IN_MULTI_PAGE_STATE_KEY));
        mIsFlashEnabled = savedInstanceState.getBoolean(IS_FLASH_ENABLED_KEY);
        mIsDetectionErrorPopupShowed = savedInstanceState.getBoolean(IS_NOT_AVAILABLE_DETECTION_POPUP_SHOWED_KEY);
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

        mViewModel.initMultiPageDocument();

        setTopBarInjectedViewContainer();
        setBottomInjectedViewContainer();
        createPopups(view);
        initOnlyQRScanning();

        if (!GiniCapture.getInstance().isQRCodeScanningEnabled()) {
            setQRDisabledTexts();
        }

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        handleOnBackPressed();
        observeViewModel();
        restoreGenericErrorIfNeeded(savedInstanceState);
    }

    private void observeViewModel() {
        mViewModel.getEvents().observe(mFragment.getViewLifecycleOwner(), consumableEvent -> {
            if (consumableEvent.getContentIfNotHandled() != null) {
                CameraViewEvent event;
                while ((event = mViewModel.pollEvent()) != null) {
                    handleViewEvent(event);
                }
            }
        });
    }

    private void handleViewEvent(@NonNull final CameraViewEvent event) {
        if (event instanceof CameraViewEvent.ShowActivityIndicator) {
            showActivityIndicatorAndDisableInteraction();
        } else if (event instanceof CameraViewEvent.HideActivityIndicator) {
            hideActivityIndicatorAndEnableInteraction();
        } else if (event instanceof CameraViewEvent.HideIbanDetected) {
            hideIBANsDetectedOnScreen();
        } else if (event instanceof CameraViewEvent.HideImageCorners) {
            hideImageCorners();
        } else if (event instanceof CameraViewEvent.ShowUnsupportedQRCodePopup) {
            showUnsupportedQRCodePopup();
        } else if (event instanceof CameraViewEvent.ShowPaymentQRCodePopup) {
            mPaymentQRCodePopup.show(
                    ((CameraViewEvent.ShowPaymentQRCodePopup) event).getData());
        } else if (event instanceof CameraViewEvent.ShowQRCodeEducation) {
            final CameraViewEvent.ShowQRCodeEducation education =
                    (CameraViewEvent.ShowQRCodeEducation) event;
            qrCodeEducationPopup.show(education.getEducationType(), education.getOnComplete());
        } else if (event instanceof CameraViewEvent.HidePaymentQRCodePopup) {
            mPaymentQRCodePopup.hide();
        } else if (event instanceof CameraViewEvent.NavigateToNoResults) {
            NoResultsFragment.navigateToNoResultsFragment(
                    mFragment.findNavController(),
                    CameraFragmentDirections.toNoResultsFragment(
                            ((CameraViewEvent.NavigateToNoResults) event).getDocument()));
        } else if (event instanceof CameraViewEvent.ShowError) {
            final CameraViewEvent.ShowError showError = (CameraViewEvent.ShowError) event;
            ErrorFragment.Companion.navigateToErrorFragment(
                    mFragment.findNavController(),
                    CameraFragmentDirections.toErrorFragment(
                            showError.getErrorType(), showError.getDocument()));
        } else if (event instanceof CameraViewEvent.NavigateToError) {
            final CameraViewEvent.NavigateToError navigateToError =
                    (CameraViewEvent.NavigateToError) event;
            mFragment.findNavController().navigate(CameraFragmentDirections.toErrorFragment(
                    navigateToError.getErrorType(), navigateToError.getDocument()));
        } else if (event instanceof CameraViewEvent.NavigateToAnalysis) {
            final CameraViewEvent.NavigateToAnalysis navigateToAnalysis =
                    (CameraViewEvent.NavigateToAnalysis) event;
            mFragment.findNavController().navigate(CameraFragmentDirections.toAnalysisFragment(
                    navigateToAnalysis.getDocument(), navigateToAnalysis.getErrorMessage()));
        } else if (event instanceof CameraViewEvent.ProceedToMultiPageReview) {
            proceedToMultiPageReviewScreen(
                    ((CameraViewEvent.ProceedToMultiPageReview) event)
                            .getShouldScrollToLastPage());
        } else if (event instanceof CameraViewEvent.MultiPageStateChanged) {
            onMultiPageStateChanged(
                    ((CameraViewEvent.MultiPageStateChanged) event).getInMultiPageState());
        } else if (event instanceof CameraViewEvent.UpdatePhotoThumbnail) {
            updatePhotoThumbnail();
        } else if (event instanceof CameraViewEvent.ShowInvalidFileAlert) {
            showInvalidFileAlert(
                    ((CameraViewEvent.ShowInvalidFileAlert) event).getMessage());
        } else if (event instanceof CameraViewEvent.ShowMultiPageLimitError) {
            showMultiPageLimitError();
        } else if (event instanceof CameraViewEvent.RequestCheckImportedDocument) {
            final CameraViewEvent.RequestCheckImportedDocument requestCheck =
                    (CameraViewEvent.RequestCheckImportedDocument) event;
            fragmentListener.onCheckImportedDocument(
                    requestCheck.getDocument(), requestCheck.getCallback());
        } else if (event instanceof CameraViewEvent.NotifyError) {
            fragmentListener.onError(((CameraViewEvent.NotifyError) event).getError());
        } else if (event instanceof CameraViewEvent.NotifyExtractionsAvailable) {
            fragmentListener.onExtractionsAvailable(
                    ((CameraViewEvent.NotifyExtractionsAvailable) event).getExtractions());
        } else if (event instanceof CameraViewEvent.OpenCamera) {
            openCamera().thenAccept(unused -> {
                enableTapToFocus();
                initFlashButton();
            });
        } else if (event instanceof CameraViewEvent.ShowNoPermissionView) {
            showNoPermissionView();
        } else if (event instanceof CameraViewEvent.HideNoPermissionView) {
            hideNoPermissionView();
        }
    }

    private void restoreGenericErrorIfNeeded(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mViewModel.restoreGenericErrorState(
                    savedInstanceState.getBoolean(GENERIC_ERROR_SHOWING_STATE_KEY, false),
                    savedInstanceState.getString(GENERIC_ERROR_MESSAGE_KEY, ""),
                    savedInstanceState.getString(GENERIC_ERROR_TYPE_KEY, ""));
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
                            mViewModel.handlePaymentQRCodeData(paymentQRCodeData);
                            return null;
                        });

        mUnsupportedQRCodePopup =
                new QRCodePopup<>(mFragment, mCameraFrameWrapper, mActivityIndicatorBackground, null,
                        getHideQRCodeDetectedPopupDelayMs(), false, null, () -> {
                    mViewModel.onUnsupportedQRCodePopupHidden();
                    return null;
                });
        qrCodeEducationPopup = new QRCodeEducationPopup<>(view.findViewById(R.id.gc_qr_code_education_compose_view));
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public void onStart() {
        mViewModel.onStart();
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
                && !isOnlyQRCodeScanningEnabled()) {
            initIBANRecognizerFilter();
        }

        mViewModel.checkCameraPermission();

        setFileChooserFragmentResultListener();
    }

    private void setFileChooserFragmentResultListener() {
        mFragment.getParentFragmentManager().setFragmentResultListener(FileChooserFragment.REQUEST_KEY, mFragment.getViewLifecycleOwner(), (requestKey, result) -> {
            final FileChooserResult fileChooserResult = BundleCompat.getParcelable(result, FileChooserFragment.RESULT_KEY, FileChooserResult.class);
            if (fileChooserResult != null) {
                final FragmentActivity activity = mFragment.getActivity();
                if (activity != null) {
                    mViewModel.handleFileChooserResult(fileChooserResult, activity);
                }
            }
        });
    }

    private void showUnsupportedQRCodePopup() {
        if (mIbanDetectedTextView.getVisibility() != View.VISIBLE) {
            mUnsupportedQRCodePopup.show(null);
            mViewModel.onUnsupportedQRCodePopupShown();
        }
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
        mViewModel.initMultiPageDocument();
    }

    private void onMultiPageStateChanged(final boolean inMultiPageState) {
        if (inMultiPageState) {
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
                                    mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_OPEN_FAILED,
                                            "Failed to open camera", cameraException);
                                    break;
                                case NO_PREVIEW:
                                    mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_NO_PREVIEW,
                                            "Failed to open camera", cameraException);
                                    break;
                                case SHOT_FAILED:
                                    mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                            "Failed to open camera", cameraException);
                                    break;
                            }
                        } else {
                            mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_UNKNOWN,
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
        outState.putBoolean(IN_MULTI_PAGE_STATE_KEY, mViewModel.isInMultiPageState());
        outState.putBoolean(IS_FLASH_ENABLED_KEY, mIsFlashEnabled);
        outState.putBoolean(IS_NOT_AVAILABLE_DETECTION_POPUP_SHOWED_KEY, mIsDetectionErrorPopupShowed);
        outState.putString(GENERIC_ERROR_MESSAGE_KEY, mViewModel.getCurrentGenericErrorMessage());
        outState.putBoolean(GENERIC_ERROR_SHOWING_STATE_KEY, mViewModel.isGenericErrorShowing());
        outState.putString(GENERIC_ERROR_TYPE_KEY, mViewModel.getGenericErrorType());
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
        mViewModel.cancelImportUrisAsyncTask();
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
                } else if (mViewModel.getMultiPageDocument() != null && !mViewModel.getMultiPageDocument().getDocuments().isEmpty()) {
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
                        if (ContextHelper.isPortraitOrientation(mFragment.getActivity()))
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_title_camera));
                        else
                            injectedViewAdapter.setTitle(mFragment.getActivity().getString(R.string.gc_camera_top_bar_title_landscape));
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
                        boolean isEmpty = mViewModel.getMultiPageDocument() == null || mViewModel.getMultiPageDocument().getDocuments().isEmpty();
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
            mLoadingIndicator.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(GiniCapture.getInstance().internal().getLoadingIndicatorAdapterInstance(), injectedViewAdapter -> {
            }));
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
        if (!mViewModel.interfaceHidden && isDocumentImportEnabled(activity)) {
            mImportDocumentButtonEnabled = true;
            mImportButtonGroup.setVisibility(View.VISIBLE);
            showImportDocumentButtonAnimated();
        }
    }

    private boolean isDocumentImportEnabled(@NonNull final Activity activity) {
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
                                add(new UserAnalyticsEventProperty.DocumentPageNumber(mViewModel.getMultiPageDocument().getDocuments().size()));
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
        if (mViewModel.exceedsMultiPageLimit()) {
            mViewModel.showMultiPageLimitError();
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

    private void showFileChooser() {
        LOG.info("Importing document");
        if (mViewModel.exceedsMultiPageLimit()) {
            mViewModel.showMultiPageLimitError();
            return;
        }
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final DocumentImportEnabledFileTypes enabledFileTypes;
        if (mViewModel.isInMultiPageState()) {
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

        if (mViewModel.getMultiPageDocument() == null) {
            return;
        }
        final List<ImageDocument> documents = mViewModel.getMultiPageDocument().getDocuments();
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

    private void showInvalidFileAlert(final String message) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        mFragment.showAlertDialog(message,
                activity.getString(R.string.gc_document_import_close_error),
                (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    mViewModel.resetGenericDialogState();
                },
                null,
                null,
                (dialogInterface -> {
                    mViewModel.resetGenericDialogState();
                }));
    }

    @UiThread
    private void onPictureTaken(final Photo photo, final Throwable throwable) {
        if (throwable != null) {
            mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED, "Failed to take picture",
                    throwable);
            mCameraController.startPreview();
            setmIsTakingPicture(false);
        } else {
            if (photo != null) {
                LOG.info("Picture taken");
                mViewModel.onPictureTaken();
                showActivityIndicatorAndDisableInteraction();
                photo.edit()
                        .crop(mCameraPreview, getRectForCroppingFromImageFrame())
                        .compressByDefault().applyAsync(new PhotoEdit.PhotoEditCallback() {
                            @Override
                            public void onDone(@NonNull final Photo result) {
                                hideActivityIndicatorAndEnableInteraction();
                                final ImageDocument document = createSavedDocument(result);
                                if (document == null) {
                                    mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                            "Failed to take picture: could not save picture to disk",
                                            null);
                                    mCameraController.startPreview();
                                    setmIsTakingPicture(false);
                                    return;
                                }
                                final CameraViewModel.CapturedImageOutcome outcome =
                                        mViewModel.onImageCaptured(document);
                                switch (outcome) {
                                    case MULTI_PAGE_ADDED:
                                        showCapturedImageThumbnail(result, document);
                                        proceedToMultiPageReviewScreen(true);
                                        break;
                                    case MULTI_PAGE_CREATED:
                                        showCapturedImageThumbnail(result, document);
                                        proceedToMultiPageReviewScreen(true);
                                        setmIsTakingPicture(false);
                                        mCameraController.startPreview();
                                        break;
                                    case SINGLE_PAGE:
                                        proceedToMultiPageReviewScreen(false);
                                        setmIsTakingPicture(false);
                                        mCameraController.startPreview();
                                        break;
                                }
                            }

                            @Override
                            public void onFailed() {
                                hideActivityIndicatorAndEnableInteraction();
                                mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                                        "Failed to take picture: picture compression failed", null);
                                mCameraController.startPreview();
                                setmIsTakingPicture(false);
                            }
                        });
            } else {
                mViewModel.handleError(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED,
                        "Failed to take picture: no picture from the camera", null);
                mCameraController.startPreview();
                setmIsTakingPicture(false);
            }
        }
    }

    private void showCapturedImageThumbnail(@NonNull final Photo photo,
                                            @NonNull final ImageDocument document) {
        mPhotoThumbnail.setImage(new PhotoThumbnail.ThumbnailBitmap(photo.getBitmapPreview(),
                document.getRotationForDisplay()));
        if (mViewModel.getMultiPageDocument() != null) {
            mPhotoThumbnail.setImageCount(mViewModel.getMultiPageDocument().getDocuments().size());
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
                    dialogInterface.dismiss();
                    mViewModel.resetGenericDialogState();
                }, activity.getString(R.string.gc_document_error_multi_page_limit_cancel_button),
                (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    mViewModel.resetGenericDialogState();
                }, (dialogInterface) -> {
                    dialogInterface.dismiss();
                    mViewModel.resetGenericDialogState();
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
        if (!mViewModel.interfaceHidden) {
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

    private void handleIBANsDetected(List<String> ibans) {
        if (!ibans.isEmpty() && !isPaymentQRCodeDetectionInProgress()) {
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
        return new CameraXController(activity);
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

    public void hideImageCorners() {
        hideDocumentCornerGuidesAnimated();
    }
}
