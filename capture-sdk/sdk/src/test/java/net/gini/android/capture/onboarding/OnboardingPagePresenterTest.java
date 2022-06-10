package net.gini.android.capture.onboarding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Activity;
import android.content.res.Resources;

import net.gini.android.capture.R;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
public class OnboardingPagePresenterTest {

    @Mock
    private Activity mActivity;
    @Mock
    private OnboardingPageContract.View mView;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void should_showImage_onStart() throws Exception {
        // Given
        final OnboardingPage page = new DefaultPages.Page1().getOnboardingPage();

        final OnboardingPagePresenter presenter = createPresenter(page);

        // When
        presenter.start();

        // Then
        verify(mView).showImage(page.getIllustrationAdapter());
    }

    @NonNull
    private OnboardingPagePresenter createPresenter(@NonNull final OnboardingPage page) throws Exception {
        return createPresenter(page, true);
    }

    @NonNull
    private OnboardingPagePresenter createPresenter(@NonNull final OnboardingPage page,
            final Boolean isPortrait) throws Exception {
        final Resources resources = mock(Resources.class);
        when(resources.getBoolean(R.bool.gc_is_portrait)).thenReturn(isPortrait);
        when(mActivity.getResources()).thenReturn(resources);

        final OnboardingPagePresenter presenter = new OnboardingPagePresenter(mActivity, mView);
        presenter.setPage(page);

        return presenter;
    }

    @Test
    public void should_notShowImage_whenNotAvailable() throws Exception {
        // Given
        final OnboardingPage page = new OnboardingPage(R.string.gc_onboarding_page_1_title, R.string.gc_onboarding_page_1_message, null);

        final OnboardingPagePresenter presenter = createPresenter(page);

        // When
        presenter.start();

        // Then
        verify(mView, never()).showImage(page.getIllustrationAdapter());
    }

    @Test
    public void should_showTitle_onStart() throws Exception {
        // Given
        final OnboardingPage page = new DefaultPages.Page1().getOnboardingPage();

        final OnboardingPagePresenter presenter = createPresenter(page);

        // When
        presenter.start();

        // Then
        verify(mView).showTitle(page.getTitleResId());
    }

    @Test
    public void should_showMessage_onStart() throws Exception {
        // Given
        final OnboardingPage page = new DefaultPages.Page1().getOnboardingPage();

        final OnboardingPagePresenter presenter = createPresenter(page);

        // When
        presenter.start();

        // Then
        verify(mView).showMessage(page.getMessageResId());
    }
}