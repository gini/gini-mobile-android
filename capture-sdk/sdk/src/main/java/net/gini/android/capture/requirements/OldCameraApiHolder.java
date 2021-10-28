package net.gini.android.capture.requirements;

import android.hardware.Camera;

import net.gini.android.capture.internal.util.Size;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import kotlin.Pair;

class OldCameraApiHolder implements CameraHolder {

    private Camera mCamera;

    @Override
    public void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null; // NOPMD
        }
    }

    @Override
    public boolean hasCamera() throws RuntimeException {
        openCamera();
        return mCamera != null;
    }

    private void openCamera() throws RuntimeException {
        if (mCamera == null) {
            mCamera = Camera.open();
        }
    }

    @NotNull
    @Override
    public Pair<Boolean, String> hasAutoFocus() {
        if (mCamera == null) {
            return new Pair<>(false, "Camera not open");
        }

        try {
            final Camera.Parameters parameters = mCamera.getParameters();

            final List<String> supportedFocusModes = parameters.getSupportedFocusModes();

            if (supportedFocusModes == null || !supportedFocusModes.contains(
                    Camera.Parameters.FOCUS_MODE_AUTO)) {
                return new Pair<>(false, "Camera does not support auto-focus");
            }
        } catch (final RuntimeException e) {
            return new Pair<>(false, "Camera exception: " + e.getMessage());
        }

        return new Pair<>(true, "");
    }

    @NotNull
    @Override
    public Pair<Boolean, String> hasFlash() {
        if (mCamera == null) {
            return new Pair<>(false, "Camera not open");
        }

        try {
            final Camera.Parameters parameters = mCamera.getParameters();

            final List<String> supportedFlashModes = parameters.getSupportedFlashModes();

            if (supportedFlashModes == null || !supportedFlashModes.contains(
                    Camera.Parameters.FLASH_MODE_ON)) {
                return new Pair<>(false, "Camera does not support flash");
            }
        } catch (final RuntimeException e) {
            return new Pair<>(false, "Camera exception: " + e.getMessage());
        }

        return new Pair<>(true, "");
    }

    @Nullable
    @Override
    public List<Size> getSupportedPictureSizes() {
        if (mCamera == null) {
            return null;
        }

        return mCamera.getParameters().getSupportedPictureSizes()
                .stream()
                .map(size -> new Size(size.width, size.height))
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public List<Size> getSupportedPreviewSizes() {
        if (mCamera == null) {
            return null;
        }

        return mCamera.getParameters().getSupportedPreviewSizes()
                .stream()
                .map(size -> new Size(size.width, size.height))
                .collect(Collectors.toList());
    }
}
