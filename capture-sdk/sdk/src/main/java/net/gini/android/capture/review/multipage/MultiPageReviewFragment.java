package net.gini.android.capture.review.multipage;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;
import static net.gini.android.capture.GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE;
import static net.gini.android.capture.document.GiniCaptureDocumentError.ErrorCode.FILE_VALIDATION_FAILED;
import static net.gini.android.capture.document.GiniCaptureDocumentError.ErrorCode.UPLOAD_FAILED;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.internal.util.FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault;
import static net.gini.android.capture.review.multipage.previews.PreviewFragment.ErrorButtonAction.DELETE;
import static net.gini.android.capture.review.multipage.previews.PreviewFragment.ErrorButtonAction.RETRY;
import static net.gini.android.capture.review.multipage.thumbnails.ThumbnailsAdapter.getNewPositionAfterDeletion;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureDocumentError;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.internal.network.NetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.ActivityHelper;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.internal.util.FileImportHelper;
import net.gini.android.capture.review.multipage.previews.PreviewFragment;
import net.gini.android.capture.review.multipage.previews.PreviewFragmentListener;
import net.gini.android.capture.review.multipage.previews.PreviewsAdapter;
import net.gini.android.capture.review.multipage.previews.PreviewsAdapterListener;
import net.gini.android.capture.review.multipage.previews.PreviewsPageChangeHandler;
import net.gini.android.capture.review.multipage.previews.PreviewsPageChangeListener;
import net.gini.android.capture.review.multipage.thumbnails.ThumbnailsAdapter;
import net.gini.android.capture.review.multipage.thumbnails.ThumbnailsAdapterListener;
import net.gini.android.capture.review.multipage.thumbnails.ThumbnailsTouchHelperCallback;
import net.gini.android.capture.review.zoom.ZoomInPreviewActivity;
import net.gini.android.capture.tracking.ReviewScreenEvent;
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 07.05.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * <h3>Component API</h3>
 *
 * <p> When you use the Compontent API and have enabled the multi-page feature, the {@code
 * MultiPageReviewFragment} displays the photographed or imported images and allows the user to
 * review them by checking the order, sharpness, quality and orientation of the images. The user can
 * correct the order by dragging the thumbnails of the images and can also correct the orientation
 * by rotating the images.
 *
 * <p> <b>Important:</b>
 *
 * <ul>
 *
 * <li> A {@link GiniCapture} instance is required to use the {@code MultiPageReviewFragment}
 *
 * <li> Your Activity hosting this Fragment must extend the {@link androidx.appcompat.app.AppCompatActivity}
 * and use an AppCompat Theme.
 *
 * </ul>
 *
 * <p> Include the {@code MultiPageReviewFragment} into your layout by using the {@link
 * MultiPageReviewFragment#createInstance()} factory method to create an instance and display it
 * using the {@link androidx.fragment.app.FragmentManager}.
 *
 * <p> A {@link MultiPageReviewFragmentListener} instance must be available until the {@code
 * MultiPageReviewFragment} is attached to an activity. Failing to do so will throw an exception.
 * The listener instance can be provided either implicitly by making the hosting Activity implement
 * the {@link MultiPageReviewFragmentListener} interface or explicitly by setting the listener using
 * {@link MultiPageReviewFragment#setListener(MultiPageReviewFragmentListener)}.
 *
 * <p> Your Activity is automatically set as the listener in {@link MultiPageReviewFragment#onCreate(Bundle)}.
 *
 * <h3>Customizing the Multi-Page Review Screen</h3>
 * <p>
 * See the {@link MultiPageReviewActivity} for details.
 */
public class MultiPageReviewFragment extends Fragment implements MultiPageReviewFragmentInterface,
        PreviewFragmentListener, FragmentImplCallback {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";
    private static final Logger LOG = LoggerFactory.getLogger(MultiPageReviewFragment.class);

    @VisibleForTesting
    Map<String, Boolean> mDocumentUploadResults = new HashMap<>();
    @VisibleForTesting
    ImageMultiPageDocument mMultiPageDocument;
    private MultiPageReviewFragmentListener mListener;
    private PreviewFragmentListener mPreviewFragmentListener;
    private ViewPager2 mPreviewsPager2;
    private PreviewsPager2Adapter mPreviewsAdapter2;
    private AppCompatButton mButtonNext;
    private LinearLayout mAddPages;
    private TabLayout mTabIndicator;
    private InjectedViewContainer<NavigationBarTopAdapter> mTopAdapterInjectedViewContainer;
    private boolean mNextClicked;
    private boolean mPreviewsShown;
    private ViewPager2.OnPageChangeCallback mPager2PageCallback;

    public static MultiPageReviewFragment createInstance() {
        return new MultiPageReviewFragment();
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forcePortraitOrientationOnPhones(getActivity());
        initListener();
        if (!GiniCapture.hasInstance()) {
            mListener.onError(new GiniCaptureError(MISSING_GINI_CAPTURE_INSTANCE,
                    "Missing GiniCapture instance. It was not created or there was an application process restart."));
        } else {
            initMultiPageDocument();
        }
    }

    @Override
    public void showAlertDialog(@NonNull final String message,
                                @NonNull final String positiveButtonTitle,
                                @NonNull final DialogInterface.OnClickListener positiveButtonClickListener,
                                @Nullable final String negativeButtonTitle,
                                @Nullable final DialogInterface.OnClickListener negativeButtonClickListener,
                                @Nullable final DialogInterface.OnCancelListener cancelListener) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        AlertDialogHelperCompat.showAlertDialog(activity, message, positiveButtonTitle,
                positiveButtonClickListener, negativeButtonTitle, negativeButtonClickListener,
                cancelListener);
    }

    private void initMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            mMultiPageDocument = GiniCapture.getInstance().internal()
                    .getImageMultiPageDocumentMemoryStore().getMultiPageDocument();
        }
        if (mMultiPageDocument == null) {
            throw new IllegalStateException(
                    "MultiPageReviewFragment requires an ImageMultiPageDocuments.");
        }
        initUploadResults();
    }

    private void initUploadResults() {
        for (final ImageDocument imageDocument : mMultiPageDocument.getDocuments()) {
            mDocumentUploadResults.put(imageDocument.getId(), false);
        }
    }

    private void initListener() {
        if (getActivity() instanceof MultiPageReviewFragmentListener) {
            mListener = (MultiPageReviewFragmentListener) getActivity();
        } else if (mListener == null) {
            throw new IllegalStateException(
                    "MultiPageReviewFragmentListener not set. "
                            + "You can set it with MultiPageReviewFragment#setListener() or "
                            + "by making the host activity implement the MultiPageReviewFragmentListener.");
        }
        mPreviewFragmentListener = this;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_multi_page_review, container,
                false);
        bindViews(view);
        setInputHandlers();
        setupTopNavigationBar();
        if (mMultiPageDocument != null) {
            updateNextButtonVisibility();
            initViewPager2();
        }
        return view;
    }

    private void delayWithBlueRect() {
        mPreviewsPager2.post(() -> showHideBlueRect(View.VISIBLE));
    }

    private void showHideBlueRect(int visibility) {
        if (mPreviewsAdapter2.getItemCount() > 0 && selectedFragment() != null)
            Objects.requireNonNull(selectedFragment()).manageSelectionRect(visibility);
    }

    private void shouldIndicatorBeVisible() {
        mTabIndicator.setVisibility(mPreviewsAdapter2.getItemCount() <= 1 ? View.INVISIBLE : View.VISIBLE);
    }

    private PreviewFragment selectedFragment() {
        Fragment current = mPreviewsAdapter2.getCurrentFragment(mPreviewsPager2.getCurrentItem());

        if (current != null) {
            return (PreviewFragment) current;
        }

        return null;
    }

    private void initViewPager2() {

        if (getActivity() == null)
            return;

        final PreviewsAdapterListener previewsAdapterListener = documentError -> {
            if (documentError.getErrorCode() == UPLOAD_FAILED) {
                return RETRY;
            } else if (documentError.getErrorCode() == FILE_VALIDATION_FAILED) {
                return DELETE;
            }
            return null;
        };

        mPreviewsPager2.setClipChildren(false);
        mPreviewsPager2.setClipToPadding(false);
        mPreviewsPager2.setOffscreenPageLimit(3);

        mPreviewsAdapter2 = new PreviewsPager2Adapter(getActivity(), mMultiPageDocument, previewsAdapterListener, mPreviewFragmentListener);
        mPreviewsPager2.setAdapter(mPreviewsAdapter2);

        mPreviewsPager2.setPageTransformer(setupTransformer());

        TabLayoutMediator mediator = new TabLayoutMediator(mTabIndicator, mPreviewsPager2, true, (tab, position) -> {

        });
        mediator.attach();

        mPager2PageCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager.SCROLL_STATE_IDLE)
                    showHideBlueRect(View.VISIBLE);
                else showHideBlueRect(View.INVISIBLE);
            }
        };

        mPreviewsPager2.registerOnPageChangeCallback(mPager2PageCallback);

        delayWithBlueRect();
        shouldIndicatorBeVisible();
    }

    private ViewPager2.PageTransformer setupTransformer() {

        int pageMarginPx = (ContextHelper.isTablet(requireContext())) ? getResources().getDimensionPixelOffset(R.dimen.large)
                : getResources().getDimensionPixelOffset(R.dimen.large);
        int offsetPx = (ContextHelper.isTablet(requireContext())) ? getResources().getDimensionPixelOffset(R.dimen.xlarge)
                : getResources().getDimensionPixelOffset(R.dimen.small);

        return (page, position) -> {
            ViewPager2 viewPager = (ViewPager2) page.getParent().getParent();
            float offset = position * -(2 * offsetPx + pageMarginPx);
            if (viewPager.getOrientation() == ORIENTATION_HORIZONTAL) {
                if (ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    page.setTranslationX(-offset);
                } else {
                    page.setTranslationX(offset);
                }
            } else {
                page.setTranslationY(offset);
            }
        };
    }

    private void bindViews(final View view) {
        mButtonNext = view.findViewById(R.id.gc_button_next);
        mTabIndicator = view.findViewById(R.id.gc_tab_indicator);
        mTopAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
        mAddPages = view.findViewById(R.id.gc_add_pages_wrapper);
        mPreviewsPager2 = view.findViewById(R.id.gc_view_pager2);

    }

    private void setupTopNavigationBar() {
        if (GiniCapture.hasInstance()) {
            mTopAdapterInjectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getNavigationBarTopAdapter());

            if (mTopAdapterInjectedViewContainer.getInjectedViewAdapter() == null)
                return;

            if (this.getActivity() == null)
                return;

            mTopAdapterInjectedViewContainer.getInjectedViewAdapter().setTitle(getString(R.string.gc_review));

            mTopAdapterInjectedViewContainer.getInjectedViewAdapter().setNavButtonType(NavButtonType.CLOSE);

            mTopAdapterInjectedViewContainer.getInjectedViewAdapter().setOnNavButtonClickListener(v -> {
                if (MultiPageReviewFragment.this.getActivity() != null) {
                    MultiPageReviewFragment.this.getActivity().onBackPressed();
                }
            });

        }
    }

    private void setInputHandlers() {
        mButtonNext.setOnClickListener(v -> onNextButtonClicked());

        if (GiniCapture.hasInstance()) {
            mAddPages.setVisibility(GiniCapture.getInstance().isMultiPageEnabled() ? View.VISIBLE : View.GONE);
        }

        mAddPages.setOnClickListener(v -> mListener.onReturnToCameraScreen());
    }


    private void deleteDocumentAndUpdateUI(@NonNull final ImageDocument document) {
        if (mMultiPageDocument.getDocuments().size() == 1) {
            final FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            if (mMultiPageDocument.getImportMethod() == Document.ImportMethod.OPEN_WITH) {
                new AlertDialog.Builder(activity)
                        .setMessage(
                                R.string.gc_multi_page_review_file_import_delete_last_page_dialog_message)
                        .setPositiveButton(
                                R.string.gc_multi_page_review_file_import_delete_last_page_dialog_positive_button,
                                (dialog, which) -> mListener.onImportedDocumentReviewCancelled())
                        .setNegativeButton(
                                R.string.gc_multi_page_review_file_import_delete_last_page_dialog_negative_button,
                                null)
                        .create().show();
            } else {
                doDeleteDocumentAndUpdateUI(document);
                mListener.onReturnToCameraScreen();
            }
        } else {
            doDeleteDocumentAndUpdateUI(document);
        }
    }

    private void doDeleteDocumentAndUpdateUI(@NonNull final ImageDocument document) {
        final int deletedPosition = mMultiPageDocument.getDocuments().indexOf(document);

        deleteDocument(document);

        final int nrOfDocuments = mMultiPageDocument.getDocuments().size();
        final int newPosition = getNewPositionAfterDeletion(deletedPosition, nrOfDocuments);

        mPreviewsAdapter2.notifyItemRemoved(deletedPosition);
        mPreviewsAdapter2.removeFragmentFromTheList(deletedPosition);
        mPreviewsPager2.post(() -> mPreviewsPager2.requestTransform());

        delayWithBlueRect();

        updateNextButtonVisibility();

        shouldIndicatorBeVisible();
    }

    private void deleteDocument(@NonNull final ImageDocument document) {
        deleteFromMultiPageDocument(document);
        deleteFromCaches(document);
        deleteFromDisk(document);
        deleteFromGiniApi(document);
        mDocumentUploadResults.remove(document.getId());
    }

    private void deleteFromMultiPageDocument(@NonNull final ImageDocument document) {
        mMultiPageDocument.getDocuments().remove(document);
        if (mMultiPageDocument.getDocuments().size() == 0
                && GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
        }
    }

    private void deleteFromGiniApi(final ImageDocument document) {
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager =
                    GiniCapture.getInstance().internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.delete(document);
            }
        }
    }

    private void deleteFromDisk(final ImageDocument document) {
        if (GiniCapture.hasInstance()) {
            final Uri uri = document.getUri();
            if (uri != null) {
                GiniCapture.getInstance().internal().getImageDiskStore().delete(uri);
            }
        }
    }

    @NonNull
    private void deleteFromCaches(final ImageDocument document) {
        if (GiniCapture.hasInstance()) {
            final GiniCapture.Internal gcInternal = GiniCapture.getInstance().internal();
            gcInternal.getDocumentDataMemoryCache().invalidate(document);
            gcInternal.getPhotoMemoryCache().invalidate(document);
        }
    }

    private void updateNextButtonVisibility() {
        if (mMultiPageDocument.getDocuments().size() == 0) {
            setNextButtonEnabled(false);
            return;
        }

        boolean uploadFailed = false;
        for (final Boolean uploadSuccess : mDocumentUploadResults.values()) {
            if (!uploadSuccess) {
                uploadFailed = true;
                break;
            }
        }
        setNextButtonEnabled(!uploadFailed);
    }

    private void setNextButtonEnabled(final boolean enabled) {
        mButtonNext.setEnabled(enabled);
        if (enabled) {
            mButtonNext.animate().alpha(1.0f).start();
        } else {
            mButtonNext.animate().alpha(0.5f).start();
        }
    }


    @VisibleForTesting
    void onNextButtonClicked() {
        trackReviewScreenEvent(ReviewScreenEvent.NEXT);
        mNextClicked = true;
        mListener.onProceedToAnalysisScreen(mMultiPageDocument);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (!GiniCapture.hasInstance()) {
            return;
        }
        initMultiPageDocument();
        mNextClicked = false;
        if (!mPreviewsShown) {
            observeViewTree();
        }
        showAlertIfOpenWithDocumentAndAppIsDefault(activity,
                mMultiPageDocument, new FileImportHelper.ShowAlertCallback() {
                    @Override
                    public void showAlertDialog(@NonNull final String message,
                                                @NonNull final String positiveButtonTitle,
                                                @NonNull final DialogInterface.OnClickListener
                                                        positiveButtonClickListener,
                                                @Nullable final String negativeButtonTitle,
                                                @Nullable final DialogInterface.OnClickListener
                                                        negativeButtonClickListener,
                                                @Nullable final DialogInterface.OnCancelListener cancelListener) {
                        MultiPageReviewFragment.this.showAlertDialog(message, positiveButtonTitle,
                                positiveButtonClickListener,
                                negativeButtonTitle, negativeButtonClickListener, cancelListener);
                    }
                })
                .thenRun(new Runnable() {
                    @Override
                    public void run() {
                        uploadDocuments();
                    }
                });
    }

    private void uploadDocuments() {
        for (final ImageDocument imageDocument : mMultiPageDocument.getDocuments()) {
            if (!mMultiPageDocument.hasDocumentError(imageDocument)) {
                // Documents with a an error should not be uploaded automatically
                uploadDocument(imageDocument);
            }
        }
    }

    @VisibleForTesting
    void uploadDocument(final ImageDocument document) {
        if (!GiniCapture.hasInstance()) {
            return;
        }
        final NetworkRequestsManager networkRequestsManager =
                GiniCapture.getInstance().internal().getNetworkRequestsManager();
        if (networkRequestsManager == null) {
            return;
        }
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mMultiPageDocument.removeErrorForDocument(document);
        mDocumentUploadResults.put(document.getId(), false);
        networkRequestsManager.upload(activity, document)
                .handle(new CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>,
                        Throwable, Void>() {
                    @Override
                    public Void apply(
                            final NetworkRequestResult<GiniCaptureDocument> requestResult,
                            final Throwable throwable) {
                        if (throwable != null
                                && !NetworkRequestsManager.isCancellation(throwable)) {
                            trackUploadError(throwable);
                            final String errorMessage = getString(
                                    R.string.gc_document_analysis_error);
                            showErrorOnPreview(errorMessage, document);
                        } else if (requestResult != null) {
                            mDocumentUploadResults.put(document.getId(), true);
                        }
                        updateNextButtonVisibility();
                        return null;
                    }
                });
    }

    private void trackUploadError(@NonNull final Throwable throwable) {
        final Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put(UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE, throwable.getMessage());
        errorDetails.put(UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT, throwable);
        trackReviewScreenEvent(ReviewScreenEvent.UPLOAD_ERROR, errorDetails);
    }

    private void showErrorOnPreview(final String errorMessage, final ImageDocument imageDocument) {
        mMultiPageDocument.setErrorForDocument(imageDocument,
                new GiniCaptureDocumentError(errorMessage,
                        UPLOAD_FAILED));

        mPreviewsAdapter2.notifyDataSetChanged();
        mPreviewsPager2.post(() -> mPreviewsPager2.requestTransform());

        showHideBlueRect(View.VISIBLE);
    }

    private void observeViewTree() {
        final View view = getView();
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
        showPreviews();
    }

    private void showPreviews() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mPreviewsShown = true;
        mPreviewsPager2.setCurrentItem(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore()
                    .setMultiPageDocument(mMultiPageDocument);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!mNextClicked && mMultiPageDocument != null
                && mMultiPageDocument.getImportMethod() == Document.ImportMethod.OPEN_WITH) {
            // Delete documents imported using "open with" because the
            // Camera Screen is not launched for "open with"
            deleteUploadedDocuments();
        }

        if (mPreviewFragmentListener != null) {
            mPreviewFragmentListener = null;
        }

        if (mPager2PageCallback != null) {
            mPreviewsPager2.unregisterOnPageChangeCallback(mPager2PageCallback);
        }
    }

    private void deleteUploadedDocuments() {
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(mMultiPageDocument);
                networkRequestsManager.delete(mMultiPageDocument)
                        .handle((CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>, Throwable, Void>) (requestResult, throwable) -> {
                            for (final Object document : mMultiPageDocument.getDocuments()) {
                                final GiniCaptureDocument giniCaptureDocument =
                                        (GiniCaptureDocument) document;
                                networkRequestsManager.cancel(giniCaptureDocument);
                                networkRequestsManager.delete(giniCaptureDocument);
                            }
                            return null;
                        });
            }
        }
    }

    @Override
    public void onRetryUpload(@NonNull final ImageDocument document) {
        uploadDocument(document);
    }

    @Override
    public void onDeleteDocument(@NonNull final ImageDocument document) {
        deleteDocumentAndUpdateUI(document);
    }

    @Override
    public void onPageClicked(@NonNull ImageDocument document) {
        Intent intent = new Intent(requireContext(), ZoomInPreviewActivity.class);
        intent.putExtra(ARGS_DOCUMENT, document);
        startActivity(intent);
    }

    @Override
    public void setListener(@NonNull final MultiPageReviewFragmentListener listener) {
        mListener = listener;
    }

}
