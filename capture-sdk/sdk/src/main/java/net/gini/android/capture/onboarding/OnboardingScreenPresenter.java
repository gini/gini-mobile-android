package net.gini.android.capture.onboarding;

import static net.gini.android.capture.tracking.EventTrackingHelper.trackOnboardingScreenEvent;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.internal.util.FeatureConfiguration;
import net.gini.android.capture.tracking.OnboardingScreenEvent;
import net.gini.android.capture.tracking.useranalytics.UserAnalytics;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent;
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 * <p>
 * Copyright (c) 2019 Gini GmbH.
 */
class OnboardingScreenPresenter extends OnboardingScreenContract.Presenter {

    private List<OnboardingPage> mPages;
    private int mCurrentPageIndex;
    // used only for user analytics tracking
    private InteractionType mInteractionType = InteractionType.SWIPED;

    private enum InteractionType {
        SWIPED,
        TAPPED
    }


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
            addUserAnalyticsEvent(mCurrentPageIndex, UserAnalyticsEvent.GET_STARTED_TAPPED);
            getView().close();
            trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH);
        } else {
            scrollToNextPage();
        }
    }


    @Override
    public void skip() {
        addUserAnalyticsEvent(mCurrentPageIndex, UserAnalyticsEvent.SKIP_TAPPED);
        getView().close();
        trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH);
    }

    private boolean isOnLastPage() {
        return mCurrentPageIndex == mPages.size() - 1;
    }

    private void scrollToNextPage() {
        addUserAnalyticsEvent(mCurrentPageIndex, UserAnalyticsEvent.NEXT_STEP_TAPPED);
        mInteractionType = InteractionType.TAPPED;
        final int nextPageIndex = mCurrentPageIndex + 1;
        if (nextPageIndex < mPages.size()) {
            getView().scrollToPage(nextPageIndex);
        }
    }

    @Override
    void onScrolledToPage(final int pageIndex) {
        if (mInteractionType == InteractionType.SWIPED) {
            addUserAnalyticsEvent(mCurrentPageIndex, UserAnalyticsEvent.PAGE_SWIPED);
        }
        addUserAnalyticsEvent(pageIndex, UserAnalyticsEvent.SCREEN_SHOWN);
        mCurrentPageIndex = pageIndex;
        getView().activatePageIndicatorForPage(mCurrentPageIndex);
        updateButtons();
        mInteractionType = InteractionType.SWIPED;
    }

    private void addUserAnalyticsEvent(int pageIndex, UserAnalyticsEvent event) {
        List<OnboardingPage> customPages = null;
        if (GiniCapture.hasInstance()) {
            customPages = GiniCapture.getInstance().getCustomOnboardingPages();
        }

        boolean hasCustomItems = customPages != null && !customPages.isEmpty();
        Set<UserAnalyticsEventProperty> eventProperties = new HashSet<>();

        if (event == UserAnalyticsEvent.SCREEN_SHOWN) {
            eventProperties.add(new UserAnalyticsEventProperty.OnboardingHasCustomItems(hasCustomItems));
        }
        if (hasCustomItems) {
            eventProperties.add(
                    new UserAnalyticsEventProperty.CustomOnboardingTitle(String.valueOf(mPages.get(pageIndex).getTitleResId()))
            );
            eventProperties.add(new UserAnalyticsEventProperty.Screen(new UserAnalyticsScreen.OnBoarding.Custom(pageIndex)));
            UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(
                    event,
                    eventProperties
            );
        } else {
            eventProperties.add(new UserAnalyticsEventProperty.Screen(getOnBoardingEventScreenName(mPages.get(pageIndex).getTitleResId())));
            UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(event, eventProperties);
        }
    }

    private UserAnalyticsScreen.OnBoarding getOnBoardingEventScreenName(@StringRes int titleResId) {
        if (titleResId == R.string.gc_onboarding_qr_code_title) {
            return UserAnalyticsScreen.OnBoarding.QrCode.INSTANCE;
        }
        if (titleResId == R.string.gc_onboarding_multipage_title) {
            return UserAnalyticsScreen.OnBoarding.MultiplePages.INSTANCE;
        }
        if (titleResId == R.string.gc_onboarding_lighting_title) {
            return UserAnalyticsScreen.OnBoarding.Lighting.INSTANCE;
        }
        return UserAnalyticsScreen.OnBoarding.FlatPaper.INSTANCE;
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
        addUserAnalyticsEvent(mCurrentPageIndex, UserAnalyticsEvent.SCREEN_SHOWN);
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
