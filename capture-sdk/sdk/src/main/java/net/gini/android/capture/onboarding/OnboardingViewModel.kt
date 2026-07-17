package net.gini.android.capture.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter
import net.gini.android.capture.tracking.EventTrackingHelper.trackOnboardingScreenEvent
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.view.InjectedViewAdapterInstance

/**
 * Internal use only.
 *
 * The buttons which should be visible on the onboarding screen.
 *
 * @suppress
 */
internal enum class OnboardingButtonsState {
    SKIP_AND_NEXT,
    SKIP_AND_NEXT_IN_NAVIGATION_BAR_BOTTOM,
    GET_STARTED,
    GET_STARTED_IN_NAVIGATION_BAR_BOTTOM
}

/**
 * Internal use only.
 *
 * ViewModel for the onboarding screen. Holds the onboarding pages and drives page changes,
 * button visibility, the injected bottom navigation bar and analytics tracking.
 *
 * @suppress
 */
internal class OnboardingViewModel : ViewModel() {

    private val mutablePages = MutableLiveData<List<OnboardingPage>>(getDefaultPages())

    /**
     * The onboarding pages to be shown.
     */
    val pages: LiveData<List<OnboardingPage>> = mutablePages

    private val mutableScrollToPage = MutableLiveData<ConsumableEvent<Int>>()

    /**
     * One-shot event requesting to scroll to the page at the given index.
     */
    val scrollToPage: LiveData<ConsumableEvent<Int>> = mutableScrollToPage

    private val mutableActivePageIndex = MutableLiveData<Int>()

    /**
     * The index of the page for which the page indicator should be activated.
     */
    val activePageIndex: LiveData<Int> = mutableActivePageIndex

    private val mutableNavigationBarBottomAdapterInstance =
        MutableLiveData<InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter>>()

    /**
     * The injected view adapter instance for the bottom navigation bar.
     */
    val navigationBarBottomAdapterInstance: LiveData<InjectedViewAdapterInstance<OnboardingNavigationBarBottomAdapter>> =
        mutableNavigationBarBottomAdapterInstance

    private val mutableButtonsState = MutableLiveData<OnboardingButtonsState>()

    /**
     * The buttons which should be visible.
     */
    val buttonsState: LiveData<OnboardingButtonsState> = mutableButtonsState

    private val mutableCloseOnboarding = MutableLiveData<ConsumableEvent<Unit>>()

    /**
     * One-shot event requesting to close the onboarding screen.
     */
    val closeOnboarding: LiveData<ConsumableEvent<Unit>> = mutableCloseOnboarding

    private var currentPageIndex = 0

    // used only for user analytics tracking
    private var interactionType = InteractionType.SWIPED

    private enum class InteractionType {
        SWIPED,
        TAPPED
    }

    private val currentPages: List<OnboardingPage>
        get() = mutablePages.value ?: emptyList()

    private fun getDefaultPages(): List<OnboardingPage> =
        DefaultPages.asArrayList(
            FeatureConfiguration.isMultiPageEnabled(),
            FeatureConfiguration.isQRCodeScanningEnabled()
        )

    fun setCustomPages(pages: List<OnboardingPage>) {
        mutablePages.value = pages
    }

    fun start() {
        currentPageIndex = 0
        mutableScrollToPage.value = ConsumableEvent(currentPageIndex)
        mutableActivePageIndex.value = currentPageIndex

        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            setupNavigationBarBottom()
        }

        updateButtons()

        trackOnboardingScreenEvent(OnboardingScreenEvent.START)
        addUserAnalyticsEvent(currentPageIndex, UserAnalyticsEvent.SCREEN_SHOWN)
    }

    fun showNextPage() {
        if (isOnLastPage()) {
            addUserAnalyticsEvent(currentPageIndex, UserAnalyticsEvent.GET_STARTED_TAPPED)
            mutableCloseOnboarding.value = ConsumableEvent(Unit)
            trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH)
        } else {
            scrollToNextPage()
        }
    }

    fun skip() {
        addUserAnalyticsEvent(currentPageIndex, UserAnalyticsEvent.SKIP_TAPPED)
        mutableCloseOnboarding.value = ConsumableEvent(Unit)
        trackOnboardingScreenEvent(OnboardingScreenEvent.FINISH)
    }

    fun onScrolledToPage(pageIndex: Int) {
        if (interactionType == InteractionType.SWIPED) {
            addUserAnalyticsEvent(currentPageIndex, UserAnalyticsEvent.PAGE_SWIPED)
        }
        addUserAnalyticsEvent(pageIndex, UserAnalyticsEvent.SCREEN_SHOWN)
        currentPageIndex = pageIndex
        mutableActivePageIndex.value = currentPageIndex
        updateButtons()
        interactionType = InteractionType.SWIPED
    }

    private fun isOnLastPage(): Boolean = currentPageIndex == currentPages.size - 1

    private fun scrollToNextPage() {
        addUserAnalyticsEvent(currentPageIndex, UserAnalyticsEvent.NEXT_STEP_TAPPED)
        interactionType = InteractionType.TAPPED
        val nextPageIndex = currentPageIndex + 1
        if (nextPageIndex < currentPages.size) {
            mutableScrollToPage.value = ConsumableEvent(nextPageIndex)
        }
    }

    private fun addUserAnalyticsEvent(pageIndex: Int, event: UserAnalyticsEvent) {
        var customPages: List<OnboardingPage>? = null
        if (GiniCapture.hasInstance()) {
            customPages = GiniCapture.getInstance().customOnboardingPages
        }

        val hasCustomItems = !customPages.isNullOrEmpty()
        val eventProperties = HashSet<UserAnalyticsEventProperty>()

        if (event == UserAnalyticsEvent.SCREEN_SHOWN) {
            eventProperties.add(UserAnalyticsEventProperty.OnboardingHasCustomItems(hasCustomItems))
        }
        if (hasCustomItems) {
            eventProperties.add(
                UserAnalyticsEventProperty.CustomOnboardingTitle(currentPages[pageIndex].titleResId.toString())
            )
            eventProperties.add(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.OnBoarding.Custom(pageIndex))
            )
        } else {
            eventProperties.add(
                UserAnalyticsEventProperty.Screen(getOnBoardingEventScreenName(currentPages[pageIndex].titleResId))
            )
        }
        UserAnalytics.getAnalyticsEventTracker()?.trackEvent(event, eventProperties)
    }

    private fun getOnBoardingEventScreenName(@StringRes titleResId: Int): UserAnalyticsScreen =
        when (titleResId) {
            R.string.gc_onboarding_qr_code_title -> UserAnalyticsScreen.OnBoarding.QrCode
            R.string.gc_onboarding_multipage_title -> UserAnalyticsScreen.OnBoarding.MultiplePages
            R.string.gc_onboarding_lighting_title -> UserAnalyticsScreen.OnBoarding.Lighting
            else -> UserAnalyticsScreen.OnBoarding.FlatPaper
        }

    private fun updateButtons() {
        val isBottomNavigationBarEnabled =
            GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled
        mutableButtonsState.value = if (isOnLastPage()) {
            if (isBottomNavigationBarEnabled) {
                OnboardingButtonsState.GET_STARTED_IN_NAVIGATION_BAR_BOTTOM
            } else {
                OnboardingButtonsState.GET_STARTED
            }
        } else {
            if (isBottomNavigationBarEnabled) {
                OnboardingButtonsState.SKIP_AND_NEXT_IN_NAVIGATION_BAR_BOTTOM
            } else {
                OnboardingButtonsState.SKIP_AND_NEXT
            }
        }
    }

    private fun setupNavigationBarBottom() {
        if (GiniCapture.hasInstance()) {
            mutableNavigationBarBottomAdapterInstance.value =
                GiniCapture.getInstance().internal().onboardingNavigationBarBottomAdapterInstance
        }
    }
}
