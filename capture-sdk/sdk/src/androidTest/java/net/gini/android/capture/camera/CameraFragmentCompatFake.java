package net.gini.android.capture.camera;

import net.gini.android.capture.internal.camera.api.CameraControllerFake;

/**
 * Created by Alpar Szotyori on 15.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

public class CameraFragmentCompatFake extends CameraFragmentCompat {

    private CameraFragmentImplFake mCameraFragmentImplFake;

    public static CameraFragmentCompatFake createInstance() {
        return new CameraFragmentCompatFake();
    }

    @Override
    protected CameraFragmentImpl createFragmentImpl() {
        final CameraFragmentHelperFake cameraFragmentHelperFake = new CameraFragmentHelperFake();
        final CameraFragmentImpl cameraFragmentImpl = cameraFragmentHelperFake.createFragmentImpl(
                this);
        mCameraFragmentImplFake = cameraFragmentHelperFake.getCameraFragmentImplFake();
        return cameraFragmentImpl;
    }

    public CameraControllerFake getCameraControllerFake() {
        return mCameraFragmentImplFake.getCameraControllerFake();
    }

    public CameraFragmentImplFake getCameraFragmentImplFake() {
        return mCameraFragmentImplFake;
    }
}
