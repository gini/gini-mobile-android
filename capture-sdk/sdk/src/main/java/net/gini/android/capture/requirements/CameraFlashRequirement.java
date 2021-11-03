package net.gini.android.capture.requirements;

import androidx.annotation.NonNull;

import kotlin.Pair;

class CameraFlashRequirement implements Requirement {

    private final CameraHolder mCameraHolder;

    CameraFlashRequirement(final CameraHolder cameraHolder) {
        mCameraHolder = cameraHolder;
    }

    @NonNull
    @Override
    public RequirementId getId() {
        return RequirementId.CAMERA_FLASH;
    }

    @NonNull
    @Override
    public RequirementReport check() {
        final Pair<Boolean, String> result = mCameraHolder.hasFlash();
        return new RequirementReport(getId(), result.getFirst(), result.getSecond());
    }
}
