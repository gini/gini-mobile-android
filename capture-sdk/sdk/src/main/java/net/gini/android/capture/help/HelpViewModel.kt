package net.gini.android.capture.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

/**
 * Internal use only.
 *
 * ViewModel for the help screen. Assembles the help menu items based on the
 * [GiniCapture] configuration, decides where item clicks should navigate to and
 * tracks the related analytics events.
 *
 * @suppress
 */
internal class HelpViewModel : ViewModel() {

    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.Help

    private val _uiState = MutableStateFlow(HelpUiState(helpItems = assembleHelpItems()))
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<HelpSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<HelpSideEffect> = _sideEffects.receiveAsFlow()

    private fun assembleHelpItems(): List<HelpItem> = mutableListOf<HelpItem>().apply {
        add(HelpItem.PhotoTips)

        if (GiniCapture.hasInstance()
            && GiniCapture.getInstance().isSupportedFormatsHelpScreenEnabled
        ) {
            add(HelpItem.SupportedFormats)
        }

        if (FeatureConfiguration.isFileImportEnabled()) {
            add(HelpItem.FileImport)
        }
        if (GiniCapture.hasInstance()) {
            addAll(GiniCapture.getInstance().customHelpItems)
        }
    }

    fun onScreenShown(resolvedItemTitles: List<String>) {
        trackHelpOpenEvent(resolvedItemTitles)
    }

    fun onStarted() {
        val helpItems = _uiState.value.helpItems
        if (helpItems.size == 1) {
            sendSideEffect(HelpSideEffect.NavigateToHelpItem(helpItems[0]))
        }
    }

    fun onHelpItemClicked(helpItem: HelpItem, resolvedTitle: String) {
        trackHelpItemTappedEvent(resolvedTitle)
        sendSideEffect(HelpSideEffect.NavigateToHelpItem(helpItem))
    }

    fun onBackClicked() {
        trackBackTappedEvent()
        sendSideEffect(HelpSideEffect.NavigateBackToCamera)
    }

    private fun sendSideEffect(sideEffect: HelpSideEffect) {
        viewModelScope.launch { _sideEffects.send(sideEffect) }
    }

    // region Analytics

    private fun trackHelpOpenEvent(resolvedItemTitles: List<String>) = runCatching {
        val hasCustomItems = GiniCapture.hasInstance() &&
                GiniCapture.getInstance().customHelpItems.isNotEmpty()

        val helpItems = resolvedItemTitles.map { "\"$it\"" }

        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN, setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.HasCustomItems(hasCustomItems),
                UserAnalyticsEventProperty.HelpItems(helpItems),
            )
        )
    }

    private fun trackBackTappedEvent() = runCatching {
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.CLOSE_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
    }

    private fun trackHelpItemTappedEvent(resolvedTitle: String) = runCatching {
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.HELP_ITEM_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.ItemTapped(resolvedTitle)
            )
        )
    }

    // endregion
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class HelpUiState(
    val helpItems: List<HelpItem>,
)

/**
 * Internal use only.
 *
 * @suppress
 */
internal sealed interface HelpSideEffect {
    data class NavigateToHelpItem(val helpItem: HelpItem) : HelpSideEffect
    data object NavigateBackToCamera : HelpSideEffect
}
