package net.gini.android.capture.review;

import static net.gini.android.capture.GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE;
import static net.gini.android.capture.internal.network.NetworkRequestsManager.isCancellation;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.internal.util.FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.ortiz.touch.TouchImageView;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.cache.PhotoMemoryCache;
import net.gini.android.capture.internal.camera.photo.ParcelableMemoryCache;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.camera.photo.PhotoEdit;
import net.gini.android.capture.internal.camera.photo.PhotoFactoryDocumentAsyncTask;
import net.gini.android.capture.internal.network.NetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.FileImportHelper;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.tracking.ReviewScreenEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Internal use only.
 *
 * @suppress
 */
class ReviewFragmentImpl implements ReviewFragmentInterface {

    private static final String PHOTO_KEY = "PHOTO_KEY";
    private static final String DOCUMENT_KEY = "DOCUMENT_KEY";
    private static final String PARCELABLE_MEMORY_CACHE_TAG = "REVIEW_FRAGMENT";
    private static final Logger LOG = LoggerFactory.getLogger(ReviewFragmentImpl.class);

    private static final ReviewFragmentListener NO_OP_LISTENER = new ReviewFragmentListener() {
        @Override
        public void onError(@NonNull final GiniCaptureError error) {
        }

        @Override
        public void onProceedToAnalysisScreen(@NonNull final Document document,
                final String errorMessage) {

        }
    };

    private FrameLayout mLayoutDocumentContainer;
    private TouchImageView mImageDocument;
    @VisibleForTesting
    ImageButton mButtonRotate;
    private ImageButton mButtonNext;
    private ProgressBar mActivityIndicator;

    private final FragmentImplCallback mFragment;
    @VisibleForTesting
    Photo mPhoto;
    private ImageDocument mDocument;
    private ReviewFragmentListener mListener = NO_OP_LISTENER;
    private boolean mDocumentWasUploaded;
    private int mCurrentRotation;
    private boolean mNextClicked;
    private boolean mStopped;
    private String mDocumentAnalysisErrorMessage;

    ReviewFragmentImpl(@NonNull final FragmentImplCallback fragment,
            @NonNull final Document document) {
        mFragment = fragment;
        if (!document.isReviewable()) {
            throw new IllegalArgumentException(
                    "Non reviewable documents must be passed directly to the Analysis Screen. You"
                            + " can use Document#isReviewable() to check whether you can use it "
                            + "with the Review Screen or have to pass it directly to the Analysis"
                            + " Screen.");
        }
        if (document.getType() != Document.Type.IMAGE) {
            throw new IllegalArgumentException("Only Documents with type IMAGE allowed");
        }
        mDocument = (ImageDocument) document;
        // Tag the documents to be able to clean up the parcelled data
        mDocument.setParcelableMemoryCacheTag(PARCELABLE_MEMORY_CACHE_TAG);
    }

    @VisibleForTesting
    TouchImageView getImageDocument() {
        return mImageDocument;
    }

    @Override
    public void setListener(@NonNull final ReviewFragmentListener listener) {
        mListener = listener;
    }

    public void onCreate(@Nullable final Bundle savedInstanceState) {
        forcePortraitOrientationOnPhones(mFragment.getActivity());
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }
    }

    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_review, container, false);
        bindViews(view);
        setInputHandlers();
        return view;
    }

    public void onStart() {
        checkGiniCaptureInstance();
        mNextClicked = false;
        mStopped = false;
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        showAlertIfOpenWithDocumentAndAppIsDefault(activity, mDocument,
                new FileImportHelper.ShowAlertCallback() {
                    @Override
                    public void showAlertDialog(@NonNull final String message,
                            @NonNull final String positiveButtonTitle,
                            @NonNull final DialogInterface.OnClickListener
                                    positiveButtonClickListener,
                            @Nullable final String negativeButtonTitle,
                            @Nullable final DialogInterface.OnClickListener
                                    negativeButtonClickListener,
                            @Nullable final DialogInterface.OnCancelListener cancelListener) {
                        mFragment.showAlertDialog(message, positiveButtonTitle,
                                positiveButtonClickListener, negativeButtonTitle,
                                negativeButtonClickListener, cancelListener);
                    }
                })
                .thenRun(new Runnable() {
                    @Override
                    public void run() {
                        handleOnStart();
                    }
                });
    }

    private void checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            mListener.onError(new GiniCaptureError(MISSING_GINI_CAPTURE_INSTANCE,
                    "Missing GiniCapture instance. It was not created or there was an application process restart."));
        }
    }

    private void handleOnStart() {
        if (mPhoto == null) {
            final Activity activity = mFragment.getActivity();
            if (activity == null) {
                return;
            }
            showActivityIndicatorAndDisableButtons();
            LOG.debug("Loading document data");
            mDocument.loadData(activity, new AsyncCallback<byte[], Exception>() {
                @Override
                public void onSuccess(final byte[] result) {
                    LOG.debug("Document data loaded");
                    if (mNextClicked || mStopped) {
                        return;
                    }
                    createPhoto();
                }

                @Override
                public void onError(final Exception exception) {
                    LOG.error("Failed to load document data", exception);
                    if (mNextClicked || mStopped) {
                        return;
                    }
                    hideActivityIndicatorAndEnableButtons();
                    ErrorLogger.log(new ErrorLog("Failed to load document data", exception));
                    mListener.onError(new GiniCaptureError(GiniCaptureError.ErrorCode.REVIEW,
                            "An error occurred while loading the document."));
                }

                @Override
                public void onCancelled() {
                    // Not used
                }
            });
        } else {
            observeViewTree();
            LOG.info("Should analyze document");
            shouldAnalyzeDocument();
        }
    }

    private void shouldAnalyzeDocument() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final GiniCaptureDocument document = DocumentFactory.newImageDocumentFromPhotoAndDocument(
                mPhoto,
                mDocument);
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.upload(activity, document)
                        .handle(new CompletableFuture.BiFun<NetworkRequestResult<
                                GiniCaptureDocument>, Throwable, Void>() {
                            @Override
                            public Void apply(
                                    final NetworkRequestResult<GiniCaptureDocument> requestResult,
                                    final Throwable throwable) {
                                if (throwable != null && !isCancellation(throwable)) {
                                    handleAnalysisError(throwable);
                                } else if (requestResult != null) {
                                    mDocumentWasUploaded = true;
                                }
                                return null;
                            }
                        });
            }
        }
    }

    private void handleAnalysisError(@NonNull final Throwable throwable) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().setReviewScreenAnalysisError(throwable);
        }
        mDocumentAnalysisErrorMessage = activity.getString(R.string.gc_document_analysis_error);
    }

    private void createPhoto() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (GiniCapture.hasInstance()) {
            LOG.debug("Loading Photo from memory cache");
            final PhotoMemoryCache photoMemoryCache =
                    GiniCapture.getInstance().internal().getPhotoMemoryCache();
            photoMemoryCache.get(activity, mDocument, new AsyncCallback<Photo, Exception>() {
                @Override
                public void onSuccess(final Photo result) {
                    LOG.debug("Photo loaded");
                    photoCreated(result);
                }

                @Override
                public void onError(final Exception exception) {
                    LOG.error("Failed to load a Photo for the ImageDocument");
                    ErrorLogger.log(new ErrorLog("Failed to load Photo for ImageDocument", exception));
                    photoCreationFailed();
                }

                @Override
                public void onCancelled() {
                    // Not used
                }
            });
        } else {
            LOG.debug("Instantiating a Photo from the Document");
            final PhotoFactoryDocumentAsyncTask asyncTask = new PhotoFactoryDocumentAsyncTask(
                    new AsyncCallback<Photo, Exception>() {
                        @Override
                        public void onSuccess(final Photo result) {
                            LOG.debug("Photo instantiated");
                            photoCreated(result);
                        }

                        @Override
                        public void onError(final Exception exception) {
                            LOG.error("Failed to instantiate a Photo from the ImageDocument");
                            ErrorLogger.log(new ErrorLog("Failed to instantiate a Photo from the ImageDocument", exception));
                            photoCreationFailed();
                        }

                        @Override
                        public void onCancelled() {
                            // Not used
                        }
                    });
            asyncTask.execute(mDocument);
        }
    }

    private void photoCreated(final Photo result) {
        if (mNextClicked || mStopped) {
            return;
        }
        mPhoto = result;
        mPhoto.setParcelableMemoryCacheTag(PARCELABLE_MEMORY_CACHE_TAG);
        mCurrentRotation = mDocument.getRotationForDisplay();
        if (!mDocument.getSource().equals(Document.Source.newCameraSource())) {
            LOG.debug("Compressing Photo");
            applyCompressionToPhoto(new PhotoEdit.PhotoEditCallback() {
                @Override
                public void onDone(@NonNull final Photo photo) {
                    LOG.debug("Photo compressed");
                    photoReady();
                }

                @Override
                public void onFailed() {
                    LOG.error("Failed to compress the Photo");
                    ErrorLogger.log(new ErrorLog("Image compression failed", null));
                    if (mNextClicked || mStopped) {
                        return;
                    }
                    mListener.onError(
                            new GiniCaptureError(GiniCaptureError.ErrorCode.REVIEW,
                                    "An error occurred while compressing the jpeg."));
                }
            });
        } else {
            photoReady();
        }
    }

    private void applyCompressionToPhoto(@NonNull final PhotoEdit.PhotoEditCallback callback) {
        if (mPhoto == null) {
            return;
        }
        LOG.debug("Compressing the Photo");
        mPhoto.edit()
                .compressByDefault()
                .applyAsync(callback);
    }

    private void photoCreationFailed() {
        if (mNextClicked || mStopped) {
            return;
        }
        mListener.onError(new GiniCaptureError(GiniCaptureError.ErrorCode.REVIEW,
                "An error occurred while instantiating a Photo from the ImageDocument."));
    }

    private void photoReady() {
        if (mNextClicked || mStopped) {
            return;
        }
        hideActivityIndicatorAndEnableButtons();
        observeViewTree();
        LOG.info("Should analyze document");
        shouldAnalyzeDocument();
    }

    private void showActivityIndicatorAndDisableButtons() {
        if (mActivityIndicator == null) {
            return;
        }
        mActivityIndicator.setVisibility(View.VISIBLE);
        disableNextButton();
        disableRotateButton();
    }

    private void hideActivityIndicatorAndEnableButtons() {
        if (mActivityIndicator == null) {
            return;
        }
        mActivityIndicator.setVisibility(View.GONE);
        enableNextButton();
        enableRotateButton();
    }

    private void disableNextButton() {
        if (mButtonNext == null) {
            return;
        }
        mButtonNext.setEnabled(false);
        mButtonNext.setAlpha(0.5f);
    }

    private void enableNextButton() {
        if (mButtonNext == null) {
            return;
        }
        mButtonNext.setEnabled(true);
        mButtonNext.setAlpha(1f);
    }

    private void disableRotateButton() {
        if (mButtonRotate == null) {
            return;
        }
        mButtonRotate.setEnabled(false);
        mButtonRotate.setAlpha(0.5f);
    }

    private void enableRotateButton() {
        if (mButtonRotate == null) {
            return;
        }
        mButtonRotate.setEnabled(true);
        mButtonRotate.setAlpha(1f);
    }

    private void showDocument() {
        if (mPhoto == null) {
            return;
        }
        mImageDocument.setImageBitmap(mPhoto.getBitmapPreview());
    }

    void onStop() {
        mStopped = true;
    }

    void onSaveInstanceState(final Bundle outState) {
        // Remove previously saved data from the memory cache to keep only the data saved in the
        // current invocation
        clearParcelableMemoryCache();
        outState.putParcelable(PHOTO_KEY, mPhoto);
        outState.putParcelable(DOCUMENT_KEY, mDocument);
    }

    private void clearParcelableMemoryCache() {
        ParcelableMemoryCache.getInstance().removeEntriesWithTag(PARCELABLE_MEMORY_CACHE_TAG);
    }

    public void onDestroy() {
        if (!mNextClicked) {
            deleteUploadedDocument();
        }
        final Activity activity = mFragment.getActivity();
        if (activity != null && activity.isFinishing()) {
            // Remove data from the memory cache. The data had been added in onSaveInstanceState()
            // and also when the document in the arguments was automatically parcelled when the
            // activity was stopped
            clearParcelableMemoryCache();
        }
        mPhoto = null; // NOPMD
        mDocument = null; // NOPMD
    }

    private void bindViews(@NonNull final View view) {
        mLayoutDocumentContainer = view.findViewById(R.id.gc_layout_document_container);
        mImageDocument = view.findViewById(R.id.gc_image_document);
        mButtonRotate = view.findViewById(R.id.gc_button_rotate);
        mButtonNext = view.findViewById(R.id.gc_button_next);
        mActivityIndicator = view.findViewById(R.id.gc_activity_indicator);
    }

    private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        LOG.debug("Restoring saved state");
        mPhoto = savedInstanceState.getParcelable(PHOTO_KEY);
        mDocument = savedInstanceState.getParcelable(DOCUMENT_KEY);
        if (mDocument == null) {
            throw new IllegalStateException(
                    "Missing required instances for restoring saved instance state.");
        }
        mCurrentRotation = mDocument.getRotationForDisplay();
    }

    private void setInputHandlers() {
        mButtonRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onRotateClicked();
            }
        });
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onNextClicked();
            }
        });
    }

    private void observeViewTree() {
        final View view = mFragment.getView();
        if (view == null) {
            return;
        }
        LOG.debug("Observing the view layout");
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        onViewLayoutFinished();
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                });
        view.requestLayout();
    }

    private void onViewLayoutFinished() {
        LOG.debug("View layout finished");
        rotateDocumentForDisplay();
        showDocument();
    }

    private void rotateDocumentForDisplay() {
        rotateImageView(mDocument.getRotationForDisplay(), false);
    }

    private void onRotateClicked() {
        mCurrentRotation += 90;
        rotateImageView(mCurrentRotation, true);
        if (GiniCapture.hasInstance()
                && GiniCapture.getInstance().internal().getNetworkRequestsManager() != null) {
            LOG.debug("Only the preview was rotated");
            mDocument.setRotationForDisplay(mCurrentRotation);
            mDocument.updateRotationDeltaBy(90);
            mPhoto.setRotationForDisplay(mCurrentRotation);
            mPhoto.updateRotationDeltaBy(90);
        }
    }

    private void deleteUploadedDocument() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(mDocument);
                networkRequestsManager.delete(mDocument);
            }
        }
    }

    @VisibleForTesting
    void onNextClicked() {
        trackReviewScreenEvent(ReviewScreenEvent.NEXT);
        mNextClicked = true;
        LOG.debug("Document wasn't modified");
        if (!mDocumentWasUploaded || !TextUtils.isEmpty(mDocumentAnalysisErrorMessage)) {
            LOG.debug("Document wasn't analyzed");
            proceedToAnalysisScreen();
        } else {
            LOG.debug("Document was analyzed");
            LOG.info("Document reviewed and analyzed");
            // Photo was not modified and has been analyzed, client should show extraction
            // results
            documentReviewedAndUploaded();
        }
    }

    private void documentReviewedAndUploaded() {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        final GiniCaptureDocument document = DocumentFactory.newImageDocumentFromPhotoAndDocument(
                mPhoto,
                mDocument);
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager =
                    GiniCapture.getInstance().internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                mListener.onProceedToAnalysisScreen(document, mDocumentAnalysisErrorMessage);
            }
        }
    }

    private void proceedToAnalysisScreen() {
        LOG.info("Proceed to Analysis Screen");
        final GiniCaptureDocument document = DocumentFactory.newImageDocumentFromPhotoAndDocument(
                mPhoto,
                mDocument);
        if (GiniCapture.hasInstance()) {
            mListener.onProceedToAnalysisScreen(document, mDocumentAnalysisErrorMessage);
        }
    }

    private void rotateImageView(final int degrees, final boolean animated) {
        LOG.info("Rotate ImageView {} degrees animated {}", degrees, animated);
        if (degrees == 0) {
            return;
        }

        mImageDocument.resetZoom();

        final ValueAnimator widthAnimation;
        final ValueAnimator heightAnimation;
        if (degrees % 360 == 90 || degrees % 360 == 270) {
            LOG.debug("ImageView width needs to fit container height");
            LOG.debug("ImageView height needs fit container width");
            widthAnimation = ValueAnimator.ofInt(mImageDocument.getWidth(),
                    mLayoutDocumentContainer.getHeight());
            heightAnimation = ValueAnimator.ofInt(mImageDocument.getHeight(),
                    mLayoutDocumentContainer.getWidth());
        } else {
            LOG.debug("ImageView width needs to fit container width");
            LOG.debug("ImageView height needs to fit container height");
            widthAnimation = ValueAnimator.ofInt(mImageDocument.getWidth(),
                    mLayoutDocumentContainer.getWidth());
            heightAnimation = ValueAnimator.ofInt(mImageDocument.getHeight(),
                    mLayoutDocumentContainer.getHeight());
        }

        widthAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                final int width = (int) valueAnimator.getAnimatedValue();
                final FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) mImageDocument.getLayoutParams();
                layoutParams.width = width;
                mImageDocument.requestLayout();
            }
        });
        heightAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                final int height = (int) valueAnimator.getAnimatedValue();
                final FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) mImageDocument.getLayoutParams();
                layoutParams.height = height;
                mImageDocument.requestLayout();
            }
        });

        final ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(mImageDocument, "rotation",
                degrees);

        if (!animated) {
            widthAnimation.setDuration(0);
            heightAnimation.setDuration(0);
            rotateAnimation.setDuration(0);
        }

        widthAnimation.start();
        heightAnimation.start();
        rotateAnimation.start();
    }
}
