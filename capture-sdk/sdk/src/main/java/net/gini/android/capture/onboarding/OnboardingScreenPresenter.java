package net.gini.android.capture.onboarding;

import static net.gini.android.capture.tracking.EventTrackingHelper.trackOnboardingScreenEvent;

import android.app.Activity;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.internal.util.FeatureConfiguration;
import net.gini.android.capture.tracking.OnboardingScreenEvent;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
class OnboardingScreenPresenter extends OnboardingScreenContract.Presenter {

    private List<OnboardingPage> mPages;
    private int mCurrentPageIndex;

    OnboardingScreenPresenter(@NonNull final Activity activity,
            @NonNull final OnboardingScreenContract.View view) {
        super(activity, view);
        view.setPresenter(this);
        mPages = getDefaultPages();
    }

    private List<OnboardingPage> getDefaultPages() {
        return DefaultPages.asArrayList(FeatureConfiguration.isMultiPageEnabled(), FeatureConfiguration.isQRCodeScanningEnabled());
    }

    @Override
    void setCustomPages(@NonNull final List<OnboardingPage> pages) {
        mPages = pages;
    }

    @Override
    public void showNextPage() {
        if (isOnLastPage()) {
            getView().close();
            trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH);
        } else {
            scrollToNextPage();
        }
    }

    @Override
    public void skip() {
        getView().close();
        trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH);
    }

    private boolean isOnLastPage() {
        return mCurrentPageIndex == mPages.size() - 1;
    }

    private void scrollToNextPage() {
        final int nextPageIndex = mCurrentPageIndex + 1;
        if (nextPageIndex < mPages.size()) {
            getView().scrollToPage(nextPageIndex);
        }
    }

    @Override
    void onScrolledToPage(final int pageIndex) {
        mCurrentPageIndex = pageIndex;
        getView().activatePageIndicatorForPage(mCurrentPageIndex);
        updateButtons();
    }

    private void updateButtons() {
        if (isOnLastPage()) {
            if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
                getView().showGetStartedButtonInNavigationBarBottom();
            } else {
                getView().showGetStartedButton();
            }
        } else {
            if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
                getView().showSkipAndNextButtonsInNavigationBarBottom();
            } else {
                getView().showSkipAndNextButtons();
            }
        }
    }

    @Override
    public void start() {
        getView().showPages(mPages);

        mCurrentPageIndex = 0;
        getView().scrollToPage(mCurrentPageIndex);
        getView().activatePageIndicatorForPage(mCurrentPageIndex);

        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            setupNavigationBarBottom();
        }

        updateButtons();

        trackOnboardingScreenEvent(OnboardingScreenEvent.START);
    }

    private void setupNavigationBarBottom() {
        if (GiniCapture.hasInstance()) {
            getView().setNavigationBarBottomAdapterInstance(
                    GiniCapture.getInstance().internal().getOnboardingNavigationBarBottomAdapterInstance());
            getView().hideButtons();
        }
    }

    @Override
    public void stop() {

    }

    @VisibleForTesting
    List<OnboardingPage> getPages() {
        return mPages;
    }
}
