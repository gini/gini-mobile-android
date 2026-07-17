package net.gini.android.capture.analysis;

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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.BankSDKBridge;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.warning.WarningBottomSheet;
import net.gini.android.capture.analysis.warning.WarningType;
import net.gini.android.capture.error.ErrorFragment;
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
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

import kotlin.Unit;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;
import static net.gini.android.capture.util.SharedPreferenceHelper.SAF_STORAGE_URI_KEY;

/**
 * Internal use only.
 */
public class AnalysisFragment extends Fragment implements FragmentImplCallback,
        AnalysisFragmentInterface {

    protected static final Logger LOG = LoggerFactory.getLogger(AnalysisFragment.class);
    static final String INVOICE_SAVING_IN_PROGRESS_KEY = "invoiceSavingInProgress";
    private static final String WARNING_TAG = "WarningBottomSheet";

    private AnalysisViewModel mViewModel;
    private AnalysisFragmentListener mListener;
    private BankSDKBridge bankSDKBridge;
    private FragmentManager fragmentManager;

    private CancelListener mCancelListener;
    private ActivityResultLauncher<Intent> safFolderIntentLauncher;

    private TextView mAnalysisMessageTextView;
    private ImageView mImageDocumentView;
    private ConstraintLayout mLayoutRoot;
    private LinearLayout mAnalysisOverlay;
    private AnalysisHintsAnimator mHintsAnimator;
    private InjectedViewContainer<NavigationBarTopAdapter> topAdapterInjectedViewContainer;
    private InjectedViewContainer<CustomLoadingIndicatorAdapter> injectedLoadingIndicatorContainer;
    private boolean isScanAnimationActive;
    private final UserAnalyticsScreen screenName = UserAnalyticsScreen.Analysis.INSTANCE;
    UserAnalyticsEventTracker userAnalyticsEventTracker;
    AnalysisFragmentExtension fragmentExtension = new AnalysisFragmentExtension();

    private boolean isInvoiceSavingInProgress = false;
    private boolean mIsInvoiceSavingEnabled = false;

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerSafFolderSelectionHandler();
        fragmentManager = requireActivity().getSupportFragmentManager();
        userAnalyticsEventTracker = UserAnalytics.INSTANCE.getAnalyticsEventTracker();
        if (savedInstanceState != null) {
            isInvoiceSavingInProgress =
                    savedInstanceState.getBoolean(INVOICE_SAVING_IN_PROGRESS_KEY, false);
        }
        createViewModel();
    }

    private void createViewModel() {
        final Bundle arguments = getArguments();
        final Document document = AnalysisFragmentHelper.requireDocument(arguments);
        final String analysisErrorMessage =
                AnalysisFragmentHelper.getDocumentAnalysisErrorMessage(arguments);
        mIsInvoiceSavingEnabled = AnalysisFragmentHelper.isInvoiceSavingEnabled(arguments);

        addUserAnalyticEvents(document);

        mViewModel = new ViewModelProvider(this,
                new AnalysisViewModelFactory(requireActivity().getApplication(), document,
                        analysisErrorMessage, mIsInvoiceSavingEnabled))
                .get(AnalysisViewModel.class);

        mListener = AnalysisFragmentHelper.resolveListener(getActivity(), mListener);
        if (bankSDKBridge != null) {
            mViewModel.setBankSDKBridge(
                    AnalysisFragmentHelper.resolveBankSDKBridge(getActivity(), bankSDKBridge));
        }
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(INVOICE_SAVING_IN_PROGRESS_KEY, isInvoiceSavingInProgress);
    }

    private void registerSafFolderSelectionHandler() {
        safFolderIntentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleFolderResult
        );
    }

    private void handleFolderResult(ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK) return;

        Intent data = result.getData();
        if (data == null) return;

        Uri treeUri = data.getData();
        if (treeUri == null) return;

        processSafFolderSelection(treeUri, data);
    }

    @Override
    public void executeSafIntent(Intent intent) {
        safFolderIntentLauncher.launch(intent);
    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        final LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);
        return getLayoutInflaterWithGiniCaptureTheme(this, inflater);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gc_fragment_analysis, container, false);
        bindViews(view);
        fragmentExtension.bindViews(view);
        setTopBarInjectedViewContainer();
        setLoadingIndicatorViewContainer();
        createHintsAnimator(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleOnBackPressed();
        observeViewModel();
    }

    private void observeViewModel() {
        mViewModel.getScanAnimationActive().observe(getViewLifecycleOwner(), active -> {
            if (Boolean.TRUE.equals(active)) {
                showScanAnimation();
            } else {
                hideScanAnimation();
            }
        });
        mViewModel.getEvents().observe(getViewLifecycleOwner(), consumableEvent -> {
            if (consumableEvent.getContentIfNotHandled() != null) {
                AnalysisViewEvent event;
                while ((event = mViewModel.pollEvent()) != null) {
                    handleViewEvent(event);
                }
            }
        });
    }

    private void handleViewEvent(@NonNull final AnalysisViewEvent event) {
        if (event instanceof AnalysisViewEvent.WaitForViewLayout) {
            waitForViewLayout();
        } else if (event instanceof AnalysisViewEvent.ShowPdfInfoPanel) {
            showPdfInfoPanel();
        } else if (event instanceof AnalysisViewEvent.ShowPdfTitle) {
            showPdfTitle(((AnalysisViewEvent.ShowPdfTitle) event).getTitle());
        } else if (event instanceof AnalysisViewEvent.ShowBitmap) {
            final AnalysisViewEvent.ShowBitmap showBitmap = (AnalysisViewEvent.ShowBitmap) event;
            showBitmap(showBitmap.getBitmap(), showBitmap.getRotationForDisplay());
        } else if (event instanceof AnalysisViewEvent.ShowAlertDialog) {
            final AnalysisViewEvent.ShowAlertDialog dialog =
                    (AnalysisViewEvent.ShowAlertDialog) event;
            showAlertDialog(dialog.getMessage(), dialog.getPositiveButtonTitle(),
                    dialog.getPositiveButtonClickListener(), dialog.getNegativeButtonTitle(),
                    dialog.getNegativeButtonClickListener(), dialog.getCancelListener());
        } else if (event instanceof AnalysisViewEvent.ShowHints) {
            showHints(((AnalysisViewEvent.ShowHints) event).getHints());
        } else if (event instanceof AnalysisViewEvent.ShowErrorMessage) {
            final AnalysisViewEvent.ShowErrorMessage error =
                    (AnalysisViewEvent.ShowErrorMessage) event;
            ErrorFragment.Companion.navigateToErrorFragment(
                    findNavController(),
                    AnalysisFragmentDirections.toErrorFragmentWithErrorMessage(
                            error.getMessage(), error.getDocument())
            );
        } else if (event instanceof AnalysisViewEvent.ShowErrorType) {
            final AnalysisViewEvent.ShowErrorType error = (AnalysisViewEvent.ShowErrorType) event;
            ErrorFragment.Companion.navigateToErrorFragment(
                    findNavController(),
                    AnalysisFragmentDirections.toErrorFragmentWithErrorType(
                            error.getErrorType(), error.getDocument())
            );
        } else if (event instanceof AnalysisViewEvent.ShowAlreadyPaidWarning) {
            final AnalysisViewEvent.ShowAlreadyPaidWarning warning =
                    (AnalysisViewEvent.ShowAlreadyPaidWarning) event;
            showWarning(warning.getWarningType(), warning.getOnProceed());
        } else if (event instanceof AnalysisViewEvent.ShowPaymentDueHint) {
            final AnalysisViewEvent.ShowPaymentDueHint hint =
                    (AnalysisViewEvent.ShowPaymentDueHint) event;
            fragmentExtension.showPaymentDueHint(() -> {
                        hint.getDismissListener().onDismiss();
                        return Unit.INSTANCE;
                    },
                    hint.getDueDate());
        } else if (event instanceof AnalysisViewEvent.ShowEducation) {
            final AnalysisViewEvent.ShowEducation education =
                    (AnalysisViewEvent.ShowEducation) event;
            fragmentExtension.showEducation(() -> {
                hideEducation();
                education.getListener().onComplete();
                return Unit.INSTANCE;
            });
        } else if (event instanceof AnalysisViewEvent.ProcessInvoiceSaving) {
            processInvoiceSaving();
        } else if (event instanceof AnalysisViewEvent.NotifyError) {
            if (mListener != null) {
                mListener.onError(((AnalysisViewEvent.NotifyError) event).getError());
            }
        } else if (event instanceof AnalysisViewEvent.NotifyExtractionsAvailable) {
            final AnalysisViewEvent.NotifyExtractionsAvailable extractions =
                    (AnalysisViewEvent.NotifyExtractionsAvailable) event;
            if (mListener != null) {
                mListener.onExtractionsAvailable(extractions.getExtractions(),
                        extractions.getCompoundExtractions(), extractions.getReturnReasons());
            }
        } else if (event instanceof AnalysisViewEvent.NotifyProceedToNoExtractionsScreen) {
            if (mListener != null) {
                mListener.onProceedToNoExtractionsScreen(
                        ((AnalysisViewEvent.NotifyProceedToNoExtractionsScreen) event)
                                .getDocument());
            }
        } else if (event instanceof AnalysisViewEvent.NotifyDefaultPDFAppAlertDialogCancelled) {
            if (mListener != null) {
                mListener.onDefaultPDFAppAlertDialogCancelled();
            }
        }
    }

    private void waitForViewLayout() {
        final View view = getView();
        if (view == null) {
            notifyLayoutFinished();
            return;
        }
        LOG.debug("Observing the view layout");
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        notifyLayoutFinished();
                        mHintsAnimator.setContainerViewHeight(view.getHeight());
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
        view.requestLayout();
    }

    private void notifyLayoutFinished() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mViewModel.onViewLayoutFinished(getPdfPreviewSize(), activity);
    }

    @NonNull
    private Size getPdfPreviewSize() {
        if (mImageDocumentView == null) {
            return new Size(0, 0);
        }
        return new Size(mImageDocumentView.getWidth(), mImageDocumentView.getHeight());
    }

    private void showScanAnimation() {
        if (mAnalysisMessageTextView == null) {
            return;
        }
        mAnalysisMessageTextView.setVisibility(View.VISIBLE);
        isScanAnimationActive = true;
        if (injectedLoadingIndicatorContainer != null)
            injectedLoadingIndicatorContainer.modifyAdapterIfOwned(adapter -> {
                adapter.onVisible();
                return Unit.INSTANCE;
            });

        Context context = getActivity();
        if (context == null) return;

        int messageResId = mIsInvoiceSavingEnabled
                ? R.string.gc_analysis_activity_indicator_message_save_invoices_locally
                : R.string.gc_analysis_activity_indicator_message;

        mAnalysisMessageTextView.setText(context.getString(messageResId));
    }

    private void hideScanAnimation() {
        isScanAnimationActive = false;
        if (injectedLoadingIndicatorContainer != null)
            injectedLoadingIndicatorContainer.modifyAdapterIfOwned(adapter -> {
                adapter.onHidden();
                return Unit.INSTANCE;
            });
        if (mAnalysisMessageTextView != null)
            mAnalysisMessageTextView.setVisibility(View.GONE);
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
    void processSafFolderSelection(Uri folderUri, Intent intent) {
        Context context = getActivity();
        if (context == null) return;

        SAFHelper.INSTANCE.persistFolderPermission(context, intent);

        // saving SAF URI to shared preferences for future use, so the SAF dialog doesn't
        // have to be shown every time a file is saved

        SharedPreferenceHelper.INSTANCE.saveString(
                SAF_STORAGE_URI_KEY,
                folderUri.toString(), context);

        int result = SAFHelper.INSTANCE.saveFilesToFolder(
                context, folderUri,
                mViewModel.assembleMultiPageDocumentUris()
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
                        Objects.requireNonNull(getActivity()), Uri.parse(folderUri));
    }

    private void saveInvoices(String folderUri) {
        Context context = getActivity();
        if (context == null) return;

        Uri treeUri = Uri.parse(folderUri);

        int result = SAFHelper.INSTANCE.saveFilesToFolder(
                context,
                treeUri,
                mViewModel.assembleMultiPageDocumentUris());

        notifyUserAboutSafResult(result, context);

        mViewModel.resumeInterruptedFlow();
        isInvoiceSavingInProgress = false;
    }

    private void processInvoiceSaving() {
        if (getActivity() == null) return;
        isInvoiceSavingInProgress = true;
        mViewModel.updateInvoiceSavingState(isInvoiceSavingInProgress);

        String folderUri = SharedPreferenceHelper.INSTANCE.getString(
                SAF_STORAGE_URI_KEY,
                Objects.requireNonNull(getActivity()));

        Boolean haveSavePermission = haveSavePermission(folderUri);
        if (haveSavePermission)
            saveInvoices(folderUri);
        else {
            executeSafIntent(SAFHelper.INSTANCE.createFolderPickerIntent());
        }
    }

    void hideEducation() {
        fragmentExtension.hideEducation();
    }

    private void showPdfInfoPanel() {
        mAnalysisOverlay.setBackgroundColor(Color.TRANSPARENT);
    }

    private void showPdfTitle(@NonNull final String title) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mAnalysisMessageTextView.setText(
                activity.getString(R.string.gc_pdf_analysis_activity_indicator_message, title));
    }

    private void showBitmap(@Nullable final Bitmap bitmap, final int rotationForDisplay) {
        rotateDocumentImageView(rotationForDisplay);
        mImageDocumentView.setImageBitmap(bitmap);
    }

    private void showHints(final List<AnalysisHint> hints) {
        mHintsAnimator.stop();
        mHintsAnimator.setHints(hints);
        mHintsAnimator.start();
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
        mHintsAnimator = new AnalysisHintsAnimator(requireActivity().getApplication(),
                hintContainer, hintImageView, hintTextView, hintHeadlineTextView);
    }

    private void setTopBarInjectedViewContainer() {
        if (GiniCapture.hasInstance()) {
            topAdapterInjectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(), injectedViewAdapter -> {
                if (getActivity() == null)
                    return;

                injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE);
                injectedViewAdapter.setTitle(getActivity().getResources().getString(R.string.gc_title_analysis));

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

    private void handleOnBackPressed() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                remove();
                onBack();
            }
        });
    }

    private void onBack() {
        boolean popBackStack = findNavController().popBackStack();
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

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onResume() {
        super.onResume();
        mViewModel.updateInvoiceSavingState(isInvoiceSavingInProgress);
        mViewModel.onStart();
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mHintsAnimator != null) {
            mHintsAnimator.stop();
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        final Activity activity = getActivity();
        if (activity != null && mViewModel != null) {
            if (!activity.isChangingConfigurations()) {
                mViewModel.onStop();
            }
            if (activity.isFinishing()) {
                mViewModel.finish();
            }
        }
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public void setBankSDKBridge(@Nullable BankSDKBridge bankSDKBridge) {
        if (mViewModel != null) {
            mViewModel.setBankSDKBridge(bankSDKBridge);
        }
        if (bankSDKBridge != null) {
            this.bankSDKBridge = bankSDKBridge;
        }
    }

    public void setCancelListener(@NonNull final CancelListener cancelListener) {
        mCancelListener = cancelListener;
    }

    /**
     * <p>
     * Factory method for creating a new instance of the Fragment using the provided document.
     * </p>
     * <p>
     * You may pass in an optional analysis error message. This error message is shown to the user
     * with a retry button.
     * </p>
     * <p>
     * <b>Note:</b> Always use this method to create new instances. Document is required and an
     * exception is thrown if it's missing.
     * </p>
     *
     * @param document                     must be the {@link Document} from {@link
     *                                     ReviewFragmentListener#onProceedToAnalysisScreen
     *                                     (Document)}
     * @param documentAnalysisErrorMessage an optional error message shown to the user
     * @return a new instance of the Fragment
     */
    public static AnalysisFragment createInstance(@NonNull final Document document,
                                                  @Nullable final String documentAnalysisErrorMessage,
                                                  final Boolean saveInvoicesLocally) {
        final AnalysisFragment fragment = new AnalysisFragment();
        fragment.setArguments(
                AnalysisFragmentHelper.createArguments(document, documentAnalysisErrorMessage,
                        saveInvoicesLocally));
        return fragment;
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

    @Override
    public void showWarning(@NonNull WarningType type, @NonNull Runnable onProceed) {
        WarningBottomSheet sheet = (WarningBottomSheet) fragmentManager.findFragmentByTag(WARNING_TAG);
        if (sheet == null) {
            sheet = WarningBottomSheet.Companion.newInstance(type);
        }

        sheet.setCancelable(false);
        sheet.setListener(makeWarningListener(onProceed));
        if (!sheet.isAdded()) {
            if (!fragmentManager.isStateSaved()) {
                sheet.show(fragmentManager, WARNING_TAG);
            } else {
                fragmentManager.beginTransaction().add(sheet, WARNING_TAG).commitAllowingStateLoss();
            }
        }
    }

    private WarningBottomSheet.Listener makeWarningListener(@NonNull Runnable onProceed) {
        return new WarningBottomSheet.Listener() {
            @Override
            public void onCancelAction() {
                if (getActivity() != null) ImageDiskStore.clear(getActivity());

                if (mCancelListener != null) {
                    mCancelListener.onCancelFlow();
                }
            }

            @Override
            public void onProceedAction() {
                onProceed.run();
            }
        };
    }

    @NonNull
    @Override
    public NavController findNavController() {
        return NavHostFragment.findNavController(this);
    }

    @VisibleForTesting
    AnalysisViewModel getViewModel() {
        return mViewModel;
    }
}
