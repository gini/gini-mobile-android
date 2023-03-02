package net.gini.android.capture.onboarding;

import android.app.Activity;

import net.gini.android.capture.GiniCaptureBasePresenter;
import net.gini.android.capture.GiniCaptureBaseView;
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter;
import net.gini.android.capture.view.InjectedViewAdapterInstance;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
interface OnboardingScreenContract {

    interface View extends GiniCaptureBaseView<Presenter>, OnboardingFragmentInterface {

        @Override
        void setPresenter(@NonNull final Presenter presenter);

        void showPages(@NonNull final List<OnboardingPage> pages);

        void scrollToPage(final int pageIndex);

        void activatePageIndicatorForPage(final int pageIndex);

        void showGetStartedButton();
        void showGetStartedButtonInNavigationBarBottom();

        void showSkipAndNextButtons();
        void showSkipAndNextButtonsInNavigationBarBottom();

        void setNavigationBarBottomAdapterInstance(@NonNull final InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter> adapterInstance);

        void hideButtons();
    }

    abstract class Presenter extends GiniCaptureBasePresenter<View> implements
            OnboardingFragmentInterface {

        Presenter(
                @NonNull final Activity activity,
                @NonNull final View view) {
            super(activity, view);
        }

        abstract void setCustomPages(@NonNull final List<OnboardingPage> pages);

        abstract void onScrolledToPage(final int pageIndex);

        abstract void showNextPage();

        abstract void skip();
    }
}
