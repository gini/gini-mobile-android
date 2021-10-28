package net.gini.android.capture.requirements;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import net.gini.android.capture.internal.camera.api.SizeSelectionHelper;
import net.gini.android.capture.internal.util.Size;

import java.util.List;

/**
 * Internal use only.
 *
 * @exclude
 */
public class CameraResolutionRequirement implements Requirement {

    // We require ~8MP or higher picture resolutions
    public static final int MIN_PICTURE_AREA = 7_900_000;
    // We allow up to 13MP picture resolutions
    public static final int MAX_PICTURE_AREA = 13_000_000;
    // We require an aspect ratio of at least 4:3
    public static final float MIN_ASPECT_RATIO = 1.33f;

    private final CameraHolder mCameraHolder;

    CameraResolutionRequirement(final CameraHolder cameraHolder) {
        mCameraHolder = cameraHolder;
    }

    @NonNull
    @Override
    public RequirementId getId() {
        return RequirementId.CAMERA_RESOLUTION;
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
                        MAX_PICTURE_AREA,
                        MIN_PICTURE_AREA,
                        MIN_ASPECT_RATIO
                );
                if (sizes == null) {
                    result = false;
                    details = "Camera doesn't have a resolution that matches the requirements";
                    return new RequirementReport(getId(), result, details);
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

}
