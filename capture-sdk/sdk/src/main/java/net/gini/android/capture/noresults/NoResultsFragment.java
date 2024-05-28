package net.gini.android.capture.noresults;


import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.Document;
import net.gini.android.capture.EnterManuallyButtonListener;
import net.gini.android.capture.R;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.util.CancelListener;

/**
 * Internal use only.
 */
public class NoResultsFragment extends Fragment implements FragmentImplCallback {

    private NoResultsFragmentImpl mFragmentImpl;
    private final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";

    public void setListeners(@Nullable final EnterManuallyButtonListener listener) {
        mListener = listener;
    }

    public void setCancelListener(@NonNull final CancelListener cancelListener) {
        mCancelListener = cancelListener;
    }

    private EnterManuallyButtonListener mListener;
    private CancelListener mCancelListener;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentImpl = createFragmentImpl(this, getArguments());
        mFragmentImpl.setListener(mListener);
        mFragmentImpl.onCreate(savedInstanceState);

    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        final LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);
        return getLayoutInflaterWithGiniCaptureTheme(this, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             final Bundle savedInstanceState) {
        return mFragmentImpl.onCreateView(inflater, container, savedInstanceState);
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

    NoResultsFragmentImpl createFragmentImpl(@NonNull final FragmentImplCallback fragment,
                                             @NonNull final Bundle arguments) {
        final Document document = arguments.getParcelable(ARGS_DOCUMENT);
        if (document != null) {
            return new NoResultsFragmentImpl(fragment, document, mCancelListener);
        } else {
            throw new IllegalStateException(
                    "NoResultsFragmentCompat requires a Document. Use the createInstance() method of these classes for instantiating.");
        }
    }

    @NonNull
    @Override
    public NavController findNavController() {
        return NavHostFragment.findNavController(this);
    }


    public static void navigateToNoResultsFragment(
            NavController navController,
            NavDirections direction
    ) {
        if (navController.getCurrentDestination().getId() == R.id.gc_destination_noresults_fragment) {
            return;
        }
        navController.navigate(direction);
    }


}
