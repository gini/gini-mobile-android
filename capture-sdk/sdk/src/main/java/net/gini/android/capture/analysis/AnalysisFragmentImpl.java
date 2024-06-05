package net.gini.android.capture.analysis;

import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.error.ErrorFragment;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsExtraProperties;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsHelperKt;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;

/**
 * Main logic implementation for analysis UI presented by {@link AnalysisFragment}
 */
class AnalysisFragmentImpl extends AnalysisScreenContract.View {

    protected static final Logger LOG = LoggerFactory.getLogger(AnalysisFragmentImpl.class);

    private final FragmentImplCallback mFragment;
    private final CancelListener mCancelListener;
    private TextView mAnalysisMessageTextView;
    private ImageView mImageDocumentView;
    private ConstraintLayout mLayoutRoot;
    private LinearLayout mAnalysisOverlay;
    private AnalysisHintsAnimator mHintsAnimator;
    private InjectedViewContainer<NavigationBarTopAdapter> topAdapterInjectedViewContainer;
    private InjectedViewContainer<CustomLoadingIndicatorAdapter> injectedLoadingIndicatorContainer;
    private boolean isScanAnimationActive;

    AnalysisFragmentImpl(final FragmentImplCallback fragment,
                         final CancelListener cancelListener,
                         @NonNull final Document document,
                         final String documentAnalysisErrorMessage) {
        mFragment = fragment;
        if (mFragment.getActivity() == null) {
            throw new IllegalStateException("Missing activity for fragment.");
        }
        mCancelListener = cancelListener;
        createPresenter(mFragment.getActivity(), document, documentAnalysisErrorMessage); // NOPMD - overridable for testing
    }

    @VisibleForTesting
    void createPresenter(@NonNull final Activity activity, @NonNull final Document document,
                         final String documentAnalysisErrorMessage) {

        addUserAnalyticEvents(document);
        new AnalysisScreenPresenter(activity, this, document,
                documentAnalysisErrorMessage);
    }

    private void addUserAnalyticEvents(@NonNull Document document) {
        String userAnalysisDocumentType = UserAnalyticsHelperKt.getDocumentTypeForUserAnalytics(document);
        UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(
                UserAnalyticsEvent.SCREEN_SHOWN,
                UserAnalyticsScreen.ANALYSIS,
                Collections.singletonMap(UserAnalyticsExtraProperties.DOCUMENT_TYPE, userAnalysisDocumentType)
        );
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        getPresenter().setListener(listener);
    }

    @Override
    void showScanAnimation() {
        mAnalysisMessageTextView.setVisibility(View.VISIBLE);
        isScanAnimationActive = true;
        injectedLoadingIndicatorContainer.modifyAdapterIfOwned(adapter -> {
            adapter.onVisible();
            return Unit.INSTANCE;
        });
    }

    @Override
    void hideScanAnimation() {
        isScanAnimationActive = false;
        injectedLoadingIndicatorContainer.modifyAdapterIfOwned(adapter -> {
            adapter.onHidden();
            return Unit.INSTANCE;
        });
        mAnalysisMessageTextView.setVisibility(View.GONE);
    }

    @Override
    CompletableFuture<Void> waitForViewLayout() {
        final View view = mFragment.getView();
        if (view == null) {
            return CompletableFuture.completedFuture(null);
        }
        final CompletableFuture<Void> future = new CompletableFuture<>();
        LOG.debug("Observing the view layout");
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        future.complete(null);
                        mHintsAnimator.setContainerViewHeight(view.getHeight());
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
        view.requestLayout();
        return future;
    }

    @Override
    void showPdfInfoPanel() {
        mAnalysisOverlay.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    void showPdfTitle(@NonNull final String title) {
        mAnalysisMessageTextView.setText(mFragment.getActivity().getString(R.string.gc_pdf_analysis_activity_indicator_message, title));
    }

    @Override
    Size getPdfPreviewSize() {
        return new Size(mImageDocumentView.getWidth(), mImageDocumentView.getHeight());
    }

    @Override
    void showBitmap(@Nullable final Bitmap bitmap, final int rotationForDisplay) {
        rotateDocumentImageView(rotationForDisplay);
        mImageDocumentView.setImageBitmap(bitmap);
    }

    @Override
    void showAlertDialog(@NonNull final String message, @NonNull final String positiveButtonTitle,
                         @NonNull final DialogInterface.OnClickListener positiveButtonClickListener,
                         @Nullable final String negativeButtonTitle,
                         @Nullable final DialogInterface.OnClickListener negativeButtonClickListener,
                         @Nullable final DialogInterface.OnCancelListener cancelListener) {
        mFragment.showAlertDialog(message, positiveButtonTitle, positiveButtonClickListener,
                negativeButtonTitle, negativeButtonClickListener, cancelListener);
    }

    @Override
    void showHints(final List<AnalysisHint> hints) {
        mHintsAnimator.stop();
        mHintsAnimator.setHints(hints);
        mHintsAnimator.start();
    }

    @Override
    void showError(String error, Document document) {
        ErrorFragment.Companion.navigateToErrorFragment(
                mFragment.findNavController(),
                AnalysisFragmentDirections.toErrorFragmentWithErrorMessage(error, document)
        );
    }

    @Override
    void showError(ErrorType errorType, Document document) {
        ErrorFragment.Companion.navigateToErrorFragment(
                mFragment.findNavController(),
                AnalysisFragmentDirections.toErrorFragmentWithErrorType(errorType, document)
        );
    }

    private void rotateDocumentImageView(final int rotationForDisplay) {
        if (rotationForDisplay == 0) {
            return;
        }
        int newWidth = mLayoutRoot.getWidth();
        int newHeight = mLayoutRoot.getHeight();
        if (rotationForDisplay == 90 || rotationForDisplay == 270) {
            newWidth = mLayoutRoot.getHeight();
            newHeight = mLayoutRoot.getWidth();
        }

        final FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mImageDocumentView.getLayoutParams();
        layoutParams.width = newWidth;
        layoutParams.height = newHeight;
        mImageDocumentView.setLayoutParams(layoutParams);
        mImageDocumentView.setRotation(rotationForDisplay);
    }

    public void onCreate(final Bundle savedInstanceState) {
        final Activity activity = mFragment.getActivity();
        if (activity == null) {
            return;
        }
        forcePortraitOrientationOnPhones(activity);
    }

    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_analysis, container, false);
        bindViews(view);
        setTopBarInjectedViewContainer();
        setLoadingIndicatorViewContainer();
        createHintsAnimator(view);
        return view;
    }

    private void bindViews(@NonNull final View view) {
        mLayoutRoot = view.findViewById(R.id.gc_layout_root);
        mImageDocumentView = view.findViewById(R.id.gc_image_picture);
        mAnalysisMessageTextView = view.findViewById(R.id.gc_analysis_message);
        mAnalysisOverlay = view.findViewById(R.id.gc_analysis_overlay);
        topAdapterInjectedViewContainer = view.findViewById(R.id.gc_navigation_top_bar);
        injectedLoadingIndicatorContainer = view.findViewById(R.id.gc_injected_loading_indicator_container);
    }

    private void createHintsAnimator(@NonNull final View view) {
        final ImageView hintImageView = view.findViewById(R.id.gc_analysis_hint_image);
        final TextView hintTextView = view.findViewById(R.id.gc_analysis_hint_text);
        final View hintContainer = view.findViewById(R.id.gc_analysis_hint_container);
        final TextView hintHeadlineTextView = view.findViewById(R.id.gc_analysis_hint_headline);
        mHintsAnimator = new AnalysisHintsAnimator(mFragment.getActivity().getApplication(),
                hintContainer, hintImageView, hintTextView, hintHeadlineTextView);
    }

    private void setTopBarInjectedViewContainer() {
        if (GiniCapture.hasInstance()) {
            topAdapterInjectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(), injectedViewAdapter -> {
                if (mFragment.getActivity() == null)
                    return;

                injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);
                injectedViewAdapter.setTitle(mFragment.getActivity().getResources().getString(R.string.gc_title_analysis));

                injectedViewAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> {
                    onBack();
                }));
            }));
        }
    }

    private void setLoadingIndicatorViewContainer() {
        if (GiniCapture.hasInstance()) {
            injectedLoadingIndicatorContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getLoadingIndicatorAdapterInstance(),
                    injectedViewAdapter -> {
                        if (isScanAnimationActive) {
                            injectedViewAdapter.onVisible();
                        } else {
                            injectedViewAdapter.onHidden();
                        }
                    }));
        }
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
                setEnabled(false);
                remove();
                onBack();
            }
        });
    }

    private void onBack() {
        boolean popBackStack = mFragment.findNavController().popBackStack();
        if (!popBackStack) {
            UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(UserAnalyticsEvent.CLOSE_TAPPED, UserAnalyticsScreen.ANALYSIS);
            trackAnalysisScreenEvent(AnalysisScreenEvent.CANCEL);
            mCancelListener.onCancelFlow();
        }
    }

    public void onResume() {
        getPresenter().start();
    }

    public void onDestroy() {
        getPresenter().stop();
        final Activity activity = mFragment.getActivity();
        if (activity != null && activity.isFinishing()) {
            getPresenter().finish();
        }
    }

    void onStop() {
        mHintsAnimator.stop();
    }

}
