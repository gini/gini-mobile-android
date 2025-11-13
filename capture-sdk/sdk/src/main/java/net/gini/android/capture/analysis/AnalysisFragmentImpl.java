package net.gini.android.capture.analysis;

import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;
import static net.gini.android.capture.util.SharedPreferenceHelper.SAF_STORAGE_URI_KEY;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.education.EducationCompleteListener;
import net.gini.android.capture.error.ErrorFragment;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.internal.util.Size;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsMappersKt;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty;
import net.gini.android.capture.util.SAFHelper;
import net.gini.android.capture.util.SharedPreferenceHelper;
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;

/**
 * Main logic implementation for analysis UI presented by {@link AnalysisFragment}
 */
class AnalysisFragmentImpl extends AnalysisScreenContract.View {

    protected static final Logger LOG = LoggerFactory.getLogger(AnalysisFragmentImpl.class);
    protected static final String INVOICE_SAVING_IN_PROGRESS_KEY = "invoiceSavingInProgress";
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
    private final UserAnalyticsScreen screenName = UserAnalyticsScreen.Analysis.INSTANCE;
    UserAnalyticsEventTracker userAnalyticsEventTracker = UserAnalytics.INSTANCE.getAnalyticsEventTracker();
    AnalysisFragmentExtension fragmentExtension = new AnalysisFragmentExtension();

    private boolean isInvoiceSavingInProgress = false;

    AnalysisFragmentImpl(final FragmentImplCallback fragment,
                         final CancelListener cancelListener,
                         @NonNull final Document document,
                         final String documentAnalysisErrorMessage,
                         final Boolean mIsInvoiceSavingEnabled) {
        mFragment = fragment;
        if (mFragment.getActivity() == null) {
            throw new IllegalStateException("Missing activity for fragment.");
        }
        mCancelListener = cancelListener;
        createPresenter(mFragment.getActivity(), document,
                documentAnalysisErrorMessage,
                mIsInvoiceSavingEnabled
        ); // NOPMD - overridable for testing
    }

    @VisibleForTesting
    void createPresenter(@NonNull final Activity activity, @NonNull final Document document,
                         final String documentAnalysisErrorMessage,
                         final Boolean mIsInvoiceSavingEnabled) {

        addUserAnalyticEvents(document);
        new AnalysisScreenPresenter(activity, this, document,
                documentAnalysisErrorMessage, mIsInvoiceSavingEnabled);
    }

    private void addUserAnalyticEvents(@NonNull Document document) {
        if (userAnalyticsEventTracker != null) {
            userAnalyticsEventTracker.setEventSuperProperty(
                    new UserAnalyticsEventSuperProperty.DocumentType(
                            UserAnalyticsMappersKt.mapToAnalyticsDocumentType(document)
                    )
            );

            userAnalyticsEventTracker.trackEvent(
                    UserAnalyticsEvent.SCREEN_SHOWN,
                    new HashSet<UserAnalyticsEventProperty>() {
                        {
                            add(new UserAnalyticsEventProperty.Screen(screenName));
                        }
                    }
            );
        }
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        getPresenter().setListener(listener);
    }

    @Override
    void showScanAnimation(Boolean isSavingInvoicesLocallyEnabled) {
        mAnalysisMessageTextView.setVisibility(View.VISIBLE);
        isScanAnimationActive = true;
        if (injectedLoadingIndicatorContainer != null)
            injectedLoadingIndicatorContainer.modifyAdapterIfOwned(adapter -> {
                adapter.onVisible();
                return Unit.INSTANCE;
            });

        Context context = mFragment.getActivity();
        if (context == null) return;

        int messageResId = isSavingInvoicesLocallyEnabled
                ? R.string.gc_analysis_activity_indicator_message_save_invoices_locally
                : R.string.gc_analysis_activity_indicator_message;

        mAnalysisMessageTextView.setText(context.getString(messageResId));
    }

    @Override
    void hideScanAnimation() {
        isScanAnimationActive = false;
        if (injectedLoadingIndicatorContainer != null)
            injectedLoadingIndicatorContainer.modifyAdapterIfOwned(adapter -> {
                adapter.onHidden();
                return Unit.INSTANCE;
            });
        if (mAnalysisMessageTextView != null)
            mAnalysisMessageTextView.setVisibility(View.GONE);
    }

    void showEducation(EducationCompleteListener listener) {
        fragmentExtension.showEducation(() -> {
            hideEducation();
            listener.onComplete();
            return Unit.INSTANCE;
        });
    }

    /**
     * - Handles the folder selected by the user and saves files into it.
     * - Persists the folder permission via SAF and stores the folder URI in shared preferences.
     * - Storing the URI ensures that future saves can bypass the SAF picker dialog and write
     *   directly to the folder. otherwise, the SAF dialog would appear every time a file is saved.
     *
     * @param folderUri The URI of the folder selected by the user.
     * @param intent The Intent returned from the folder picker containing the folder data.
     */
    public void processSafFolderSelection(Uri folderUri, Intent intent) {
        Context context = mFragment.getActivity();
        if (context == null) return;

        SAFHelper.INSTANCE.persistFolderPermission(context, intent);

        // saving SAF URI to shared preferences for future use, so the SAF dialog doesn't
        // have to be shown every time a file is saved

        SharedPreferenceHelper.INSTANCE.saveString(
                SAF_STORAGE_URI_KEY,
                folderUri.toString(), context);

        int result = SAFHelper.INSTANCE.saveFilesToFolder(
                context , folderUri,
                getPresenter().assembleMultiPageDocumentUris()
        );

        notifyUserAboutSafResult(result, context);
    }

    private void notifyUserAboutSafResult(int count, @NonNull Context context) {
        if (count > 0)
            Toast.makeText(context,
                    context.getString(R.string.gc_invoice_saving_success_toast_text),
                    Toast.LENGTH_LONG).show();
    }

    /**
     * Checks if the app has permission to save files in the given folder URI, selected by the user.
     *
     * @param folderUri The folder URI to check.
     * @return True if the URI is valid and write permission is granted.
     */
    private Boolean haveSavePermission(String folderUri) {
        return folderUri != null && !folderUri.isEmpty() &&
                SAFHelper.INSTANCE.hasWritePermission(
                        Objects.requireNonNull(mFragment.getActivity()), Uri.parse(folderUri));
    }

    private void saveInvoices(String folderUri) {
        Context context = mFragment.getActivity();
        if (context == null) return;

        Uri treeUri = Uri.parse(folderUri);

        int result = SAFHelper.INSTANCE.saveFilesToFolder(
                context,
                treeUri,
                getPresenter().assembleMultiPageDocumentUris());

        notifyUserAboutSafResult(result, context);

        getPresenter().resumeInterruptedFlow();
        isInvoiceSavingInProgress = false;
    }

    @Override
    void processInvoiceSaving() {
        isInvoiceSavingInProgress = true;
        getPresenter().updateInvoiceSavingState(isInvoiceSavingInProgress);

        String folderUri = SharedPreferenceHelper.INSTANCE.getString(
                SAF_STORAGE_URI_KEY,
                Objects.requireNonNull(mFragment.getActivity()));

        Boolean haveSavePermission = haveSavePermission(folderUri);
        if (haveSavePermission)
            saveInvoices(folderUri);
        else {
            getPresenter().releaseMutexForEducation();
            mFragment.executeSafIntent(SAFHelper.INSTANCE.createFolderPickerIntent());
        }
    }

    public Boolean getIsInvoiceSavingInProgress() {
        return isInvoiceSavingInProgress;
    }

    void hideEducation() {
        fragmentExtension.hideEducation();
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

    // Required by superclass
    public void onCreate(final Bundle savedInstanceState) {
        if (savedInstanceState != null)
            isInvoiceSavingInProgress = savedInstanceState.getBoolean(INVOICE_SAVING_IN_PROGRESS_KEY, false);
    }


    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_analysis, container, false);
        bindViews(view);
        fragmentExtension.bindViews(view);
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
            if (userAnalyticsEventTracker != null) {
                userAnalyticsEventTracker.trackEvent(UserAnalyticsEvent.CLOSE_TAPPED,
                        new HashSet<UserAnalyticsEventProperty>() {
                            {
                                add(new UserAnalyticsEventProperty.Screen(screenName));
                            }
                        });
            }

            trackAnalysisScreenEvent(AnalysisScreenEvent.CANCEL);
            mCancelListener.onCancelFlow();
        }
    }

    public void onResume() {
        getPresenter().updateInvoiceSavingState(isInvoiceSavingInProgress);
        getPresenter().start();
    }

    public void onDestroy() {
        final Activity activity = mFragment.getActivity();
        if (activity != null) {
            if (!activity.isChangingConfigurations()) getPresenter().stop();
            if (activity.isFinishing()) getPresenter().finish();
        }
    }

    void onStop() {
        mHintsAnimator.stop();
    }

}
