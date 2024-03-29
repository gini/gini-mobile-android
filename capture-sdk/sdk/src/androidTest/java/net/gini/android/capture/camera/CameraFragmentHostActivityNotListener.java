package net.gini.android.capture.camera;

import net.gini.android.capture.test.FragmentHostActivity;


/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class CameraFragmentHostActivityNotListener extends
        FragmentHostActivity<CameraFragmentFake> {

    static CameraFragmentListener sListener;

    @Override
    protected void setListener() {
        if (sListener != null) {
            getFragment().setListener(sListener);
        }
    }

    @Override
    protected CameraFragmentFake createFragment() {
        return CameraFragmentFake.createInstance();
    }

}
