package net.gini.android.capture.camera;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.AlertDialogHelperCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * <h3>Component API</h3>
 *
 * <p>
 * {@code CameraFragmentCompat} is the main entry point to the Gini Capture SDK when using the
 * Component API with the Android Support Library.
 * </p>
 * <p>
 * It shows a camera preview with tap-to-focus functionality, a trigger button and an optional
 * flash on/off button. The camera preview also shows document corner guides to which the user
 * should align the document.
 * </p>
 * <p>
 * If instantiated with {@link CameraFragmentCompat#createInstance(GiniCaptureFeatureConfiguration)}
 * then a button for importing documents is shown next to the trigger button. A hint popup is
 * displayed the first time the Gini Capture SDK is used to inform the user about document
 * importing.
 * </p>
 * <p>
 * For importing documents {@code READ_EXTERNAL_STORAGE} permission is required and if the
 * permission is not granted the Gini Capture SDK will prompt the user to grant the permission.
 * See @{code Customizing the Camera Screen} on how to override the message and button titles for
 * the rationale and on permission denial alerts.
 * </p>
 * <p>
 * <b>Note:</b> Your Activity hosting this Fragment must extend the {@link
 * androidx.appcompat.app.AppCompatActivity} and use an AppCompat Theme.
 * </p>
 * <p>
 * Include the {@code CameraFragmentCompat} into your layout either directly with {@code <fragment>}
 * in your Activity's layout or using the {@link androidx.fragment.app.FragmentManager} and one of
 * the {@code createInstance()} methods.
 * </p>
 * <p>
 * A {@link CameraFragmentListener} instance must be available until the {@code
 * CameraFragmentCompat} is attached to an activity. Failing to do so will throw an exception. The
 * listener instance can be provided either implicitly by making the hosting Activity implement the
 * {@link CameraFragmentListener} interface or explicitly by setting the listener using {@link
 * CameraFragmentCompat#setListener(CameraFragmentListener)}.
 * </p>
 * <p>
 * Your Activity is automatically set as the listener in {@link CameraFragmentCompat#onAttach(Context)}.
 * </p>
 *
 * <h3>Customizing the Camera Screen</h3>
 *
 * <p>
 * See the {@link CameraActivity} for details.
 * </p>
 */
public class CameraFragmentCompat extends Fragment implements CameraFragmentInterface,
        FragmentImplCallback {

    private CameraFragmentListener mListener;

    public static CameraFragmentCompat createInstance() {
        return new CameraFragmentCompat();
    }

    private CameraFragmentImpl mFragmentImpl;

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentImpl = createFragmentImpl();
        CameraFragmentHelper.setListener(mFragmentImpl, getActivity(), mListener);
        mFragmentImpl.onCreate(savedInstanceState);
    }

    protected CameraFragmentImpl createFragmentImpl() {
        return new CameraFragmentHelper().createFragmentImpl(this);
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
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final boolean handled = mFragmentImpl.onActivityResult(requestCode, resultCode, data);
        if (!handled) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void setListener(@NonNull final CameraFragmentListener listener) {
        if (mFragmentImpl != null) {
            mFragmentImpl.setListener(listener);
        }
        mListener = listener;
    }

    @Override
    public void showInterface() {
        if (mFragmentImpl == null) {
            return;
        }
        mFragmentImpl.showInterface();
    }

    @Override
    public void hideInterface() {
        if (mFragmentImpl == null) {
            return;
        }
        mFragmentImpl.hideInterface();
    }

    @Override
    public void showActivityIndicatorAndDisableInteraction() {
        mFragmentImpl.showActivityIndicatorAndDisableInteraction();
    }

    @Override
    public void hideActivityIndicatorAndEnableInteraction() {
        mFragmentImpl.hideActivityIndicatorAndEnableInteraction();
    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        mFragmentImpl.showError(message, duration);
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
}
