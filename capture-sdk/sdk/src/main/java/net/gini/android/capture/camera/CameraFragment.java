package net.gini.android.capture.camera;

import static net.gini.android.capture.internal.util.FragmentExtensionsKt.getLayoutInflaterWithGiniCaptureTheme;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import net.gini.android.capture.R;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;
import net.gini.android.capture.internal.util.FragmentExtensionsKt;

/**
 * Internal use only.
 *
 * @suppress
 */
public class CameraFragment extends Fragment implements CameraFragmentInterface,
        FragmentImplCallback {

    public static final String REQUEST_KEY = "GC_CAMERA_FRAGMENT_REQUEST_KEY";
    public static final String RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE = "RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE";
    private static final String ARGS_ADD_PAGES = "GC_ARGS_ADD_PAGES";

    private CameraFragmentListener mListener;

    private CameraFragmentImpl mFragmentImpl;
    private boolean addPages = false;

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readArguments();
        mFragmentImpl = createFragmentImpl();
        setListener(mFragmentImpl, mListener);
        mFragmentImpl.onCreate(savedInstanceState);
    }

    private void readArguments() {
        final Bundle arguments = getArguments();
        if (arguments != null) {
            addPages = arguments.getBoolean(ARGS_ADD_PAGES, false);
        }
    }

    private void setListener(@NonNull final CameraFragmentImpl fragmentImpl, @Nullable final CameraFragmentListener listener) {
        if (listener != null) {
            fragmentImpl.setListener(listener);
        } else {
            throw new IllegalStateException(
                    "CameraFragmentListener not set. "
                            + "You can set it with CameraFragmentCompat#setListener() or "
                            + "by making the host activity implement the CameraFragmentListener.");
        }
    }

    protected CameraFragmentImpl createFragmentImpl() {
        return new CameraFragmentImpl(this, addPages);
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
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
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

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragmentImpl.onSaveInstanceState(outState);
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

    @Override
    public void setListener(@NonNull final CameraFragmentListener listener) {
        if (mFragmentImpl != null) {
            mFragmentImpl.setListener(listener);
        }
        mListener = listener;
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
}
