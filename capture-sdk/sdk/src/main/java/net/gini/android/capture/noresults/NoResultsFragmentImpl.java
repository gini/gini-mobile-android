package net.gini.android.capture.noresults;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gini.android.capture.Document;
import net.gini.android.capture.EnterManuallyButtonListener;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.error.view.ErrorNavigationBarBottomAdapter;
import net.gini.android.capture.help.PhotoTipsAdapter;
import net.gini.android.capture.help.SupportedFormatsAdapter;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import static android.view.View.GONE;

/**
 * Main logic implementation for no results UI presented by {@link NoResultsFragment}.
 * The back/retake/enter manually decisions and the analytics tracking live in
 * {@link NoResultsViewModel}; this class renders the view model's state and executes its
 * one-shot events.
 * Internal use only.
 */
class NoResultsFragmentImpl {

    private static final EnterManuallyButtonListener NO_OP_LISTENER =
            () -> {
            };

    private final FragmentImplCallback mFragment;
    private View mView;
    private final Document mDocument;
    private final NoResultsViewModel mViewModel;
    // CancelListener should be removed in the next major version - not a breaking change but better to keep it for now
    private final CancelListener mCancelListener;
    private EnterManuallyButtonListener mListener;
    private TextView mTitleTextView;

    NoResultsFragmentImpl(@NonNull final FragmentImplCallback fragment,
                          @NonNull final Document document,
                          @NonNull final NoResultsViewModel viewModel,
                          @NonNull final CancelListener cancelListener) {
        mFragment = fragment;
        mDocument = document;
        mViewModel = viewModel;
        mCancelListener = cancelListener;
    }

    void setListener(@Nullable final EnterManuallyButtonListener listener) {
        if (listener == null) {
            mListener = NO_OP_LISTENER;
        } else {
            mListener = listener;
        }
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                      final Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.gc_fragment_noresults, container, false);
        final View retakeImagesButton = mView.findViewById(R.id.gc_button_no_results_retake_images);
        handleOnBackPressed();
        mViewModel.onScreenShown();
        observeViewModel();

        final NoResultsUiState uiState = mViewModel.getUiState().getValue();
        if (uiState != null && uiState.getAllowRetakeImages()) {
            ClickListenerExtKt.setIntervalClickListener(retakeImagesButton, v ->
                    mViewModel.onRetakeImagesClicked());
        } else {
            retakeImagesButton.setVisibility(GONE);
        }

        final View enterManuallyButton = mView.findViewById(R.id.gc_button_no_results_enter_manually);
        ClickListenerExtKt.setIntervalClickListener(enterManuallyButton, v ->
                mViewModel.onEnterManuallyClicked());

        bindViews(mView);

        if (uiState != null && uiState.getShowQrCodeTitle()) {
            mTitleTextView.setText(mFragment.getActivity().getResources().getString(R.string.gc_noresults_header_qr));
        }

        setupTopBarNavigation();
        setupBottomBarNavigation();
        setUpList(mView);

        return mView;
    }

    private void observeViewModel() {
        mViewModel.getEvents().observe(mFragment.getViewLifecycleOwner(), consumableEvent -> {
            final NoResultsViewEvent event = consumableEvent.getContentIfNotHandled();
            if (event == null) {
                return;
            }
            if (event instanceof NoResultsViewEvent.NavigateToCamera) {
                mFragment.findNavController().navigate(NoResultsFragmentDirections.toCameraFragment());
            } else if (event instanceof NoResultsViewEvent.EnterManually) {
                mListener.onEnterManuallyPressed();
            }
        });
    }

    private void handleOnBackPressed() {
        mFragment.getActivity().getOnBackPressedDispatcher().addCallback(mFragment.getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mViewModel.onCloseClicked();
            }
        });
    }

    private void setUpList(View view) {
        final RecyclerView recyclerView = view.findViewById(R.id.gc_no_results_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mFragment.getActivity()));

        switch (mDocument.getType()) {
            case PDF:
            case PDF_MULTI_PAGE:
                recyclerView.setAdapter(new SupportedFormatsAdapter(false));
                break;
            case IMAGE:
            case IMAGE_MULTI_PAGE:
                recyclerView.setAdapter(new PhotoTipsAdapter(view.getContext(), true));
                break;
            default:
                recyclerView.setAdapter(new SupportedFormatsAdapter(true));
        }
    }

    private void setupTopBarNavigation() {
        InjectedViewContainer<NavigationBarTopAdapter> topBarContainer =
                mView.findViewById(R.id.gc_injected_navigation_bar_container_top);
        if (GiniCapture.hasInstance()) {
            topBarContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(),
                    injectedViewAdapter -> {
                        NavButtonType navType = GiniCapture.getInstance().isBottomNavigationBarEnabled()
                                ? NavButtonType.NONE : NavButtonType.BACK;
                        injectedViewAdapter.setNavButtonType(navType);
                        injectedViewAdapter.setTitle(
                                mFragment.getActivity() != null
                                        ? mFragment.getActivity().getString(R.string.gc_title_no_results)
                                        : ""
                        );
                        injectedViewAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> {
                            mViewModel.onCloseClicked();
                        }));
                    }
            ));
        }
    }

    private void setupBottomBarNavigation() {
        InjectedViewContainer<ErrorNavigationBarBottomAdapter> topBarContainer =
                mView.findViewById(R.id.gc_injected_navigation_bar_container_bottom);

        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            topBarContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getErrorNavigationBarBottomAdapterInstance(),
                    injectedViewAdapter -> injectedViewAdapter.setOnBackClickListener(new IntervalClickListener(v -> {
                        mViewModel.onCloseClicked();
                    }))
            ));
        }

    }

    private void bindViews(@NonNull final View view) {
        mTitleTextView = view.findViewById(R.id.gc_no_results_header);
    }
}
