package net.gini.android.capture.review.multipage;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.camera.CameraFragment;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.error.ErrorFragment;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.review.multipage.previews.MiddlePageManager;
import net.gini.android.capture.review.multipage.previews.PreviewFragmentListener;
import net.gini.android.capture.review.multipage.previews.PreviewPagesAdapter;
import net.gini.android.capture.review.multipage.view.ReviewNavigationBarBottomAdapter;
import net.gini.android.capture.saveinvoiceslocally.SaveInvoicesFeatureEvaluator;
import net.gini.android.capture.util.recyclerview.SnappedItemChangeRecyclerViewListener;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kotlin.Unit;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

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
public class MultiPageReviewFragment extends Fragment implements PreviewFragmentListener {

    private static final Logger LOG = LoggerFactory.getLogger(MultiPageReviewFragment.class);

    private MultiPageReviewViewModel mViewModel;
    private ImageMultiPageDocument mMultiPageDocument;
    private PreviewFragmentListener mPreviewFragmentListener;
    private PreviewPagesAdapter mPreviewPagesAdapter;
    private RecyclerView mRecyclerView;
    private Button mButtonNext;
    private ConstraintLayout mSaveInvoicesWrapper;
    private MaterialSwitch mSaveInvoicesSwitch;
    private LinearLayout mAddPagesWrapperLayout;
    private Button mAddPagesButton;
    private TabLayout mTabIndicator;
    private ConstraintLayout mProcessDocumentsWrapper;
    private InjectedViewContainer<NavigationBarTopAdapter> mTopAdapterInjectedViewContainer;
    private InjectedViewContainer<OnButtonLoadingIndicatorAdapter> injectedLoadingIndicatorContainer;
    private InjectedViewContainer<ReviewNavigationBarBottomAdapter> mReviewNavigationBarBottomAdapter;
    private boolean mPreviewsShown;
    private SnapHelper mSnapHelper;
    private MiddlePageManager mSnapManager;
    private boolean mInstanceStateSaved;
    private boolean isOnButtonLoadingIndicatorActive;
    private boolean isBottomNavigationBarContinueButtonEnabled;
    private boolean isBottomNavigationBarLoadingIndicatorActive;

    private boolean mShouldScrollToLastPage = false;
    private CancelListener mCancelListener;
    private int mScrollToPosition = -1;
    private final String KEY_SHOULD_SCROLL_TO_LAST_PAGE = "GC_SHOULD_SCROLL_TO_LAST_PAGE";
    private final String KEY_SCROLL_TO_POSITION = "GC_SHOULD_SCROLL_TO_LAST_PAGE";

    private SnappedItemChangeRecyclerViewListener mAnalyticsPageScrolledListener;

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

        mViewModel = new ViewModelProvider(this).get(MultiPageReviewViewModel.class);

        mViewModel.trackScreenShownEvent();
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                mViewModel.onBackClicked();
                setEnabled(false);
                remove();
                onBack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        if (getArguments() != null) {
            mShouldScrollToLastPage = getArguments().getBoolean(KEY_SHOULD_SCROLL_TO_LAST_PAGE, false);
        }

        if (savedInstanceState != null) {
            mScrollToPosition = savedInstanceState.getInt(KEY_SCROLL_TO_POSITION, -1);
            mShouldScrollToLastPage = savedInstanceState.getBoolean(KEY_SHOULD_SCROLL_TO_LAST_PAGE, false);
        }

        mPreviewFragmentListener = this;

        if (!GiniCapture.hasInstance()) {
            NavHostFragment.findNavController(this).navigate(MultiPageReviewFragmentDirections.toErrorFragment(ErrorType.GENERAL, mViewModel.getMultiPageDocument()));
        } else {
            initMultiPageDocument();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mInstanceStateSaved = false;
        setCameraFragmentResultListener();
    }

    private void setCameraFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener(CameraFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            mShouldScrollToLastPage = result.getBoolean(CameraFragment.RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mInstanceStateSaved = true;
        outState.putBoolean(KEY_SHOULD_SCROLL_TO_LAST_PAGE, mShouldScrollToLastPage);
        outState.putInt(KEY_SCROLL_TO_POSITION, mScrollToPosition);
    }

    public void setCancelListener(CancelListener cancelListener) {
        mCancelListener = cancelListener;
    }

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
        final boolean hasNewPages = mViewModel.initMultiPageDocument();
        mMultiPageDocument = mViewModel.getMultiPageDocument();

        if (mPreviewPagesAdapter != null && hasNewPages) {
            setupTabIndicator();
            resetUploadedDocumentsViews();
            scrollToCorrectPosition(mMultiPageDocument.getDocuments().size() - 1, true);
        }
    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        final LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);
        return getLayoutInflaterWithGiniCaptureTheme(this, inflater);
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

        handleViewsForSavingInvoices();

        observeViewModel();

        if (mMultiPageDocument != null) {
            mViewModel.updateNextButtonVisibility();
            initRecyclerView();
            delayWithBlueRect();
        }
    }

    private void observeViewModel() {
        mViewModel.getNextButtonEnabled().observe(getViewLifecycleOwner(), enabled ->
                setNextButtonEnabled(Boolean.TRUE.equals(enabled)));
        mViewModel.getLoadingIndicatorActive().observe(getViewLifecycleOwner(), active -> {
            if (Boolean.TRUE.equals(active)) {
                showIndicator();
            } else {
                hideIndicator();
            }
        });
        mViewModel.getEvents().observe(getViewLifecycleOwner(), consumableEvent -> {
            if (consumableEvent.getContentIfNotHandled() != null) {
                MultiPageReviewViewEvent event;
                while ((event = mViewModel.pollEvent()) != null) {
                    handleViewEvent(event);
                }
            }
        });
    }

    private void handleViewEvent(@NonNull final MultiPageReviewViewEvent event) {
        if (event instanceof MultiPageReviewViewEvent.FinishActivity) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        } else if (event instanceof MultiPageReviewViewEvent.ShowAlertDialog) {
            final MultiPageReviewViewEvent.ShowAlertDialog dialog =
                    (MultiPageReviewViewEvent.ShowAlertDialog) event;
            showAlertDialog(dialog.getMessage(), dialog.getPositiveButtonTitle(),
                    dialog.getPositiveButtonClickListener(), dialog.getNegativeButtonTitle(),
                    dialog.getNegativeButtonClickListener(), dialog.getCancelListener());
        } else if (event instanceof MultiPageReviewViewEvent.PageDeleted) {
            onPageDeleted(((MultiPageReviewViewEvent.PageDeleted) event).getDeletedPosition());
        } else if (event instanceof MultiPageReviewViewEvent.NavigateToCameraForFirstPage) {
            NavHostFragment.findNavController(this).navigate(
                    MultiPageReviewFragmentDirections.toCameraFragmentForFirstPage());
        } else if (event instanceof MultiPageReviewViewEvent.NavigateToAnalysis) {
            final MultiPageReviewViewEvent.NavigateToAnalysis navigateToAnalysis =
                    (MultiPageReviewViewEvent.NavigateToAnalysis) event;
            NavHostFragment.findNavController(this).navigate(
                    MultiPageReviewFragmentDirections.toAnalysisFragment(
                            navigateToAnalysis.getDocument(),
                            "",
                            navigateToAnalysis.getShouldSaveInvoicesLocally()
                    )
            );
        } else if (event instanceof MultiPageReviewViewEvent.NavigateToError) {
            final MultiPageReviewViewEvent.NavigateToError error =
                    (MultiPageReviewViewEvent.NavigateToError) event;
            Navigation.findNavController(this.getView()).navigate(
                    MultiPageReviewFragmentDirections.toErrorFragment(
                            error.getErrorType(), error.getDocument()));
        } else if (event instanceof MultiPageReviewViewEvent.NavigateToUploadError) {
            final MultiPageReviewViewEvent.NavigateToUploadError error =
                    (MultiPageReviewViewEvent.NavigateToUploadError) event;
            ErrorFragment.Companion.navigateToErrorFragment(
                    Navigation.findNavController(this.getView()),
                    MultiPageReviewFragmentDirections.toErrorFragment(
                            error.getErrorType(), error.getDocument())
            );
        }
    }

    private void handleViewsForSavingInvoices() {
        if (SaveInvoicesFeatureEvaluator.INSTANCE.shouldShowSaveInvoicesLocallyView()) {
            updateSaveInvoicesBackground();
            mSaveInvoicesWrapper.setVisibility(View.VISIBLE);
        } else
            mSaveInvoicesWrapper.setVisibility(View.GONE);

        mSaveInvoicesSwitch.setOnCheckedChangeListener((
                buttonView,
                isChecked) -> updateSaveInvoicesBackground());
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
        // First clear all the already selected tabs
        if (visibility == View.VISIBLE) clearAllBlueRect();
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

        mAnalyticsPageScrolledListener = new SnappedItemChangeRecyclerViewListener(mSnapHelper, pos -> {
            mViewModel.onPageSwiped();
            return Unit.INSTANCE;
        });

        mRecyclerView.postDelayed(this::attachScrollListener, 200);

        shouldIndicatorBeVisible();
    }

    //Smooth scroll needed when starting screen
    //Ordinary scroll needed when screen rotated
    private void scrollToCorrectPosition(int mSnapViewPosition, boolean isSmooth) {
        // Notify listener about ongoing programmatic scrolling.
        // Programmatic scroll detection should be skipped
        mAnalyticsPageScrolledListener.skipNextEventDetection();

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
                            mShouldScrollToLastPage = false;
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
            if (mShouldScrollToLastPage) {
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

        mRecyclerView.addOnScrollListener(mAnalyticsPageScrolledListener);
    }


    private void bindViews(final View view) {
        mButtonNext = view.findViewById(R.id.gc_button_next);
        mSaveInvoicesWrapper = view.findViewById(R.id.gc_save_invoices_wrapper);
        mSaveInvoicesSwitch = view.findViewById(R.id.gc_save_invoices_switch);
        mTabIndicator = view.findViewById(R.id.gc_tab_indicator);
        mTopAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
        mAddPagesWrapperLayout = view.findViewById(R.id.gc_add_pages_wrapper);
        mAddPagesButton = view.findViewById(R.id.gc_add_page_button);
        mRecyclerView = view.findViewById(R.id.gc_pager_recycler_view);
        injectedLoadingIndicatorContainer = view.findViewById(R.id.gc_injected_loading_indicator_container);
        mProcessDocumentsWrapper = view.findViewById(R.id.gc_process_documents_wrapper);

        // Please Do not remove the below check [ContextHelper.isPortraitOrTablet(requireContext())], as we have different
        // layouts for landscape, we don't need bottom bar in landscape mode only because of repositioning related to buttons.
        // In phones and tablets regardless of orientation this bottom bar is needed.
        if (ContextHelper.isPortraitOrTablet(requireContext()))
            setReviewNavigationBarBottomAdapter(view);
        else {
            // this is landscape mode in phones only , where we don't show the bottom bar but
            // we have to handle visibility of add pages.
            handleAddPagesVisibility();
        }
    }

    private void setInjectedLoadingIndicatorContainer() {
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            injectedLoadingIndicatorContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getOnButtonLoadingIndicatorAdapterInstance(), injectedViewAdapter -> {
                if (isOnButtonLoadingIndicatorActive) {
                    injectedViewAdapter.onVisible();
                } else {
                    injectedViewAdapter.onHidden();
                }
            }));
        }
    }

    private void updateSaveInvoicesBackground() {
        mSaveInvoicesWrapper.setBackgroundResource(
                mSaveInvoicesSwitch.isChecked()
                        ? R.drawable.gc_bg_on_save_invoices_locally
                        : R.drawable.gc_bg_off_save_invoices_locally
        );
    }

    private void setReviewNavigationBarBottomAdapter(View view) {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {

            mReviewNavigationBarBottomAdapter =
                    view.findViewById(R.id.gc_injected_navigation_bar_container_bottom);

            ViewGroup.LayoutParams params = mReviewNavigationBarBottomAdapter.getLayoutParams();
            params.height = (int) getResources().getDimension(R.dimen.gc_review_bottom_bar_height);

            mReviewNavigationBarBottomAdapter.setLayoutParams(params);

            hideViewsIfBottomBarEnabled();

            mReviewNavigationBarBottomAdapter.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getReviewNavigationBarBottomAdapterInstance(),
                    injectedViewAdapter -> {
                        injectedViewAdapter.setOnAddPageButtonClickListener(new IntervalClickListener(v -> {
                            mViewModel.onAddPagesTapped();
                            NavHostFragment.findNavController(this).navigate(MultiPageReviewFragmentDirections.toCameraFragmentForAddingPages());
                        }));

                        boolean isMultiPage = GiniCapture.getInstance().isMultiPageEnabled();

                        injectedViewAdapter.setAddPageButtonVisibility(isMultiPage ? View.VISIBLE : View.GONE);
                        injectedViewAdapter.setOnContinueButtonClickListener(new IntervalClickListener(v -> onNextButtonClicked()));

                        injectedViewAdapter.setContinueButtonEnabled(isBottomNavigationBarContinueButtonEnabled);
                        if (isBottomNavigationBarLoadingIndicatorActive) {
                            injectedViewAdapter.showLoadingIndicator();
                        } else {
                            injectedViewAdapter.hideLoadingIndicator();
                        }
                    }));
        }
    }

    private void hideViewsIfBottomBarEnabled() {
        // Please Do not remove the below check [ContextHelper.isPortraitOrTablet(requireContext())], as we have different
        // layouts for landscape in phone only!!  and in landscape (phone only) these buttons should not be hidden.
        if (ContextHelper.isPortraitOrTablet(requireContext()))
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
                View c = child.findViewById(R.id.gc_image_preview_root);
                if (c != null) {
                    c.findViewById(R.id.gc_image_selected_rect)
                            .setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void setupTopNavigationBar() {
        if (GiniCapture.hasInstance()) {
            mTopAdapterInjectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(),
                    injectedViewAdapter -> {
                        injectedViewAdapter.setTitle(getString(R.string.gc_title_review));

                        injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);

                        injectedViewAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> {
                            mViewModel.onBackClicked();
                            onBack();
                        }));
                    }));
        }
    }

    private void handleAddPagesVisibility() {
        mAddPagesWrapperLayout.setVisibility(GiniCapture.getInstance().isMultiPageEnabled() ? View.VISIBLE : View.GONE);
        mAddPagesButton.setVisibility(GiniCapture.getInstance().isMultiPageEnabled() ? View.VISIBLE : View.GONE);
    }
    private void setInputHandlers() {
        ClickListenerExtKt.setIntervalClickListener(mButtonNext, v -> onNextButtonClicked());

        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            handleAddPagesVisibility();
        }

        ClickListenerExtKt.setIntervalClickListener(mAddPagesButton, v -> {
            mViewModel.onAddPagesTapped();
            NavHostFragment.findNavController(this).navigate(MultiPageReviewFragmentDirections.toCameraFragmentForAddingPages());
        });
    }


    private void onPageDeleted(final int deletedPosition) {
        final int nrOfDocuments = mMultiPageDocument.getDocuments().size();
        final int newPosition = PreviewPagesAdapter.getNewPositionAfterDeletion(deletedPosition, nrOfDocuments);

        mPreviewPagesAdapter.notifyItemRemoved(deletedPosition);
        mTabIndicator.removeTabAt(deletedPosition);
        updateTabIndicatorPosition(newPosition);

        delayWithBlueRect();

        shouldIndicatorBeVisible();

        if (mMultiPageDocument.getDocuments().isEmpty()) {
            setScrollToPosition(-1);
        } else {
            handleViewsForSavingInvoices();
        }
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
        } else {
            isBottomNavigationBarContinueButtonEnabled = enabled;
            if (mReviewNavigationBarBottomAdapter == null) {
                return;
            }
            mReviewNavigationBarBottomAdapter.modifyAdapterIfOwned(injectedViewAdapter -> {
                injectedViewAdapter.setContinueButtonEnabled(enabled);
                return Unit.INSTANCE;
            });
        }
    }


    @VisibleForTesting
    void onNextButtonClicked() {
        mViewModel.onNextButtonClicked(
                SaveInvoicesFeatureEvaluator.INSTANCE.shouldSaveInvoicesLocally(
                        mSaveInvoicesWrapper.getVisibility(),
                        mSaveInvoicesSwitch.isChecked()
                )
        );
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
        if (!mPreviewsShown) {
            observeViewTree();
        }
        mViewModel.onResume(activity);
    }

    private void showIndicator() {
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            isOnButtonLoadingIndicatorActive = true;
            if (injectedLoadingIndicatorContainer == null) {
                return;
            }
            injectedLoadingIndicatorContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                injectedViewAdapter.onVisible();
                return Unit.INSTANCE;
            });
        } else {
            isBottomNavigationBarLoadingIndicatorActive = true;
            if (mReviewNavigationBarBottomAdapter == null) {
                return;
            }
            mReviewNavigationBarBottomAdapter.modifyAdapterIfOwned(injectedViewAdapter -> {
                injectedViewAdapter.showLoadingIndicator();
                return Unit.INSTANCE;
            });
        }
    }

    private void hideIndicator() {
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            isOnButtonLoadingIndicatorActive = false;
            if (injectedLoadingIndicatorContainer == null) {
                return;
            }
            injectedLoadingIndicatorContainer.modifyAdapterIfOwned(injectedViewAdapter -> {
                injectedViewAdapter.onHidden();
                return Unit.INSTANCE;
            });
        } else {
            isBottomNavigationBarLoadingIndicatorActive = false;
            if (mReviewNavigationBarBottomAdapter == null) {
                return;
            }
            mReviewNavigationBarBottomAdapter.modifyAdapterIfOwned(injectedViewAdapter -> {
                injectedViewAdapter.hideLoadingIndicator();
                return Unit.INSTANCE;
            });
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

        return mScrollToPosition;
    }

    private void setScrollToPosition(int scrollToPosition) {
        if (getActivity() == null)
            return;

        mScrollToPosition = scrollToPosition;
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mViewModel.onDestroy(mInstanceStateSaved);

        if (mPreviewFragmentListener != null) {
            mPreviewFragmentListener = null;
        }
    }

    private void onBack() {
        boolean popBackStack = NavHostFragment.findNavController(this).popBackStack();
        if (!popBackStack) {
            mCancelListener.onCancelFlow();
        }
    }

    @Override
    public void onRetryUpload(@NonNull final ImageDocument document) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mViewModel.uploadDocument(document, activity);
    }

    @Override
    public void onDeleteDocument(@NonNull final ImageDocument document) {
        mViewModel.onDeleteDocument(document);
    }

    @Override
    public void onPageClicked(@NonNull ImageDocument document) {
        mViewModel.onPageClicked();
        NavHostFragment.findNavController(this).navigate(MultiPageReviewFragmentDirections.toZoomInPreviewFragment(document));
    }
}
