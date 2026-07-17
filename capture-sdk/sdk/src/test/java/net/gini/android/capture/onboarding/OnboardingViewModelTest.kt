package net.gini.android.capture.onboarding

import com.google.common.collect.Lists
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by Alpar Szotyori on 20.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OnboardingViewModelTest {


    private lateinit var mUserAnalyticsEventTracker: UserAnalyticsEventTracker

    private val onboardingPageComparator =
        Correspondence.from<OnboardingPage, OnboardingPage>({ actual, expected ->
            actual?.titleResId == expected?.titleResId && actual?.messageResId == expected?.messageResId
        }, "is equivalent to")

    @Before
    fun setUp() {
        mUserAnalyticsEventTracker = mockk<UserAnalyticsEventTracker>().apply {
            every { trackEvent(any()) } returns true
            every { trackEvent(any(), any()) } returns true
        }

        mockkObject(UserAnalytics)

        every { UserAnalytics.getAnalyticsEventTracker() } returns mUserAnalyticsEventTracker
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private fun createViewModel(): OnboardingViewModel {
        return OnboardingViewModel()
    }

    @Test
    @Throws(Exception::class)
    fun `show next page`() {
        // Given
        val viewModel = createViewModel()

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
        viewModel.setCustomPages(customPages)
        viewModel.showNextPage()

        // Then
        Truth.assertThat(viewModel.scrollToPage.value?.peekContent()).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun `notify view to close onboarding when show next page on last page was requested`() {
        // Given
        val viewModel = createViewModel()

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
        viewModel.setCustomPages(customPages)
        viewModel.onScrolledToPage(1)
        viewModel.showNextPage()

        // Then
        Truth.assertThat(viewModel.closeOnboarding.value).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun `update page indicator after scrolling to a page`() {
        // Given
        val viewModel = createViewModel()

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
        viewModel.setCustomPages(customPages)
        viewModel.onScrolledToPage(1)

        // Then
        Truth.assertThat(viewModel.activePageIndex.value).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun `show pages on start`() {
        // Given
        val viewModel = createViewModel()
        var shownPages: List<OnboardingPage>? = null
        viewModel.pages.observeForever { shownPages = it }

        // When
        viewModel.start()

        // Then
        Truth.assertThat(shownPages).isNotNull()
        Truth.assertThat(shownPages)
            .comparingElementsUsing(onboardingPageComparator)
            .containsExactlyElementsIn(DefaultPages.asArrayList(false, false))
            .inOrder()
    }

    @Test
    @Throws(Exception::class)
    fun `scroll to first page on start`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.start()

        // Then
        Truth.assertThat(viewModel.scrollToPage.value?.peekContent()).isEqualTo(0)
    }

    @Test
    @Throws(Exception::class)
    fun `trigger finish event when clicking next on the last page`() {
        // Given
        val viewModel = createViewModel()
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
        viewModel.setCustomPages(customPages)
        viewModel.onScrolledToPage(1)

        // When
        viewModel.showNextPage()

        // Then
        verify { eventTracker.onOnboardingScreenEvent(Event(OnboardingScreenEvent.FINISH)) }
    }

    @Test
    @Throws(Exception::class)
    fun `trigger finish event when clicking the skip button`() {
        // Given
        val viewModel = createViewModel()
        val eventTracker = spyk<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        viewModel.skip()

        // Then
        verify { eventTracker.onOnboardingScreenEvent(Event(OnboardingScreenEvent.FINISH)) }
    }

    @Test
    @Throws(Exception::class)
    fun `trigger start event`() {
        // Given
        val viewModel = createViewModel()
        val eventTracker = spyk<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        viewModel.start()

        // Then
        verify() { eventTracker.onOnboardingScreenEvent(Event(OnboardingScreenEvent.START)) }
    }

    @Test
    fun `shows default onboarding pages - multi-page disabled, QR code scanning disabled`() {
        verifyShowsDefaultOnboardingPages(isMultiPageEnabled = false, isQRCodeScanningEnabled = false)
    }

    @Test
    fun `shows default onboarding pages - multi-page enabled, QR code scanning disabled`() {
        verifyShowsDefaultOnboardingPages(isMultiPageEnabled = true, isQRCodeScanningEnabled = false)
    }

    @Test
    fun `shows default onboarding pages - multi-page disabled, QR code scanning enabled`() {
        verifyShowsDefaultOnboardingPages(isMultiPageEnabled = false, isQRCodeScanningEnabled = true)
    }

    @Test
    fun `shows default onboarding pages - multi-page enabled, QR code scanning enabled`() {
        verifyShowsDefaultOnboardingPages(isMultiPageEnabled = true, isQRCodeScanningEnabled = true)
    }

    private fun verifyShowsDefaultOnboardingPages(isMultiPageEnabled: Boolean, isQRCodeScanningEnabled: Boolean) {
        // Given
        GiniCapture.Builder()
            .setMultiPageEnabled(isMultiPageEnabled)
            .setQRCodeScanningEnabled(isQRCodeScanningEnabled)
            .build()
        val viewModel = createViewModel()

        // Then
        Truth.assertThat(viewModel.pages.value)
            .comparingElementsUsing(onboardingPageComparator)
            .containsExactlyElementsIn(DefaultPages.asArrayList(isMultiPageEnabled, isQRCodeScanningEnabled))
            .inOrder()
    }

    @Test
    @Throws(Exception::class)
    fun `shows custom onboarding pages`() {
        // Given
        val viewModel = createViewModel()

        val customPages: List<OnboardingPage> = Lists.newArrayList(
            OnboardingPage(R.string.gc_onboarding_align_corners_title, R.string.gc_onboarding_align_corners_message, null),
            OnboardingPage(R.string.gc_onboarding_lighting_title, R.string.gc_onboarding_lighting_message, null)
        )
        viewModel.setCustomPages(customPages)

        // Then
        Truth.assertThat(viewModel.pages.value)
            .comparingElementsUsing(onboardingPageComparator)
            .containsExactlyElementsIn(customPages)
            .inOrder()
    }

    @Test
    fun `skip button notifies the listener to close the onboarding`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.skip()

        // Then
        Truth.assertThat(viewModel.closeOnboarding.value).isNotNull()
    }

    @Test
    fun `show skip and next buttons when not on last page`() {
        // Given
        GiniCapture.Builder().build()
        val viewModel = createViewModel()

        // When
        viewModel.start()

        // Then

        Truth.assertThat(viewModel.buttonsState.value)
            .isEqualTo(OnboardingButtonsState.SKIP_AND_NEXT)

    }

    @Test
    fun `show skip and next buttons after going back from the last page`() {
        // Given
        GiniCapture.Builder()
            .build()
        val viewModel = createViewModel()

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
        viewModel.setCustomPages(customPages)

        val buttonsStates = mutableListOf<OnboardingButtonsState>()
        viewModel.buttonsState.observeForever { buttonsStates.add(it) }

        // When
        viewModel.start()
        viewModel.onScrolledToPage(1)
        viewModel.onScrolledToPage(0)

        // Then
        Truth.assertThat(buttonsStates.count { it == OnboardingButtonsState.SKIP_AND_NEXT })
            .isEqualTo(2)
        Truth.assertThat(buttonsStates.count { it == OnboardingButtonsState.SKIP_AND_NEXT_IN_NAVIGATION_BAR_BOTTOM })
            .isEqualTo(0)
    }

    @Test
    fun `show 'get started' button when on last page`() {
        // Given
        GiniCapture.Builder()
            .build()
        val viewModel = createViewModel()

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
        viewModel.setCustomPages(customPages)

        // When
        viewModel.start()
        viewModel.onScrolledToPage(1)

        // Then
        Truth.assertThat(viewModel.buttonsState.value)
            .isEqualTo(OnboardingButtonsState.GET_STARTED)
    }

}
