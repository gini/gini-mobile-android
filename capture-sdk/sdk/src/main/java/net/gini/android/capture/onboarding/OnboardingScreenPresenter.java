package net.gini.android.capture.onboarding;

import static net.gini.android.capture.internal.util.ContextHelper.isTablet;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackOnboardingScreenEvent;

import android.app.Activity;

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

    private static final OnboardingFragmentListener NO_OP_LISTENER =
            new OnboardingFragmentListener() {
                @Override
                public void onCloseOnboarding() {
                }

                @Override
                public void onError(@NonNull final GiniCaptureError error) {
                }
            };

    private OnboardingFragmentListener mListener = NO_OP_LISTENER;
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
            mListener.onCloseOnboarding();
            trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH);
        } else {
            scrollToNextPage();
        }
    }

    @Override
    public void skip() {
        mListener.onCloseOnboarding();
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
            getView().showGetStartedButton();
        } else {
            getView().showSkipAndNextButtons();
        }
    }

    @Override
    public void start() {
        getView().showPages(mPages);
        mCurrentPageIndex = 0;
        getView().scrollToPage(mCurrentPageIndex);
        getView().activatePageIndicatorForPage(mCurrentPageIndex);
        trackOnboardingScreenEvent(OnboardingScreenEvent.START);
    }

    @Override
    public void stop() {

    }

    @Override
    public void setListener(@NonNull final OnboardingFragmentListener listener) {
        mListener = listener;
    }

    @VisibleForTesting
    List<OnboardingPage> getPages() {
        return mPages;
    }
}
