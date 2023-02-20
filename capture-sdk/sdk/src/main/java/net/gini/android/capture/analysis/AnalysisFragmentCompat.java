package net.gini.android.capture.analysis;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.gini.android.capture.Document;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.review.ReviewFragmentListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

/**
 * Internal use only.
 */
public class AnalysisFragmentCompat extends Fragment implements FragmentImplCallback,
        AnalysisFragmentInterface {

    private AnalysisFragmentImpl mFragmentImpl;
    private AnalysisFragmentListener mListener;

    @Override
    public void hideError() {
        mFragmentImpl.hideError();
    }

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
        final AnalysisFragmentImpl fragmentImpl = AnalysisFragmentHelper.createFragmentImpl(this,
                getArguments());
        AnalysisFragmentHelper.setListener(fragmentImpl, getActivity(), mListener);
        return fragmentImpl;
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
    public void onStart() {
        super.onStart();
        mFragmentImpl.onStart();
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
    public void showError(@NonNull final String message, @NonNull final String buttonTitle,
            @NonNull final View.OnClickListener onClickListener) {
        mFragmentImpl.showError(message, buttonTitle, onClickListener);
    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        mFragmentImpl.showError(message, duration);
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        if (mFragmentImpl != null) {
            mFragmentImpl.setListener(listener);
        }
        mListener = listener;
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
    public static AnalysisFragmentCompat createInstance(@NonNull final Document document,
            @Nullable final String documentAnalysisErrorMessage) {
        final AnalysisFragmentCompat fragment = new AnalysisFragmentCompat();
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

    @VisibleForTesting
    AnalysisFragmentImpl getFragmentImpl() {
        return mFragmentImpl;
    }
}
