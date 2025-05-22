package net.gini.android.capture.noresults;

import static android.view.View.GONE;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;

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
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.error.view.ErrorNavigationBarBottomAdapter;
import net.gini.android.capture.help.PhotoTipsAdapter;
import net.gini.android.capture.help.SupportedFormatsAdapter;
import net.gini.android.capture.internal.ui.ClickListenerExtKt;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsMappersKt;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import java.util.HashSet;

/**
 * Main logic implementation for no results UI presented by {@link NoResultsFragment}.
 * Internal use only.
 */
class NoResultsFragmentImpl {

    private static final EnterManuallyButtonListener NO_OP_LISTENER =
            () -> {
            };

    private final FragmentImplCallback mFragment;
    private View mView;
    private final Document mDocument;
    // CancelListener should be removed in the next major version - not a breaking change but better to keep it for now
    private final CancelListener mCancelListener;
    private EnterManuallyButtonListener mListener;
    private TextView mTitleTextView;
    private UserAnalyticsEventTracker mUserAnalyticsEventTracker;

    private final UserAnalyticsScreen screenName = UserAnalyticsScreen.NoResults.INSTANCE;

    NoResultsFragmentImpl(@NonNull final FragmentImplCallback fragment,
                          @NonNull final Document document,
                          @NonNull final CancelListener cancelListener) {
        mFragment = fragment;
        mDocument = document;
        mCancelListener = cancelListener;
    }

    void setListener(@Nullable final EnterManuallyButtonListener listener) {
        if (listener == null) {
            mListener = NO_OP_LISTENER;
        } else {
            mListener = listener;
        }
    }

    void onCreate(final Bundle savedInstanceState) {
        // Clear the image from the memory store because the user can only exit for manual entry or in some cases
        // can go back to the camera to take new pictures
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
        }
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                      final Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.gc_fragment_noresults, container, false);
        final View retakeImagesButton = mView.findViewById(R.id.gc_button_no_results_retake_images);
        handleOnBackPressed();
        mUserAnalyticsEventTracker = UserAnalytics.INSTANCE.getAnalyticsEventTracker();
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.setEventSuperProperty(
                    new UserAnalyticsEventSuperProperty.DocumentType(
                            UserAnalyticsMappersKt.mapToAnalyticsDocumentType(mDocument)
                    )
            );
            mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.SCREEN_SHOWN,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                            add(new UserAnalyticsEventProperty.DocumentId(mDocument.getId()));
                        }
                    }
            );
        }
        if (shouldAllowRetakeImages()) {
            ClickListenerExtKt.setIntervalClickListener(retakeImagesButton, v -> {
                trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY);
                navigateToCameraScreen(UserAnalyticsEvent.RETAKE_IMAGES_TAPPED);
            });
        } else {
            retakeImagesButton.setVisibility(GONE);
        }

        final View enterManuallyButton = mView.findViewById(R.id.gc_button_no_results_enter_manually);
        ClickListenerExtKt.setIntervalClickListener(enterManuallyButton, v -> {
            if (mUserAnalyticsEventTracker != null) {
                mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.ENTER_MANUALLY_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                            }
                        });
            }
            mListener.onEnterManuallyPressed();
        });

        bindViews(mView);

        if (mDocument.getType() == Document.Type.QRCode || mDocument.getType() == Document.Type.QR_CODE_MULTI_PAGE) {
            mTitleTextView.setText(mFragment.getActivity().getResources().getString(R.string.gc_noresults_header_qr));
        }

        setupTopBarNavigation();
        setupBottomBarNavigation();
        setUpList(mView);

        return mView;
    }

    private void handleOnBackPressed() {
        mFragment.getActivity().getOnBackPressedDispatcher().addCallback(mFragment.getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED);
            }
        });
    }

    private boolean isDocumentFromCameraScreen(Document document) {
        return document.getImportMethod() != Document.ImportMethod.OPEN_WITH && document.getSource().getName().equals("camera");
    }

    private boolean shouldAllowRetakeImages() {
        if (mDocument instanceof ImageMultiPageDocument) {
            ImageMultiPageDocument doc = (ImageMultiPageDocument) mDocument;
            boolean isImportedDocFound = false;
            int i = 0;

            while (!isImportedDocFound && i < doc.getDocuments().size()) {
                isImportedDocFound = !isDocumentFromCameraScreen(doc.getDocuments().get(i));
                i++;
            }

            return !isImportedDocFound;
        }

        return isDocumentFromCameraScreen(mDocument);
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
                            navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED);
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
                        navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED);
                    }))
            ));
        }

    }

    private void navigateToCameraScreen(UserAnalyticsEvent event) {
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(event,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                        }
                    });
        }
        mFragment.findNavController().navigate(NoResultsFragmentDirections.toCameraFragment());
    }

    private void bindViews(@NonNull final View view) {
        mTitleTextView = view.findViewById(R.id.gc_no_results_header);
    }
}
