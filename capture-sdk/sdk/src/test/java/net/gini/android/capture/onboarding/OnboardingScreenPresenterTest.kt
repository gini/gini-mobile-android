package net.gini.android.capture.onboarding

import android.app.Activity
import com.google.common.collect.Lists
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(JUnitParamsRunner::class)
class OnboardingScreenPresenterTest {

    @Mock
    private lateinit var mActivity: Activity

    @Mock
    private lateinit var mView: OnboardingScreenContract.View

    private val onboardingPageComparator = Correspondence.from<OnboardingPage, OnboardingPage>({ actual, expected ->
        actual?.titleResId == expected?.titleResId && actual?.messageResId == expected?.messageResId
    }, "is equivalent to")

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private fun createPresenter(): OnboardingScreenPresenter {
        return OnboardingScreenPresenter(mActivity, mView)
    }

    @Test
    @Throws(Exception::class)
    fun `show next page`() {
        // Given
        val presenter = createPresenter()

        // When
        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)
        presenter.showNextPage()

        // Then
        Mockito.verify(mView).scrollToPage(1)
    }

    @Test
    @Throws(Exception::class)
    fun `notify view to close onboarding when show next page on last page was requested`() {
        // Given
        val presenter = createPresenter()

        // When
        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)
        presenter.onScrolledToPage(1)
        presenter.showNextPage()

        // Then
        Mockito.verify(mView).close()
    }

    @Test
    @Throws(Exception::class)
    fun `update page indicator after scrolling to a page`() {
        // Given
        val presenter = createPresenter()

        // When
        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)
        presenter.onScrolledToPage(1)

        // Then
        Mockito.verify(mView).activatePageIndicatorForPage(1)
    }

    @Test
    @Throws(Exception::class)
    fun `show pages on start`() {
        // Given
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        Mockito.verify(mView).showPages(presenter.pages)
    }

    @Test
    @Throws(Exception::class)
    fun `scroll to first page on start`() {
        // Given
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        Mockito.verify(mView).scrollToPage(0)
    }

    @Test
    @Throws(Exception::class)
    fun `trigger finish event when clicking next on the last page`() {
        // Given
        val presenter = createPresenter()
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)
        presenter.onScrolledToPage(1)

        // When
        presenter.showNextPage()

        // Then
        Mockito.verify(eventTracker).onOnboardingScreenEvent(Event(OnboardingScreenEvent.FINISH))
    }

    @Test
    @Throws(Exception::class)
    fun `trigger finish event when clicking the skip button`() {
        // Given
        val presenter = createPresenter()
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        presenter.skip()

        // Then
        Mockito.verify(eventTracker).onOnboardingScreenEvent(Event(OnboardingScreenEvent.FINISH))
    }

    @Test
    @Throws(Exception::class)
    fun `trigger start event`() {
        // Given
        val presenter = createPresenter()
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        presenter.start()

        // Then
        Mockito.verify(eventTracker).onOnboardingScreenEvent(Event(OnboardingScreenEvent.START))
    }

    @Test
    @Parameters(
        "false, false",
        "true, false",
        "false, true",
        "true, true"
    )
    fun `shows default onboarding pages - (isMultiPageEnabled, isQRCodeScanningEnabled) `(isMultiPageEnabled: Boolean, isQRCodeScanningEnabled: Boolean) {
        // Given
        GiniCapture.Builder()
            .setMultiPageEnabled(isMultiPageEnabled)
            .setQRCodeScanningEnabled(isQRCodeScanningEnabled)
            .build()
        val presenter = createPresenter()

        // Then
        Truth.assertThat(presenter.pages)
            .comparingElementsUsing(onboardingPageComparator)
            .containsExactlyElementsIn(DefaultPages.asArrayList(isMultiPageEnabled, isQRCodeScanningEnabled))
            .inOrder()
    }

    @Test
    @Throws(Exception::class)
    fun `shows custom onboarding pages`() {
        // Given
        val presenter = createPresenter()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)

        // Then
        Truth.assertThat(presenter.pages)
            .comparingElementsUsing(onboardingPageComparator)
            .containsExactlyElementsIn(customPages)
            .inOrder()
    }

    @Test
    fun `skip button notifies the listener to close the onboarding`() {
        // Given
        val presenter = createPresenter()

        // When
        presenter.skip()

        // Then
        verify(mView).close()
    }

    @Test
    fun `use default onboarding bottom navigation bar if enabled`() {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(true)
            .build()
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        verify(mView).setNavigationBarBottomAdapterInstance(eq(GiniCapture.getInstance().internal().onboardingNavigationBarBottomAdapterInstance))
    }

    @Test
    fun `use custom onboarding bottom navigation bar if enabled`() {
        // Given
        val customOnboardingNavigationBarBottomAdapter = mock<OnboardingNavigationBarBottomAdapter>()
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(true)
            .setOnboardingNavigationBarBottomAdapter(customOnboardingNavigationBarBottomAdapter)
            .build()
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        verify(mView).setNavigationBarBottomAdapterInstance(eq(GiniCapture.getInstance().internal().onboardingNavigationBarBottomAdapterInstance))
    }

    @Test
    @Parameters("false", "true")
    fun `show skip and next buttons when not on last page - (isBottomNavigationBarEnabled) `(isBottomNavigationBarEnabled: Boolean) {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(isBottomNavigationBarEnabled)
            .build()
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        if (isBottomNavigationBarEnabled) {
            verify(mView, never()).showSkipAndNextButtons()
            verify(mView).showSkipAndNextButtonsInNavigationBarBottom()
        } else {
            verify(mView).showSkipAndNextButtons()
            verify(mView, never()).showSkipAndNextButtonsInNavigationBarBottom()
        }
    }

    @Test
    @Parameters("false", "true")
    fun `show skip and next buttons after going back from the last page - (isBottomNavigationBarEnabled) `(isBottomNavigationBarEnabled: Boolean) {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(isBottomNavigationBarEnabled)
            .build()
        val presenter = createPresenter()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)

        // When
        presenter.start()
        presenter.onScrolledToPage(1)
        presenter.onScrolledToPage(0)

        // Then
        if (isBottomNavigationBarEnabled) {
            verify(mView, never()).showSkipAndNextButtons()
            verify(mView, times(2)).showSkipAndNextButtonsInNavigationBarBottom()
        } else {
            verify(mView, times(2)).showSkipAndNextButtons()
            verify(mView, never()).showSkipAndNextButtonsInNavigationBarBottom()
        }
    }

    @Test
    @Parameters("false", "true")
    fun `show 'get started' button when on last page - (isBottomNavigationBarEnabled) `(isBottomNavigationBarEnabled: Boolean) {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(isBottomNavigationBarEnabled)
            .build()
        val presenter = createPresenter()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        presenter.setCustomPages(customPages)

        // When
        presenter.start()
        presenter.onScrolledToPage(1)

        // Then
        if (isBottomNavigationBarEnabled) {
            verify(mView, never()).showGetStartedButton()
            verify(mView).showGetStartedButtonInNavigationBarBottom()
        } else {
            verify(mView).showGetStartedButton()
            verify(mView, never()).showGetStartedButtonInNavigationBarBottom()
        }
    }

    @Test
    fun `hides buttons when bottom navigation bar is enabled`() {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(true)
            .build()
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        verify(mView).hideButtons()
    }

}