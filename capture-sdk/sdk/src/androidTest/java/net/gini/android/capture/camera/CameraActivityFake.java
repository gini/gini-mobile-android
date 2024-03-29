package net.gini.android.capture.camera;

import net.gini.android.capture.internal.camera.api.CameraControllerFake;

/**
 * Created by Alpar Szotyori on 15.12.2017.
 * <p>
 * Copyright (c) 2017 Gini GmbH.
 */

public class CameraActivityFake extends CameraActivity {

    private CameraFragmentFake mCameraFragmentCompatFake;

    @Override
    protected CameraFragment createCameraFragment() {
        return mCameraFragmentCompatFake = CameraFragmentFake.createInstance();
    }

    public CameraControllerFake getCameraControllerFake() {
        return mCameraFragmentCompatFake.getCameraControllerFake();
    }

    public CameraFragmentImplFake getCameraFragmentImplFake() {
        return mCameraFragmentCompatFake.getCameraFragmentImplFake();
    }
}
