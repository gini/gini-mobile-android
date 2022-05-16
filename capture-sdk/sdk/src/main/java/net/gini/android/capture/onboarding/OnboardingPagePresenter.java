package net.gini.android.capture.onboarding;

import android.app.Activity;

import net.gini.android.capture.internal.util.ContextHelper;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
class OnboardingPagePresenter extends OnboardingPageContract.Presenter {

    private OnboardingPage mPage;

    OnboardingPagePresenter(
            @NonNull final Activity activity,
            @NonNull final OnboardingPageContract.View view) {
        super(activity, view);
        view.setPresenter(this);
    }

    @Override
    void setPage(@NonNull final OnboardingPage page) {
        mPage = page;
    }

    @Override
    void onPageIsVisible() {
        if (mPage.getIconProvider() == null) {
            return;
        }
        mPage.getIconProvider().onVisible();
    }

    @Override
    void onPageIsHidden() {
        if (mPage.getIconProvider() == null) {
            return;
        }
        mPage.getIconProvider().onHidden();
    }

    @Override
    public void start() {
        showImage();
        showText();
        if (mPage.isTransparent()) {
            getView().showTransparentBackground();
        }
    }

    @Override
    public void stop() {

    }

    private void showImage() {
        if (mPage.getIconProvider() == null) {
            return;
        }
        final boolean rotated = !ContextHelper.isPortraitOrientation(getActivity())
                && mPage.shouldRotateImageForLandscape();
        getView().showImage(mPage.getIconProvider(), rotated);
    }

    private void showText() {
        if (mPage.getTextResId() == 0) {
            return;
        }
        getView().showText(mPage.getTextResId());
    }
}
