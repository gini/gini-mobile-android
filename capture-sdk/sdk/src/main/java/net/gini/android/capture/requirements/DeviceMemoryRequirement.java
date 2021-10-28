package net.gini.android.capture.requirements;

import net.gini.android.capture.internal.camera.api.SizeSelectionHelper;
import net.gini.android.capture.internal.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

import java.util.List;

class DeviceMemoryRequirement implements Requirement {

    private final CameraHolder mCameraHolder;

    DeviceMemoryRequirement(final CameraHolder cameraHolder) {
        mCameraHolder = cameraHolder;
    }

    @NonNull
    @Override
    public RequirementId getId() {
        return RequirementId.DEVICE_MEMORY;
    }

    @NonNull
    @Override
    public RequirementReport check() {
        boolean result = true;
        String details = "";

        try {
            final List<Size> supportedPictureSizes = mCameraHolder.getSupportedPictureSizes();
            final List<Size> supportedPreviewSizes = mCameraHolder.getSupportedPreviewSizes();
            if (supportedPictureSizes != null && supportedPreviewSizes != null) {
                final Pair<Size, Size> sizes = SizeSelectionHelper.getBestSize(
                        supportedPictureSizes,
                        supportedPreviewSizes,
                        CameraResolutionRequirement.MAX_PICTURE_AREA,
                        CameraResolutionRequirement.MIN_PICTURE_AREA,
                        CameraResolutionRequirement.MIN_ASPECT_RATIO);
                if (sizes == null) {
                    result = false;
                    details =
                            "Cannot determine memory requirement as the camera has no picture resolution with a 4:3 aspect ratio";
                } else if (!sufficientMemoryAvailable(sizes.first)) {
                    result = false;
                    details = "Insufficient memory available";
                }
            } else {
                result = false;
                details = "Camera not open";
            }
        } catch (final RuntimeException e) {
            result = false;
            details = "Camera exception: " + e.getMessage();
        }

        return new RequirementReport(getId(), result, details);
    }

    /**
     * Given a photo size, return whether there is (currently) enough memory available.
     *
     * @param photoSize the size of photos that will be used for image processing
     *
     * @return whether there is enough memory for the image processing to succeed
     */
    @VisibleForTesting
    boolean sufficientMemoryAvailable(final Size photoSize) {
        final Runtime runtime = Runtime.getRuntime();
        return sufficientMemoryAvailable(runtime.totalMemory(), runtime.freeMemory(),
                runtime.maxMemory(), photoSize);
    }

    @VisibleForTesting
    boolean sufficientMemoryAvailable(final long totalMemoryBytes, final long freeMemoryBytes,
                                      final long maxMemoryBytes, final Size photoSize) {
        final float memoryUsedBytes = (totalMemoryBytes - freeMemoryBytes);
        final float memoryNeededBytes = calculateMemoryUsageForSize(photoSize);
        return memoryNeededBytes + memoryUsedBytes < maxMemoryBytes;
    }

    private float calculateMemoryUsageForSize(final Size photoSize) {
        // We have three channels of one byte each and we hold about three pictures in memory.
        return photoSize.width * photoSize.height * 3 * 3;
    }
}
