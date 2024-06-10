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
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsExtraProperties;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsMappersKt;
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<UserAnalyticsExtraProperties, Object> eventProperties = new HashMap<>();

        if (event == UserAnalyticsEvent.SCREEN_SHOWN) {
            eventProperties.put(UserAnalyticsExtraProperties.ONBOARDING_HAS_CUSTOM_ITEMS, UserAnalyticsMappersKt.mapToAnalyticsValue(hasCustomItems));
        }
        if (hasCustomItems) {
            eventProperties.put(UserAnalyticsExtraProperties.CUSTOM_ONBOARDING_TITLE, String.valueOf(mPages.get(pageIndex).getTitleResId()));
            UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(
                    event, new UserAnalyticsScreen.OnboardingCustom(pageIndex), eventProperties
            );
        } else {
            UserAnalytics.INSTANCE.getAnalyticsEventTracker().trackEvent(
                    event,
                    getOnBoardingEventScreenName(mPages.get(pageIndex).getTitleResId()),
                    eventProperties
            );
        }
    }

    private UserAnalyticsScreen getOnBoardingEventScreenName(@StringRes int titleResId) {
        if (titleResId == R.string.gc_onboarding_qr_code_title) {
            return UserAnalyticsScreen.OnboardingQrCode.INSTANCE;
        }
        if (titleResId == R.string.gc_onboarding_multipage_title) {
            return UserAnalyticsScreen.OnboardingMultiplePages.INSTANCE;
        }
        if (titleResId == R.string.gc_onboarding_lighting_title) {
            return UserAnalyticsScreen.OnboardingLighting.INSTANCE;
        }
        return UserAnalyticsScreen.OnboardingFlatPaper.INSTANCE;
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
