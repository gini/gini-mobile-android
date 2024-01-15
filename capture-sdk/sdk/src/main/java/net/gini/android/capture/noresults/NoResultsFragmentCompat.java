package net.gini.android.capture.noresults;


import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.ImageRetakeOptionsListener;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import static net.gini.android.capture.GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE;

/**
 * Internal use only.
 */
public class NoResultsFragmentCompat extends Fragment implements FragmentImplCallback {

    private NoResultsFragmentImpl mFragmentImpl;
    private NoResultsFragmentListener errorListener;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentImpl = NoResultsFragmentHelper.createFragmentImpl(this, getArguments());
        NoResultsFragmentHelper.setListener(mFragmentImpl, getActivity());
        if (getActivity() instanceof NoResultsFragmentListener) {
            errorListener = (NoResultsFragmentListener) getActivity();
        }
        mFragmentImpl.onCreate(savedInstanceState);

        checkGiniCaptureInstance();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
            final Bundle savedInstanceState) {
        return mFragmentImpl.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * <p>
     * Factory method for creating a new instance of the Fragment.
     * </p>
     *
     * @param document a {@link Document} for which no valid extractions were received
     *
     * @return a new instance of the Fragment
     */
    public static NoResultsFragmentCompat createInstance(@NonNull final Document document) {
        final NoResultsFragmentCompat fragment = new NoResultsFragmentCompat();
        fragment.setArguments(NoResultsFragmentHelper.createArguments(document));
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

    private void checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            errorListener.onError(new GiniCaptureError(MISSING_GINI_CAPTURE_INSTANCE,
                    "Missing GiniCapture instance. It was not created or there was an application process restart."));
        }
    }
}
