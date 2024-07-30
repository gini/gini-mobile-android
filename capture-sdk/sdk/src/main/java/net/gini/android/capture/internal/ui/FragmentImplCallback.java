package net.gini.android.capture.internal.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal use only.
 *
 * @suppress
 */
public interface FragmentImplCallback {

    @Nullable
    FragmentActivity getActivity();

    @Nullable
    View getView();

    @Nullable
    Fragment getParentFragment();

    void startActivity(Intent intent);

    void startActivityForResult(Intent intent, int requestCode);

    void showAlertDialog(@NonNull final String message,
            @NonNull final String positiveButtonTitle,
            @NonNull final DialogInterface.OnClickListener positiveButtonClickListener,
            @Nullable final String negativeButtonTitle,
            @Nullable final DialogInterface.OnClickListener negativeButtonClickListener,
            @Nullable final DialogInterface.OnCancelListener cancelListener);

    @NonNull
    FragmentManager getChildFragmentManager();

    @NonNull
    FragmentManager getParentFragmentManager();

    @MainThread
    @NonNull
    LifecycleOwner getViewLifecycleOwner();

    @NonNull
    NavController findNavController();

    @NonNull
    default void safeNavigate(@NonNull final NavDirections navDirections) {
        try {
            findNavController().navigate(navDirections);
        } catch (Exception exception) {
            Logger logger = LoggerFactory.getLogger(FragmentImplCallback.class);
            logger.error("Navigation exception " + exception.getMessage());
        }
    }
}
