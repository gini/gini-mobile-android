package net.gini.android.capture.onboarding;

import android.app.Activity;

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
        if (mPage.getIconAdapter() == null) {
            return;
        }
        mPage.getIconAdapter().onVisible();
    }

    @Override
    void onPageIsHidden() {
        if (mPage.getIconAdapter() == null) {
            return;
        }
        mPage.getIconAdapter().onHidden();
    }

    @Override
    public void start() {
        showImage();
        showText();
    }

    @Override
    public void stop() {

    }

    private void showImage() {
        if (mPage.getIconAdapter() == null) {
            return;
        }
        getView().showImage(mPage.getIconAdapter());
    }

    private void showText() {
        if (mPage.getTitleResId() != 0) {
            getView().showTitle(mPage.getTitleResId());
        }
        if (mPage.getMessageResId() != 0) {
            getView().showMessage(mPage.getMessageResId());
        }
    }
}
