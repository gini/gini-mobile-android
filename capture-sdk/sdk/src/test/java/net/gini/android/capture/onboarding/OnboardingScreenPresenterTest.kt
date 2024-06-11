package net.gini.android.capture.onboarding

import android.app.Activity
import com.google.common.collect.Lists
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(JUnitParamsRunner::class)
class OnboardingScreenPresenterTest {


    private lateinit var mActivity: Activity


    private lateinit var mView: OnboardingScreenContract.View


    private lateinit var mUserAnalyticsEventTracker: UserAnalyticsEventTracker

    private val onboardingPageComparator =
        Correspondence.from<OnboardingPage, OnboardingPage>({ actual, expected ->
            actual?.titleResId == expected?.titleResId && actual?.messageResId == expected?.messageResId
        }, "is equivalent to")

    @Before
    fun setUp() {
        mUserAnalyticsEventTracker = mockk<UserAnalyticsEventTracker>().apply {
            every { trackEvent(any()) } just Runs
            every { trackEvent(any(), any()) } just Runs
        }
        mActivity = mockk()

        mockkObject(UserAnalytics)

        mView = mockk<OnboardingScreenContract.View>(relaxed = true)

        every { UserAnalytics.getAnalyticsEventTracker() } returns mUserAnalyticsEventTracker
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
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            ),
            OnboardingPage(
                R.string.gc_onboarding_lighting_title,
                R.string.gc_onboarding_lighting_message,
                null
            )
        )
        presenter.setCustomPages(customPages)
        presenter.showNextPage()

        // Then
        verify { mView.scrollToPage(1) }
    }

    @Test
    @Throws(Exception::class)
    fun `notify view to close onboarding when show next page on last page was requested`() {
        // Given
        val presenter = createPresenter()

        // When
        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            ),
            OnboardingPage(
                R.string.gc_onboarding_lighting_title,
                R.string.gc_onboarding_lighting_message,
                null
            )
        )
        presenter.setCustomPages(customPages)
        presenter.onScrolledToPage(1)
        presenter.showNextPage()

        // Then
        verify { mView.close() }
    }

    @Test
    @Throws(Exception::class)
    fun `update page indicator after scrolling to a page`() {
        // Given
        val presenter = createPresenter()

        // When
        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            ),
            OnboardingPage(
                R.string.gc_onboarding_lighting_title,
                R.string.gc_onboarding_lighting_message,
                null
            )
        )
        presenter.setCustomPages(customPages)
        presenter.onScrolledToPage(1)

        // Then
        verify { mView.activatePageIndicatorForPage(1) }
    }

    @Test
    @Throws(Exception::class)
    fun `show pages on start`() {
        // Given
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        verify { mView.showPages(presenter.pages) }
    }

    @Test
    @Throws(Exception::class)
    fun `scroll to first page on start`() {
        // Given
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        verify { mView.scrollToPage(0) }
    }

    @Test
    @Throws(Exception::class)
    fun `trigger finish event when clicking next on the last page`() {
        // Given
        val presenter = createPresenter()
        val eventTracker = spyk<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            ),
            OnboardingPage(
                R.string.gc_onboarding_lighting_title,
                R.string.gc_onboarding_lighting_message,
                null
            )
        )
        presenter.setCustomPages(customPages)
        presenter.onScrolledToPage(1)

        // When
        presenter.showNextPage()

        // Then
        verify { eventTracker.onOnboardingScreenEvent(Event(OnboardingScreenEvent.FINISH)) }
    }

    @Test
    @Throws(Exception::class)
    fun `trigger finish event when clicking the skip button`() {
        // Given
        val presenter = createPresenter()
        val eventTracker = spyk<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        presenter.skip()

        // Then
        verify { eventTracker.onOnboardingScreenEvent(Event(OnboardingScreenEvent.FINISH)) }
    }

    @Test
    @Throws(Exception::class)
    fun `trigger start event`() {
        // Given
        val presenter = createPresenter()
        val eventTracker = spyk<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        presenter.start()

        // Then
        verify() { eventTracker.onOnboardingScreenEvent(Event(OnboardingScreenEvent.START)) }
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
        verify { mView.close() }
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
        verify {
            mView.setNavigationBarBottomAdapterInstance(
                eq(
                    GiniCapture.getInstance()
                        .internal().onboardingNavigationBarBottomAdapterInstance
                )
            )
        }
    }

    @Test
    fun `use custom onboarding bottom navigation bar if enabled`() {
        // Given
        val customOnboardingNavigationBarBottomAdapter =
            mockk<OnboardingNavigationBarBottomAdapter>()
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(true)
            .setOnboardingNavigationBarBottomAdapter(customOnboardingNavigationBarBottomAdapter)
            .build()
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        verify {
            mView.setNavigationBarBottomAdapterInstance(
                eq(
                    GiniCapture.getInstance()
                        .internal().onboardingNavigationBarBottomAdapterInstance
                )
            )
        }
    }

    @Test
    @Parameters("false", "true")
    fun `show skip and next buttons when not on last page - (isBottomNavigationBarEnabled) `(
        isBottomNavigationBarEnabled: Boolean
    ) {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(isBottomNavigationBarEnabled)
            .build()
        val presenter = createPresenter()

        // When
        presenter.start()

        // Then
        if (isBottomNavigationBarEnabled) {
            verify(exactly = 0) { mView.showSkipAndNextButtons() }
            verify { mView.showSkipAndNextButtonsInNavigationBarBottom() }
        } else {
            verify { mView.showSkipAndNextButtons() }
            verify(exactly = 0) { mView.showSkipAndNextButtonsInNavigationBarBottom() }
        }
    }

    @Test
    @Parameters("false", "true")
    fun `show skip and next buttons after going back from the last page - (isBottomNavigationBarEnabled) `(
        isBottomNavigationBarEnabled: Boolean
    ) {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(isBottomNavigationBarEnabled)
            .build()
        val presenter = createPresenter()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            ),
            OnboardingPage(
                R.string.gc_onboarding_lighting_title,
                R.string.gc_onboarding_lighting_message,
                null
            )
        )
        presenter.setCustomPages(customPages)

        // When
        presenter.start()
        presenter.onScrolledToPage(1)
        presenter.onScrolledToPage(0)

        // Then
        if (isBottomNavigationBarEnabled) {
            verify(exactly = 0) { mView.showSkipAndNextButtons() }
            verify(exactly = 2) { mView.showSkipAndNextButtonsInNavigationBarBottom() }
        } else {
            verify(exactly = 2) { mView.showSkipAndNextButtons() }
            verify(exactly = 0) { mView.showSkipAndNextButtonsInNavigationBarBottom() }
        }
    }

    @Test
    @Parameters("false", "true")
    fun `show 'get started' button when on last page - (isBottomNavigationBarEnabled) `(
        isBottomNavigationBarEnabled: Boolean
    ) {
        // Given
        GiniCapture.Builder()
            .setBottomNavigationBarEnabled(isBottomNavigationBarEnabled)
            .build()
        val presenter = createPresenter()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            ),
            OnboardingPage(
                R.string.gc_onboarding_lighting_title,
                R.string.gc_onboarding_lighting_message,
                null
            )
        )
        presenter.setCustomPages(customPages)

        // When
        presenter.start()
        presenter.onScrolledToPage(1)

        // Then
        if (isBottomNavigationBarEnabled) {
            verify(exactly = 0) { mView.showGetStartedButton() }
            verify { mView.showGetStartedButtonInNavigationBarBottom() }
        } else {
            verify { mView.showGetStartedButton() }
            verify(exactly = 0) { mView.showGetStartedButtonInNavigationBarBottom() }
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
        verify { mView.hideButtons() }
    }

}