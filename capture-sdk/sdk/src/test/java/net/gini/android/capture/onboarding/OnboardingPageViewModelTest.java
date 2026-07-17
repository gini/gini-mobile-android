package net.gini.android.capture.onboarding;

import static com.google.common.truth.Truth.assertThat;

import net.gini.android.capture.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OnboardingPageViewModelTest {

    @Test
    public void should_showImage_onStart() throws Exception {
        // Given
        final OnboardingPage page = new DefaultPages.Page1().getOnboardingPage();

        final OnboardingPageViewModel viewModel = createViewModel(page);

        // When
        viewModel.start();

        // Then
        assertThat(viewModel.getIllustrationAdapter().getValue())
                .isEqualTo(page.getIllustrationAdapter());
    }

    @NonNull
    private OnboardingPageViewModel createViewModel(@NonNull final OnboardingPage page) throws Exception {
        final OnboardingPageViewModel viewModel = new OnboardingPageViewModel();
        viewModel.setPage(page);

        return viewModel;
    }

    @Test
    public void should_notShowImage_whenNotAvailable() throws Exception {
        // Given
        final OnboardingPage page = new OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null);

        final OnboardingPageViewModel viewModel = createViewModel(page);

        // When
        viewModel.start();

        // Then
        assertThat(viewModel.getIllustrationAdapter().getValue()).isNull();
    }

    @Test
    public void should_showTitle_onStart() throws Exception {
        // Given
        final OnboardingPage page = new DefaultPages.Page1().getOnboardingPage();

        final OnboardingPageViewModel viewModel = createViewModel(page);

        // When
        viewModel.start();

        // Then
        assertThat(viewModel.getTitleResId().getValue()).isEqualTo(page.getTitleResId());
    }

    @Test
    public void should_showMessage_onStart() throws Exception {
        // Given
        final OnboardingPage page = new DefaultPages.Page1().getOnboardingPage();

        final OnboardingPageViewModel viewModel = createViewModel(page);

        // When
        viewModel.start();

        // Then
        assertThat(viewModel.getMessageResId().getValue()).isEqualTo(page.getMessageResId());
    }
}
