package net.gini.android.capture.onboarding;

import android.app.Activity;

import net.gini.android.capture.GiniCaptureBasePresenter;
import net.gini.android.capture.GiniCaptureBaseView;
import net.gini.android.capture.onboarding.view.OnboardingIconProvider;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
interface OnboardingPageContract {

    abstract class View implements GiniCaptureBaseView<Presenter> {

        private Presenter mPresenter;

        @Override
        public void setPresenter(@NonNull final Presenter presenter) {
            mPresenter = presenter;
        }

        public Presenter getPresenter() {
            return mPresenter;
        }

        abstract void showImage(@NonNull final OnboardingIconProvider iconProvider, final boolean rotated);

        abstract void showText(@StringRes final int textResId);

        abstract void showTransparentBackground();

        abstract void onPause();
        abstract void onResume();
    }

    abstract class Presenter extends GiniCaptureBasePresenter<View> {

        Presenter(
                @NonNull final Activity activity,
                @NonNull final View view) {
            super(activity, view);
        }

        abstract void setPage(@NonNull final OnboardingPage page);

        abstract void onPageIsVisible();
        abstract void onPageIsHidden();
    }

}
