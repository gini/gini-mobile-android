package net.gini.android.capture.review.multipage;

import static net.gini.android.capture.GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.internal.util.FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault;
import static net.gini.android.capture.review.multipage.thumbnails.ThumbnailsAdapter.getNewPositionAfterDeletion;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureDocumentError;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.error.ErrorActivity;
import net.gini.android.capture.internal.network.NetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.internal.util.FileImportHelper;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.network.FailureException;
import net.gini.android.capture.review.multipage.previews.MiddlePageManager;
import net.gini.android.capture.review.multipage.previews.PreviewFragmentListener;
import net.gini.android.capture.review.multipage.previews.PreviewPagesAdapter;
import net.gini.android.capture.review.multipage.view.ReviewNavigationBarBottomAdapter;
import net.gini.android.capture.review.zoom.ZoomInPreviewActivity;
import net.gini.android.capture.tracking.ReviewScreenEvent;
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 07.05.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
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
    private PreviewPagesAdapter mPreviewPagesAdapter;
    private RecyclerView mRecyclerView;
    private Button mButtonNext;
    private LinearLayout mAddPages;
    private TabLayout mTabIndicator;
    private ConstraintLayout mProcessDocumentsWrapper;
    private InjectedViewContainer<NavigationBarTopAdapter> mTopAdapterInjectedViewContainer;
    private InjectedViewContainer<OnButtonLoadingIndicatorAdapter> injectedLoadingIndicatorContainer;
    private InjectedViewContainer<ReviewNavigationBarBottomAdapter> mReviewNavigationBarBottomAdapter;
    private boolean mNextClicked;
    private boolean mPreviewsShown;
    private SnapHelper mSnapHelper;
    private MiddlePageManager mSnapManager;
    private boolean mInstanceStateSaved;

    public static MultiPageReviewFragment newInstance() {

        Bundle args = new Bundle();
        MultiPageReviewFragment fragment = new MultiPageReviewFragment();
        fragment.setArguments(args);
        return fragment;
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
    public void onStart() {
        super.onStart();
        mInstanceStateSaved = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mInstanceStateSaved = true;
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

        if (mMultiPageDocument.getDocuments().isEmpty()) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

        initUploadResults();

    }

    private void initUploadResults() {

        if (mPreviewPagesAdapter != null && mDocumentUploadResults.size() < mMultiPageDocument.getDocuments().size()) {
            setupTabIndicator();
            resetUploadedDocumentsViews();
            scrollToCorrectPosition(mMultiPageDocument.getDocuments().size() - 1, true);
        }

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

        return inflater.inflate(R.layout.gc_fragment_multi_page_review, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);

        setInjectedLoadingIndicatorContainer();

        setupTabIndicator();

        setInputHandlers();

        setupTopNavigationBar();

        if (mMultiPageDocument != null) {
            updateNextButtonVisibility();
            initRecyclerView();
        }
    }


    private void resetUploadedDocumentsViews() {
        //Needed to refresh views in the recyclerview
        mRecyclerView.setAdapter(null);
        mSnapHelper.attachToRecyclerView(null);
        mSnapHelper = null;
        mPreviewPagesAdapter = null;
        mSnapManager = null;
        mRecyclerView.setLayoutManager(null);

        initRecyclerView();

        if (getScrollPosition() > -1 && getScrollPosition() <= mMultiPageDocument.getDocuments().size() - 1)
            scrollToCorrectPosition(getScrollPosition(), true);

    }

    //Delay with blue rect when starting the screen
    private void delayWithBlueRect() {
        mRecyclerView.post(() -> showHideBlueRect(View.VISIBLE));
    }

    //Look for the page in the middle
    //Make it blue
    private View showHideBlueRect(int visibility) {
        View mChild = mSnapHelper.findSnapView(mSnapManager);

        if (mChild != null) {
            mChild.findViewById(R.id.gc_image_selected_rect)
                    .setVisibility(visibility);
        }
        return mChild;
    }

    private void shouldIndicatorBeVisible() {
        mTabIndicator.setVisibility(mPreviewPagesAdapter.getItemCount() <= 1 ? View.INVISIBLE : View.VISIBLE);
    }

    private void initRecyclerView() {

        if (getActivity() == null)
            return;

        //Custom LayoutManager for keeping the page in the middle
        mSnapManager = new MiddlePageManager(requireContext(), MiddlePageManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mSnapManager);
        mRecyclerView.setClipToPadding(false);

        //Snap helper to mimic ViewPager snap behaviour
        mSnapHelper = new PagerSnapHelper();
        mSnapHelper.attachToRecyclerView(mRecyclerView);

        mPreviewPagesAdapter = new PreviewPagesAdapter(mMultiPageDocument, mPreviewFragmentListener);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mPreviewPagesAdapter);


        mRecyclerView.postDelayed(this::attachScrollListener, 200);

        shouldIndicatorBeVisible();
    }

    //Smooth scroll needed when starting screen
    //Ordinary scroll needed when screen rotated
    private void scrollToCorrectPosition(int mSnapViewPosition, boolean isSmooth) {
        mRecyclerView.post(() -> {
            if (!isSmooth)
                mRecyclerView.scrollToPosition(mSnapViewPosition);
            else mRecyclerView.smoothScrollToPosition(mSnapViewPosition);

            mTabIndicator.selectTab(mTabIndicator.getTabAt(mSnapViewPosition));
        });
    }

    private void attachScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                //Wait to state goes in IDLE
                //Make the middle page rect blue
                //Update position of tablayout
                //Save the position for screen change
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    View viewAtPosition = showHideBlueRect(View.VISIBLE);

                    if (viewAtPosition != null) {
                        int position = mRecyclerView.getChildAdapterPosition(viewAtPosition);
                        updateTabIndicatorPosition(position);
                        setScrollToPosition(position);

                        if (position < mMultiPageDocument.getDocuments().size() - 1)
                            setShouldScrollToLastPage(false);
                    }

                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    showHideBlueRect(View.INVISIBLE);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        //Scroll to the last captured document
        if (mMultiPageDocument.getDocuments().size() > 1) {
            if (shouldScrollToLastPage()) {
                scrollToCorrectPosition(mMultiPageDocument.getDocuments().size() - 1, true);
            } else {
                if (getScrollPosition() > -1)
                    scrollToCorrectPosition(getScrollPosition(), true);
                else scrollToCorrectPosition(mMultiPageDocument.getDocuments().size() - 1, true);
            }
        }

        //If there is one document highlight it
        if (mMultiPageDocument.getDocuments().size() == 1) {
            mRecyclerView.post(() -> showHideBlueRect(View.VISIBLE));
        }
    }


    private void bindViews(final View view) {
        mButtonNext = view.findViewById(R.id.gc_button_next);
        mTabIndicator = view.findViewById(R.id.gc_tab_indicator);
        mTopAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
        mAddPages = view.findViewById(R.id.gc_add_pages_wrapper);
        mRecyclerView = view.findViewById(R.id.gc_pager_recycler_view);
        injectedLoadingIndicatorContainer = view.findViewById(R.id.gc_injected_loading_indicator_container);
        mProcessDocumentsWrapper = view.findViewById(R.id.gc_process_documents_wrapper);
        setReviewNavigationBarBottomAdapter(view);
    }

    private void setInjectedLoadingIndicatorContainer() {
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            injectedLoadingIndicatorContainer.setInjectedViewAdapter(GiniCapture.getInstance().getOnButtonLoadingIndicatorAdapter());
        }
    }

    private void setReviewNavigationBarBottomAdapter(View view) {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {

            mReviewNavigationBarBottomAdapter =
                    view.findViewById(R.id.gc_injected_navigation_bar_container_bottom);

            ViewGroup.LayoutParams params = mReviewNavigationBarBottomAdapter.getLayoutParams();
            params.height = (int) getResources().getDimension(R.dimen.gc_review_bottom_bar_height);

            mReviewNavigationBarBottomAdapter.setLayoutParams(params);

            mReviewNavigationBarBottomAdapter.setInjectedViewAdapter(GiniCapture.getInstance().getReviewNavigationBarBottomAdapter());

            if (mReviewNavigationBarBottomAdapter.getInjectedViewAdapter() == null) {
                return;
            }

            hideViewsIfBottomBarEnabled();

            mReviewNavigationBarBottomAdapter.getInjectedViewAdapter().setOnAddPageButtonClickListener(new IntervalClickListener(v -> mListener.onReturnToCameraScreenToAddPages()));

            boolean isMultiPage = GiniCapture.getInstance().isMultiPageEnabled();

            mReviewNavigationBarBottomAdapter.getInjectedViewAdapter().setAddPageButtonVisibility(isMultiPage ? View.VISIBLE : View.GONE);
            mReviewNavigationBarBottomAdapter.getInjectedViewAdapter().setOnContinueButtonClickListener(new IntervalClickListener(v -> onNextButtonClicked()));

        }

    }

    private void hideViewsIfBottomBarEnabled() {
        mProcessDocumentsWrapper.setVisibility(View.GONE);
    }

    //Add empty tabs to present dots on the screen
    private void setupTabIndicator() {

        if (mMultiPageDocument == null)
            return;

        mTabIndicator.removeAllTabs();

        for (int i = 0; i < mMultiPageDocument.getDocuments().size(); i++) {
            TabLayout.Tab tab = mTabIndicator.newTab();

            tab.view.setOnClickListener(v -> {

                //If user is clicking on same tab -> return
                if (mTabIndicator.getSelectedTabPosition() == tab.getPosition())
                    return;

                clearAllBlueRect();
                scrollToCorrectPosition(tab.getPosition(), true);
                mRecyclerView.postDelayed(() -> showHideBlueRect(View.VISIBLE), 200);
            });

            mTabIndicator.addTab(tab);
        }
        mTabIndicator.setSmoothScrollingEnabled(true);

    }

    private void updateTabIndicatorPosition(int viewAtPosition) {
        mTabIndicator.selectTab(mTabIndicator.getTabAt(viewAtPosition));
    }

    //Clear all blue rect from views
    private void clearAllBlueRect() {
        for (int i = 0; i < mPreviewPagesAdapter.getItemCount(); i++) {
            View child = mSnapManager.getChildAt(i);
            if (child != null) {
                ConstraintLayout c = child.findViewById(R.id.gc_image_wrapper);
                if (c != null) {
                    c.findViewById(R.id.gc_image_selected_rect)
                            .setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void setupTopNavigationBar() {
        if (GiniCapture.hasInstance()) {
            mTopAdapterInjectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getNavigationBarTopAdapter());

            if (mTopAdapterInjectedViewContainer.getInjectedViewAdapter() == null)
                return;

            if (this.getActivity() == null)
                return;

            mTopAdapterInjectedViewContainer.getInjectedViewAdapter().setTitle(getString(R.string.gc_title_review));

            mTopAdapterInjectedViewContainer.getInjectedViewAdapter().setNavButtonType(NavButtonType.CLOSE);

            mTopAdapterInjectedViewContainer.getInjectedViewAdapter().setOnNavButtonClickListener(new IntervalClickListener(v -> {
                if (MultiPageReviewFragment.this.getActivity() != null) {
                    MultiPageReviewFragment.this.getActivity().onBackPressed();
                }
            }));

        }
    }

    private void setInputHandlers() {
        ClickListenerExtKt.setIntervalClickListener(mButtonNext, v -> onNextButtonClicked());

        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            mAddPages.setVisibility(GiniCapture.getInstance().isMultiPageEnabled() ? View.VISIBLE : View.GONE);
        }

        ClickListenerExtKt.setIntervalClickListener(mAddPages, v -> mListener.onReturnToCameraScreenToAddPages());
    }


    private void deleteDocumentAndUpdateUI(@NonNull final ImageDocument document) {
        if (mMultiPageDocument.getDocuments().size() == 1) {
            final FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            if (mMultiPageDocument.getImportMethod() == Document.ImportMethod.OPEN_WITH) {
                new MaterialAlertDialogBuilder(activity)
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
                mListener.onReturnToCameraScreenForFirstPage();
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

        mPreviewPagesAdapter.notifyItemRemoved(deletedPosition);
        mTabIndicator.removeTabAt(deletedPosition);
        updateTabIndicatorPosition(newPosition);

        delayWithBlueRect();

        updateNextButtonVisibility();

        shouldIndicatorBeVisible();

        if (mMultiPageDocument.getDocuments().isEmpty()) {
            setScrollToPosition(-1);
        }
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

        if (!GiniCapture.hasInstance())
            return;

        if (!GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            mButtonNext.setEnabled(enabled);
            if (enabled) {
                mButtonNext.animate().alpha(1.0f).start();
            } else {
                mButtonNext.animate().alpha(0.5f).start();
            }
        } else if (mReviewNavigationBarBottomAdapter != null
                && mReviewNavigationBarBottomAdapter.getInjectedViewAdapter() != null) {
            mReviewNavigationBarBottomAdapter.getInjectedViewAdapter()
                    .setContinueButtonEnabled(enabled);
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
            } else {
                final GiniCaptureDocumentError documentError = mMultiPageDocument.getErrorForDocument(imageDocument);
                if (documentError != null) {
                    ErrorType errorType = ErrorType.typeFromDocumentErrorCode(documentError.getErrorCode());
                    ErrorActivity.startErrorActivity(requireActivity(), errorType, imageDocument);
                }
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

        showIndicator();

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

                            hideIndicator();

                            trackUploadError(throwable);

                            if (getActivity() != null) {
                                handleError(throwable, document);
                            }

                        } else if (requestResult != null) {
                            hideIndicator();
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

    private void handleError(Throwable throwable, Document document) {
        if (getActivity() != null) {
            final FailureException failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable);
            if (failureException != null) {
                ErrorActivity.startErrorActivity(requireActivity(), failureException.getErrorType(), document);
            } else {
                ErrorActivity.startErrorActivity(requireActivity(), ErrorType.GENERAL, document);
            }
        }
    }

    private void showIndicator() {
        if (injectedLoadingIndicatorContainer != null && injectedLoadingIndicatorContainer.getInjectedViewAdapter() != null)
            injectedLoadingIndicatorContainer.getInjectedViewAdapter().onVisible();
        else if (mReviewNavigationBarBottomAdapter != null && mReviewNavigationBarBottomAdapter.getInjectedViewAdapter() != null) {
            mReviewNavigationBarBottomAdapter.getInjectedViewAdapter().showLoadingIndicator();
        }
    }

    private void hideIndicator() {
        if (injectedLoadingIndicatorContainer != null && injectedLoadingIndicatorContainer.getInjectedViewAdapter() != null)
            injectedLoadingIndicatorContainer.getInjectedViewAdapter().onHidden();
        else if (mReviewNavigationBarBottomAdapter != null && mReviewNavigationBarBottomAdapter.getInjectedViewAdapter() != null) {
            mReviewNavigationBarBottomAdapter.getInjectedViewAdapter().hideLoadingIndicator();
        }
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
    }

    private int getScrollPosition() {
        if (getActivity() == null)
            return -1;

        return ((MultiPageReviewActivity) getActivity()).getScrollToPosition();
    }

    private void setScrollToPosition(int scrollToPosition) {
        if (getActivity() == null)
            return;

        ((MultiPageReviewActivity) getActivity()).setScrollToPosition(scrollToPosition);
    }

    public boolean shouldScrollToLastPage() {
        if (getActivity() == null)
            return false;

        return ((MultiPageReviewActivity) getActivity()).shouldScrollToLastPage();
    }

    public void setShouldScrollToLastPage(boolean shouldScrollToLastPage) {
        if (getActivity() == null)
            return;

        ((MultiPageReviewActivity) getActivity()).setShouldScrollToLastPage(shouldScrollToLastPage);
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

        if (!mInstanceStateSaved) {
            // Instance state wasn't saved meaning that this fragment won't restart
            if (!mNextClicked) {
                // Delete documents because the Multi-Page Review Fragment
                // acts as the root screen and when it's destroyed it means
                // the user will exit the SDK
                deleteUploadedDocuments();
                clearMultiPageDocument();
            }
        }

        if (mPreviewFragmentListener != null) {
            mPreviewFragmentListener = null;
        }
    }

    private void deleteUploadedDocuments() {
        if (mMultiPageDocument == null) {
            return;
        }

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

    private void clearMultiPageDocument() {
        if (GiniCapture.hasInstance()) {
            mMultiPageDocument = null; // NOPMD
            GiniCapture.getInstance().internal()
                    .getImageMultiPageDocumentMemoryStore().clear();
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
