package net.gini.android.capture.onboarding;

import android.app.Activity;

import net.gini.android.capture.GiniCaptureBasePresenter;
import net.gini.android.capture.GiniCaptureBaseView;

import java.util.List;

import androidx.annotation.NonNull;
import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
interface OnboardingScreenContract {

    abstract class View implements GiniCaptureBaseView<Presenter>, OnboardingFragmentInterface {

        private Presenter mPresenter;

        @Override
        public void setPresenter(@NonNull final Presenter presenter) {
            mPresenter = presenter;
        }

        public Presenter getPresenter() {
            return mPresenter;
        }

        abstract void showPages(@NonNull final List<OnboardingPage> pages,
                final boolean showEmptyLastPage);

        abstract void scrollToPage(final int pageIndex);

        abstract void activatePageIndicatorForPage(final int pageIndex);

        abstract CompletableFuture<Void> slideOutViews();
    }

    abstract class Presenter extends GiniCaptureBasePresenter<View> implements
            OnboardingFragmentInterface {

        Presenter(
                @NonNull final Activity activity,
                @NonNull final View view) {
            super(activity, view);
        }

        abstract void setCustomPages(@NonNull final List<OnboardingPage> pages);

        abstract void addEmptyLastPage();

        abstract void showNextPage();

        abstract void onScrolledToPage(final int pageIndex);

    }
}
