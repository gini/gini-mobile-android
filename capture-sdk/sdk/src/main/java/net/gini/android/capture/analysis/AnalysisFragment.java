package net.gini.android.capture.analysis;

import static net.gini.android.capture.analysis.AnalysisFragmentImpl.INVOICE_SAVING_IN_PROGRESS_KEY;
import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.Document;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.internal.util.CancelListener;

/**
 * Internal use only.
 */
public class AnalysisFragment extends Fragment implements FragmentImplCallback,
        AnalysisFragmentInterface {

    private AnalysisFragmentImpl mFragmentImpl;
    private AnalysisFragmentListener mListener;
    private CancelListener mCancelListener;
    private ActivityResultLauncher<Intent> safFolderIntentLauncher;

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerSafFolderSelectionHandler();
        mFragmentImpl = createFragmentImpl();
        mFragmentImpl.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(
                INVOICE_SAVING_IN_PROGRESS_KEY,
                mFragmentImpl.getIsInvoiceSavingInProgress());
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

        mFragmentImpl.processSafFolderSelection(treeUri, data);
    }

    @Override
    public void executeSafIntent(Intent intent) {
        safFolderIntentLauncher.launch(intent);
    }

    @VisibleForTesting
    AnalysisFragmentImpl createFragmentImpl() {
        final AnalysisFragmentImpl fragmentImpl = AnalysisFragmentHelper.createFragmentImpl(this, mCancelListener,
                getArguments());
        AnalysisFragmentHelper.setListener(fragmentImpl, getActivity(), mListener);
        return fragmentImpl;
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
        return mFragmentImpl.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentImpl.onViewCreated(view, savedInstanceState);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mFragmentImpl.onDestroy();
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onResume() {
        super.onResume();
        mFragmentImpl.onResume();
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onStop() {
        super.onStop();
        mFragmentImpl.onStop();
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        if (mFragmentImpl != null) {
            mFragmentImpl.setListener(listener);
        }
        mListener = listener;
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
     *
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

    @NonNull
    @Override
    public NavController findNavController() {
        return NavHostFragment.findNavController(this);
    }

    @VisibleForTesting
    AnalysisFragmentImpl getFragmentImpl() {
        return mFragmentImpl;
    }
}
