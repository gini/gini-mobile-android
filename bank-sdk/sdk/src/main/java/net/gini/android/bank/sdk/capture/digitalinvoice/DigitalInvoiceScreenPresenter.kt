package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import net.gini.android.bank.sdk.di.getGiniBankKoin
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

internal class DigitalInvoiceScreenPresenter(
    activity: Activity,
    view: DigitalInvoiceScreenContract.View,
    val extractions: Map<String, GiniCaptureSpecificExtraction> = emptyMap(),
    val compoundExtractions: Map<String, GiniCaptureCompoundExtraction> = emptyMap(),
    val returnReasons: List<GiniCaptureReturnReason> = emptyList(),
    private var skontoData: SkontoData? = null,
    private val isInaccurateExtraction: Boolean = false,
    savedInstanceBundle: Bundle?,
    private val oncePerInstallEventStore: OncePerInstallEventStore = OncePerInstallEventStore(
        activity
    ),
    private val simpleBusEventStore: SimpleBusEventStore = SimpleBusEventStore(activity)
) : DigitalInvoiceScreenContract.Presenter(activity, view) {

    private var onboardingDisplayed: Boolean = savedInstanceBundle != null

    private var footerDetails =
        DigitalInvoiceScreenContract.FooterDetails(inaccurateExtraction = isInaccurateExtraction)

    private fun shouldDisplayOnboarding(): Boolean = !onboardingDisplayed &&
            !oncePerInstallEventStore
                .containsEvent(OncePerInstallEvent.SHOW_DIGITAL_INVOICE_ONBOARDING)

    private val digitalInvoice: DigitalInvoice
    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.ReturnAssistant

    private val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase
            by getGiniBankKoin().inject()
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase
            by getGiniBankKoin().inject()
    private val getSkontoAmountUseCase: GetSkontoAmountUseCase
            by getGiniBankKoin().inject()
    private val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase
            by getGiniBankKoin().inject()

    private val skontoInfoBannerTextFactory: SkontoInfoBannerTextFactory
            by getGiniBankKoin().inject()
    private var skontoEdgeCase: SkontoEdgeCase? = null

    init {
        view.setPresenter(this)
        skontoData = savedInstanceBundle?.getParcelable(KEY_SKONTO_DATA) ?: skontoData
        digitalInvoice = DigitalInvoice(
            extractions = extractions,
            compoundExtractions = compoundExtractions,
            savedSelectableItems = savedInstanceBundle?.getParcelableArray(
                KEY_SELECTABLE_ITEMS
            )?.filterIsInstance<SelectableLineItem>()?.toList(),
            skontoData = skontoData,
            getSkontoAmountUseCase = getSkontoAmountUseCase,
            getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
            getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase
        )
        savedInstanceBundle?.let {
            digitalInvoice.updateSkontoEnabled(it.getBoolean(KEY_SKONTO_STATE, false))
        }
    }

    override fun saveState(outState: Bundle) {
        outState.putParcelableArray(
            KEY_SELECTABLE_ITEMS,
            digitalInvoice.selectableLineItems.toTypedArray()
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

    override fun selectLineItem(lineItem: SelectableLineItem) {
        digitalInvoice.selectLineItem(lineItem)
        updateView()
    }

    override fun editSkontoDataListItem(skontoListItem: DigitalInvoiceSkontoListItem) {
        digitalInvoice.skontoData?.let { data ->
            view.showSkontoEditScreen(
                data = data,
                isSkontoSectionActive = skontoListItem.enabled
            )
        }
    }

    override fun enableSkonto() {
        digitalInvoice.updateSkontoEnabled(true)
        updateView()
    }

    override fun disableSkonto() {
        digitalInvoice.updateSkontoEnabled(false)
        updateView()
    }

    override fun updateSkontoData(skontoData: SkontoData?) {
        digitalInvoice.updateSkontoData(skontoData)
        updateView()
    }

    override fun deselectLineItem(lineItem: SelectableLineItem) {
        if (canShowReturnReasonsDialog()) {
            view.showReturnReasonDialog(returnReasons) { selectedReason ->
                if (selectedReason != null) {
                    digitalInvoice.deselectLineItem(lineItem, selectedReason)
                } else {
                    digitalInvoice.selectLineItem(lineItem)
                }
                updateView()
            }
        } else {
            digitalInvoice.deselectLineItem(lineItem, null)
            updateView()
        }
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

    override fun editLineItem(lineItem: SelectableLineItem) {
        view.onEditLineItem(lineItem)
    }

    override fun pay() {
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
        listener?.onPayInvoice(digitalInvoice.extractions, digitalInvoice.compoundExtractions)
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


    override fun updateLineItem(selectableLineItem: SelectableLineItem) {
        digitalInvoice.updateLineItem(selectableLineItem)
        updateView()
    }

    override fun onViewCreated() {
        simpleBusEventStore.observeChange(BusEvent.DISMISS_ONBOARDING_FRAGMENT)
            .onEach {
                if (it) {
                    updateView()
                }
            }
            .launchIn(view.viewLifecycleScope)
    }

    override fun start() {
        updateView()
        if (shouldDisplayOnboarding()) {
            simpleBusEventStore.saveEvent(BusEvent.DISMISS_ONBOARDING_FRAGMENT, false)
            onboardingDisplayed = true
            view.showOnboarding()
        } else {
            trackScreenShownEvent()
        }
    }

    override fun stop() {
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
        view.apply {
            showLineItems(digitalInvoice.selectableLineItems, isInaccurateExtraction)
            showAddons(digitalInvoice.addons)

            digitalInvoice.skontoData?.let { skontoData ->
                showSkonto(
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
                updateFooterDetails(footerDetails)
            }

            val animateList = !shouldDisplayOnboarding() && !oncePerInstallEventStore.containsEvent(
                OncePerInstallEvent.SCROLL_DIGITAL_INVOICE
            )
            if (animateList) {
                oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SCROLL_DIGITAL_INVOICE)
                animateListScroll()
            }
        }
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