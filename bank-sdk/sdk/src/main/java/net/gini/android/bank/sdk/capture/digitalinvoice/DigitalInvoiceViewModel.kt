package net.gini.android.bank.sdk.capture.digitalinvoice

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.skonto.factory.text.SkontoInfoBannerTextFactory
import net.gini.android.bank.sdk.capture.skonto.mapper.toAnalyticsModel
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.capture.util.BusEvent
import net.gini.android.bank.sdk.capture.util.OncePerInstallEvent
import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.bank.sdk.capture.util.SimpleBusEventStore
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import java.math.BigDecimal

/**
 * Created by Alpar Szotyori on 05.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

private const val KEY_SELECTABLE_ITEMS = "SELECTABLE_ITEMS"
private const val KEY_SKONTO_STATE = "SKONTO_STATE"
private const val KEY_SKONTO_DATA = "SKONTO_DATA"

/**
 * Internal use only.
 *
 * @suppress
 */
@Suppress("TooManyFunctions", "LongParameterList")
internal class DigitalInvoiceViewModel(
    val extractions: Map<String, GiniCaptureSpecificExtraction> = emptyMap(),
    val compoundExtractions: Map<String, GiniCaptureCompoundExtraction> = emptyMap(),
    val returnReasons: List<GiniCaptureReturnReason> = emptyList(),
    private var skontoData: SkontoData? = null,
    private val isInaccurateExtraction: Boolean = false,
    savedInstanceBundle: Bundle? = null,
    private val oncePerInstallEventStore: OncePerInstallEventStore,
    private val simpleBusEventStore: SimpleBusEventStore,
    getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
    getSkontoAmountUseCase: GetSkontoAmountUseCase,
    getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase,
    private val skontoInfoBannerTextFactory: SkontoInfoBannerTextFactory,
) : ViewModel() {

    private var onboardingDisplayed: Boolean = savedInstanceBundle != null

    private var footerDetails = FooterDetails(inaccurateExtraction = isInaccurateExtraction)

    private fun shouldDisplayOnboarding(): Boolean = !onboardingDisplayed &&
            !oncePerInstallEventStore
                .containsEvent(OncePerInstallEvent.SHOW_DIGITAL_INVOICE_ONBOARDING)

    private val digitalInvoice: DigitalInvoice
    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.ReturnAssistant

    private var skontoEdgeCase: SkontoEdgeCase? = null

    private val _uiState = MutableStateFlow(
        DigitalInvoiceUiState(isInaccurateExtraction = isInaccurateExtraction)
    )
    val uiState: StateFlow<DigitalInvoiceUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<DigitalInvoiceSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<DigitalInvoiceSideEffect> = _sideEffects.receiveAsFlow()

    init {
        skontoData = savedInstanceBundle?.let {
            BundleCompat.getParcelable(it, KEY_SKONTO_DATA, SkontoData::class.java)
        } ?: skontoData
        digitalInvoice = DigitalInvoice(
            extractions = extractions,
            compoundExtractions = compoundExtractions,
            savedSelectableItems = savedInstanceBundle?.let {
                // Try the new ArrayList format first (written by putParcelableArrayList).
                // Fall back to the legacy Array format (written by putParcelableArray in older
                // versions) so that state saved before this format change is not silently lost.
                BundleCompat.getParcelableArrayList(it, KEY_SELECTABLE_ITEMS, SelectableLineItem::class.java)
                    ?: @Suppress("DEPRECATION") it.getParcelableArray(KEY_SELECTABLE_ITEMS)
                        ?.filterIsInstance<SelectableLineItem>()
            },
            skontoData = skontoData,
            getSkontoAmountUseCase = getSkontoAmountUseCase,
            getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
            getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase
        )
        savedInstanceBundle?.let {
            digitalInvoice.updateSkontoEnabled(it.getBoolean(KEY_SKONTO_STATE, false))
        }
        simpleBusEventStore.observeChange(BusEvent.DISMISS_ONBOARDING_FRAGMENT)
            .onEach {
                if (it) {
                    updateView()
                }
            }
            .launchIn(viewModelScope)
    }

    fun saveState(outState: Bundle) {
        outState.putParcelableArrayList(
            KEY_SELECTABLE_ITEMS,
            ArrayList(digitalInvoice.selectableLineItems)
        )

        outState.putParcelable(
            KEY_SKONTO_DATA,
            digitalInvoice.skontoData
        )

        outState.putBoolean(
            KEY_SKONTO_STATE,
            digitalInvoice.skontoEnabled
        )
    }

    fun selectLineItem(lineItem: SelectableLineItem) {
        digitalInvoice.selectLineItem(lineItem)
        updateView()
    }

    fun editSkontoDataListItem(skontoListItem: DigitalInvoiceSkontoListItem) {
        digitalInvoice.skontoData?.let { data ->
            sendSideEffect(
                DigitalInvoiceSideEffect.ShowSkontoEditScreen(
                    data = data,
                    isSkontoSectionActive = skontoListItem.enabled
                )
            )
        }
    }

    fun enableSkonto() {
        digitalInvoice.updateSkontoEnabled(true)
        updateView()
    }

    fun disableSkonto() {
        digitalInvoice.updateSkontoEnabled(false)
        updateView()
    }

    fun updateSkontoData(skontoData: SkontoData?) {
        digitalInvoice.updateSkontoData(skontoData)
        updateView()
    }

    fun deselectLineItem(lineItem: SelectableLineItem) {
        if (canShowReturnReasonsDialog()) {
            sendSideEffect(
                DigitalInvoiceSideEffect.ShowReturnReasonDialog(returnReasons, lineItem)
            )
        } else {
            digitalInvoice.deselectLineItem(lineItem, null)
            updateView()
        }
    }

    fun onReturnReasonSelected(
        lineItem: SelectableLineItem,
        selectedReason: GiniCaptureReturnReason?
    ) {
        if (selectedReason != null) {
            digitalInvoice.deselectLineItem(lineItem, selectedReason)
        } else {
            digitalInvoice.selectLineItem(lineItem)
        }
        updateView()
    }

    internal fun deselectLineItem(index: Int) {
        digitalInvoice.selectableLineItems.getOrNull(index)?.let { selectableLineItem ->
            deselectLineItem(selectableLineItem)
        }
    }

    internal fun deselectAllLineItems() {
        digitalInvoice.selectableLineItems.forEach { deselectLineItem(it) }
    }

    private fun canShowReturnReasonsDialog() =
        GiniBank.enableReturnReasons && returnReasons.isNotEmpty()

    fun editLineItem(lineItem: SelectableLineItem) {
        sendSideEffect(DigitalInvoiceSideEffect.EditLineItem(lineItem))
    }

    fun pay() {
        skipOrPay()
    }

    private fun skipOrPay() {
        trackProceedTapped()
        digitalInvoice.updateLineItemExtractionsWithReviewedLineItems()
        digitalInvoice.updateAmountToPayExtractionWithTotalPrice()
        digitalInvoice.updateSkontoExtractions()

        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().setUpdatedCompoundExtractions(
                digitalInvoice.compoundExtractions
            )
        }
        if (digitalInvoice.skontoData != null && digitalInvoice.skontoEnabled) {
            sendTransferSummary()
        }
        sendSideEffect(
            DigitalInvoiceSideEffect.PayInvoice(
                digitalInvoice.extractions,
                digitalInvoice.compoundExtractions
            )
        )
    }


    private fun sendTransferSummary() {
        val amount =
            Amount(digitalInvoice.extractions["amountToPay"]?.let { parsePriceString(it.value).first }
                ?: BigDecimal.ZERO, AmountCurrency.EUR)

        var skontoPercentageDiscountedCalculated: String? = null
        var skontoAmountToPayCalculated: String? = null
        var skontoDueDateCalculated: String? = null

        compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.map { skontoDiscountData ->
            skontoPercentageDiscountedCalculated = skontoDiscountData.getDataByKeys(
                "skontoPercentageDiscountedCalculated",
            ) ?: throw NoSuchElementException("Data for `PercentageDiscounted` is missing")

            skontoAmountToPayCalculated = skontoDiscountData.getDataByKeys(
                "skontoAmountToPayCalculated"
            ) ?: ""

            skontoDueDateCalculated = skontoDiscountData.getDataByKeys(
                "skontoDueDateCalculated"
            ) ?: ""

        }
        GiniBank.sendTransferSummaryForSkonto(
            amount,
            skontoAmountToPayCalculated ?: "",
            skontoPercentageDiscountedCalculated ?: "",
            skontoDueDateCalculated ?: ""
        )

    }

    private fun MutableMap<String, GiniCaptureSpecificExtraction>.getDataByKeys(
        vararg keys: String
    ) = keys.firstNotNullOfOrNull { this[it]?.value }


    fun updateLineItem(selectableLineItem: SelectableLineItem) {
        digitalInvoice.updateLineItem(selectableLineItem)
        updateView()
    }

    fun start() {
        updateView()
        if (shouldDisplayOnboarding()) {
            simpleBusEventStore.saveEvent(BusEvent.DISMISS_ONBOARDING_FRAGMENT, false)
            onboardingDisplayed = true
            sendSideEffect(DigitalInvoiceSideEffect.ShowOnboarding)
        } else {
            trackScreenShownEvent()
        }
    }

    @VisibleForTesting
    internal fun updateView() {
        val skontoSavedAmount = digitalInvoice.getSkontoSavedAmount()
        skontoEdgeCase = digitalInvoice.skontoData?.let { skontoData ->
            getSkontoEdgeCaseUseCase.execute(
                skontoData.skontoDueDate,
                skontoData.skontoPaymentMethod
            )
        }
        val skontoListItem = digitalInvoice.skontoData?.let { skontoData ->
            DigitalInvoiceSkontoListItem(
                isEdgeCase = skontoEdgeCase != null,
                savedAmount = skontoSavedAmount!!,
                message = skontoInfoBannerTextFactory.create(
                    edgeCase = skontoEdgeCase,
                    discountAmount = skontoData.skontoPercentageDiscounted,
                    remainingDays = skontoData.skontoRemainingDays
                ),
                enabled = digitalInvoice.skontoEnabled,
            )
        }
        digitalInvoice.selectedAndTotalLineItemsCount().let { (selected, total) ->
            footerDetails = footerDetails
                .copy(
                    totalGrossPriceIntegralAndFractionalParts =
                    digitalInvoice.totalPriceIntegralAndFractionalParts(),
                    buttonEnabled = digitalInvoice.getAmountToPay() > BigDecimal.ZERO,
                    count = selected,
                    total = total,
                    skontoSavedAmount = skontoSavedAmount
                        .takeIf { digitalInvoice.skontoEnabled },
                    skontoDiscountPercentage =
                    digitalInvoice
                        .skontoData
                        ?.skontoPercentageDiscounted
                        .takeIf { digitalInvoice.skontoEnabled }
                )
        }
        _uiState.update { state ->
            state.copy(
                lineItems = digitalInvoice.selectableLineItems,
                isInaccurateExtraction = isInaccurateExtraction,
                addons = digitalInvoice.addons,
                skontoListItem = skontoListItem,
                footerDetails = footerDetails,
                revision = state.revision + 1,
            )
        }

        val animateList = !shouldDisplayOnboarding() && !oncePerInstallEventStore.containsEvent(
            OncePerInstallEvent.SCROLL_DIGITAL_INVOICE
        )
        if (animateList) {
            oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SCROLL_DIGITAL_INVOICE)
            sendSideEffect(DigitalInvoiceSideEffect.AnimateListScroll)
        }
    }

    private fun sendSideEffect(sideEffect: DigitalInvoiceSideEffect) {
        viewModelScope.launch { _sideEffects.send(sideEffect) }
    }

    private fun trackScreenShownEvent() = runCatching {
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName)
            ) + if (skontoData != null) setOf(
                UserAnalyticsEventProperty
                    .SwitchActive(digitalInvoice.skontoEnabled)
            )
            else emptySet()
        )
    }

    private fun trackProceedTapped() = runCatching {
        val skontoEdgeCase = skontoEdgeCase
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.PROCEED_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
                    + if (skontoEdgeCase != null) {
                setOf(
                    UserAnalyticsEventProperty.EdgeCaseType(
                        skontoEdgeCase.toAnalyticsModel()
                    )
                )
            } else emptySet()
        )
    }
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class DigitalInvoiceUiState(
    val lineItems: List<SelectableLineItem> = emptyList(),
    val isInaccurateExtraction: Boolean = false,
    val addons: List<DigitalInvoiceAddon> = emptyList(),
    val skontoListItem: DigitalInvoiceSkontoListItem? = null,
    val footerDetails: FooterDetails? = null,
    /**
     * Increased on every view update. [SelectableLineItem]s are mutated in place by
     * [DigitalInvoice], so consecutive states can be structurally equal even though the view
     * must be re-rendered (e.g. re-selecting a line item after the return reason dialog was
     * cancelled). The revision makes every update distinct for the [StateFlow].
     */
    val revision: Int = 0,
)

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class FooterDetails(
    val inaccurateExtraction: Boolean,
    val buttonEnabled: Boolean = true,
    val count: Int = 0,
    val total: Int = 0,
    val skontoSavedAmount: Amount? = null,
    val skontoDiscountPercentage: BigDecimal? = null,
    val totalGrossPriceIntegralAndFractionalParts: Pair<String, String> = Pair("", ""),
)

/**
 * Internal use only.
 *
 * @suppress
 */
internal sealed interface DigitalInvoiceSideEffect {

    data class ShowReturnReasonDialog(
        val reasons: List<GiniCaptureReturnReason>,
        val lineItem: SelectableLineItem,
    ) : DigitalInvoiceSideEffect

    data class EditLineItem(val lineItem: SelectableLineItem) : DigitalInvoiceSideEffect

    object ShowOnboarding : DigitalInvoiceSideEffect

    data class ShowSkontoEditScreen(
        val data: SkontoData,
        val isSkontoSectionActive: Boolean,
    ) : DigitalInvoiceSideEffect

    object AnimateListScroll : DigitalInvoiceSideEffect

    data class PayInvoice(
        val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    ) : DigitalInvoiceSideEffect
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class DigitalInvoiceViewModelArgs(
    val extractions: Map<String, GiniCaptureSpecificExtraction>,
    val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    val returnReasons: List<GiniCaptureReturnReason>,
    val skontoData: SkontoData?,
    val isInaccurateExtraction: Boolean,
    val savedInstanceBundle: Bundle?,
)
