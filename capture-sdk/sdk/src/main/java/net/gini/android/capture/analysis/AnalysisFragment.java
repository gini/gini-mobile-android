package net.gini.android.capture.analysis;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import net.gini.android.capture.Document;
import net.gini.android.capture.analysis.warning.WarningType;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.internal.util.CancelListener;
import net.gini.android.capture.analysis.warning.WarningBottomSheet;

/**
 * Internal use only.
 */
public class AnalysisFragment extends Fragment implements FragmentImplCallback,
        AnalysisFragmentInterface {

    private static final String WARNING_TAG = "WarningBottomSheet";
    private AnalysisFragmentImpl mFragmentImpl;
    private AnalysisFragmentListener mListener;
    private CancelListener mCancelListener;

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentImpl = createFragmentImpl();
        mFragmentImpl.onCreate(savedInstanceState);
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
     * @return a new instance of the Fragment
     */
    public static AnalysisFragment createInstance(@NonNull final Document document,
                                                  @Nullable final String documentAnalysisErrorMessage) {
        final AnalysisFragment fragment = new AnalysisFragment();
        fragment.setArguments(
                AnalysisFragmentHelper.createArguments(document, documentAnalysisErrorMessage));
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
        FragmentManager fm = getParentFragmentManager();

        WarningBottomSheet sheet = (WarningBottomSheet) fm.findFragmentByTag(WARNING_TAG);
        if (sheet == null) {
            sheet = WarningBottomSheet.Companion.newInstance(type);
        }

        sheet.setCancelable(false);
        sheet.setListener(makeWarningListener(onProceed));
        if (!sheet.isAdded()) {
            if (!fm.isStateSaved()) {
                sheet.show(fm, WARNING_TAG);
            } else {
                fm.beginTransaction().add(sheet, WARNING_TAG).commitAllowingStateLoss();
            }
        }
    }

    private WarningBottomSheet.Listener makeWarningListener(@NonNull Runnable onProceed) {
        return new WarningBottomSheet.Listener() {
            @Override
            public void onCancelAction() {
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
    AnalysisFragmentImpl getFragmentImpl() {
        return mFragmentImpl;
    }
}
