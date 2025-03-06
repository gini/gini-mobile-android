package net.gini.android.capture.noresults;

import static android.view.View.GONE;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
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
    private final Document mDocument;
    private final CancelListener mCancelListener;
    private EnterManuallyButtonListener mListener;
    private TextView mTitleTextView;
    private UserAnalyticsEventTracker mUserAnalyticsEventTracker;

    private InjectedViewContainer<NavigationBarTopAdapter> topAdapterInjectedViewContainer;
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
        forcePortraitOrientationOnPhones(mFragment.getActivity());
        // Clear the image from the memory store because the user can only exit for manual entry or in some cases
        // can go back to the camera to take new pictures
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
        }
    }

    View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                      final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_noresults, container, false);
        final View retakeImagesButton = view.findViewById(R.id.gc_button_no_results_retake_images);
        handleOnBackPressed();
        mUserAnalyticsEventTracker = UserAnalytics.INSTANCE.getAnalyticsEventTracker();
        if (mUserAnalyticsEventTracker != null) {
            mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.SCREEN_SHOWN,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                            add(new UserAnalyticsEventProperty.DocumentId(mDocument.getId()));
                            add(new UserAnalyticsEventProperty.DocumentType(UserAnalyticsMappersKt.mapToAnalyticsDocumentType(mDocument)));
                        }
                    }
            );
        }
        if (shouldAllowRetakeImages()) {
            ClickListenerExtKt.setIntervalClickListener(retakeImagesButton, v -> {
                if (mUserAnalyticsEventTracker != null) {
                    mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.RETAKE_IMAGES_TAPPED,
                            new HashSet<UserAnalyticsEventProperty>() {
                                {
                                    add(new UserAnalyticsEventProperty.Screen(screenName));
                                }
                            });
                }
                trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY);
                mFragment.findNavController().navigate(NoResultsFragmentDirections.toCameraFragment());
            });
        } else {
            retakeImagesButton.setVisibility(GONE);
        }

        final View enterManuallyButton = view.findViewById(R.id.gc_button_no_results_enter_manually);
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

        bindViews(view);

        if (mDocument.getType() == Document.Type.QRCode || mDocument.getType() == Document.Type.QR_CODE_MULTI_PAGE) {
            mTitleTextView.setText(mFragment.getActivity().getResources().getString(R.string.gc_noresults_header_qr));
        }

        setTopBarInjectedViewContainer();
        setUpList(view);

        return view;
    }

    private void handleOnBackPressed() {
        mFragment.getActivity().getOnBackPressedDispatcher().addCallback(mFragment.getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mUserAnalyticsEventTracker != null) {
                    mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.CLOSE_TAPPED,
                            new HashSet<UserAnalyticsEventProperty>() {
                                {
                                    add(new UserAnalyticsEventProperty.Screen(screenName));
                                }
                            });
                }
                remove();

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

    private void setTopBarInjectedViewContainer() {
        if (GiniCapture.hasInstance()) {
            topAdapterInjectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(),
                    injectedViewAdapter -> {
                        if (mFragment.getActivity() == null)
                            return;

                        injectedViewAdapter.setTitle(mFragment.getActivity().getResources().getString(R.string.gc_title_no_results));

                        injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);
                        injectedViewAdapter.setOnNavButtonClickListener(new IntervalClickListener(view -> {
                            if (mFragment.getActivity() != null) {
                                if (mUserAnalyticsEventTracker != null) {
                                    mUserAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.CLOSE_TAPPED,
                                            new HashSet<UserAnalyticsEventProperty>() {
                                                {
                                                    add(new UserAnalyticsEventProperty.Screen(screenName));
                                                }
                                            });
                                }
                                mCancelListener.onCancelFlow();
                            }
                        }));
                    })
            );
        }
    }

    private void bindViews(@NonNull final View view) {
        topAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
        mTitleTextView = view.findViewById(R.id.gc_no_results_header);
    }
}
