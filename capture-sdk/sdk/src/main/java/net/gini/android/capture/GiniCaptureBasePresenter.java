package net.gini.android.capture;

import android.app.Activity;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 08.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public abstract class GiniCaptureBasePresenter<V extends GiniCaptureBaseView> {

    private final Activity mActivity;
    private final V mView;

    protected GiniCaptureBasePresenter(@NonNull final Activity activity, @NonNull final V view) {
        mActivity = activity;
        mView = view;
    }

    @NonNull
    public Activity getActivity() {
        return mActivity;
    }

    @NonNull
    protected V getView() {
        return mView;
    }

    public abstract void start();

    public abstract void stop();
}
