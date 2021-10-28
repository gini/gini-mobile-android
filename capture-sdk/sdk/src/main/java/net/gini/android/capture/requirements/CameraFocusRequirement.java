package net.gini.android.capture.requirements;

import androidx.annotation.NonNull;

import kotlin.Pair;

class CameraFocusRequirement implements Requirement {

    private final CameraHolder mCameraHolder;

    CameraFocusRequirement(final CameraHolder cameraHolder) {
        mCameraHolder = cameraHolder;
    }

    @NonNull
    @Override
    public RequirementId getId() {
        return RequirementId.CAMERA_FOCUS;
    }

    @NonNull
    @Override
    public RequirementReport check() {
        final Pair<Boolean, String> result = mCameraHolder.hasAutoFocus();
        return new RequirementReport(getId(), result.getFirst(), result.getSecond());
    }
}
