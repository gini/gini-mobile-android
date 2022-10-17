package net.gini.android.capture.camera;

import static org.mockito.Mockito.spy;

import net.gini.android.capture.internal.camera.api.CameraControllerFake;

/**
 * Created by Alpar Szotyori on 15.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

public class CameraFragmentFake extends CameraFragment {

    private CameraFragmentImplFake mCameraFragmentImplFake;

    public static CameraFragmentFake createInstance() {
        return new CameraFragmentFake();
    }

    @Override
    protected CameraFragmentImpl createFragmentImpl() {
        mCameraFragmentImplFake = spy(new CameraFragmentImplFake(this));
        return mCameraFragmentImplFake;
    }

    public CameraControllerFake getCameraControllerFake() {
        return mCameraFragmentImplFake.getCameraControllerFake();
    }

    public CameraFragmentImplFake getCameraFragmentImplFake() {
        return mCameraFragmentImplFake;
    }
}
