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

    interface View extends GiniCaptureBaseView<Presenter> {

        @Override
        void setPresenter(@NonNull final Presenter presenter);

        void showImage(@NonNull final OnboardingIconProvider iconProvider);

        void showTitle(@StringRes final int titleResId);
        void showMessage(@StringRes final int messageResId);

        void onPause();
        void onResume();
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
