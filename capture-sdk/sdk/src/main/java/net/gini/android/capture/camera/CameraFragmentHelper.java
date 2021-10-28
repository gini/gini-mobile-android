package net.gini.android.capture.camera;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.internal.ui.FragmentImplCallback;

class CameraFragmentHelper {

    @NonNull
    CameraFragmentImpl createFragmentImpl(@NonNull final FragmentImplCallback fragment) {
        return createCameraFragment(fragment);
    }

    @NonNull
    protected CameraFragmentImpl createCameraFragment(
            @NonNull final FragmentImplCallback fragment) {
        return new CameraFragmentImpl(fragment);
    }

    public static void setListener(@NonNull final CameraFragmentImpl fragmentImpl,
            @NonNull final Context context, @Nullable final CameraFragmentListener listener) {
        if (context instanceof CameraFragmentListener) {
            fragmentImpl.setListener((CameraFragmentListener) context);
        } else if (listener != null) {
            fragmentImpl.setListener(listener);
        } else {
            throw new IllegalStateException(
                    "CameraFragmentListener not set. "
                            + "You can set it with CameraFragmentCompat#setListener() or "
                            + "by making the host activity implement the CameraFragmentListener.");
        }
    }
}
