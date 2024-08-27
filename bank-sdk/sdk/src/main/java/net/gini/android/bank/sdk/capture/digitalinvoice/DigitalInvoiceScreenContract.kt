package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes
import net.gini.android.capture.Amount
import net.gini.android.capture.GiniCaptureBasePresenter
import net.gini.android.capture.GiniCaptureBaseView
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import java.math.BigDecimal

/**
 * Created by Alpar Szotyori on 05.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
interface DigitalInvoiceScreenContract {

    /**
     * Internal use only.
     *
     * @suppress
     */
    interface View : GiniCaptureBaseView<Presenter> {
        val viewLifecycleScope: CoroutineScope
        fun showLineItems(lineItems: List<SelectableLineItem>, isInaccurateExtraction: Boolean)
        fun showAddons(addons: List<DigitalInvoiceAddon>)
        fun showSkonto(data: DigitalInvoiceSkontoListItem)
        fun updateFooterDetails(data: FooterDetails)
        fun showReturnReasonDialog(
            reasons: List<GiniCaptureReturnReason>,
            resultCallback: ReturnReasonDialogResultCallback
        )

        fun animateListScroll()
        fun onEditLineItem(selectableLineItem: SelectableLineItem)

        fun showOnboarding()
        fun showSkontoEditScreen(
            data: SkontoData,
            isSkontoSectionActive: Boolean,
        )
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Suppress("TooManyFunctions")
    abstract class Presenter(activity: Activity, view: View) :
        GiniCaptureBasePresenter<View>(activity, view) {

        var listener: DigitalInvoiceFragmentListener? = null

        abstract fun selectLineItem(lineItem: SelectableLineItem)
        abstract fun deselectLineItem(lineItem: SelectableLineItem)
        abstract fun editLineItem(lineItem: SelectableLineItem)
        abstract fun pay()
        abstract fun onViewCreated()
        abstract fun saveState(outState: Bundle)
        abstract fun updateLineItem(selectableLineItem: SelectableLineItem)
        abstract fun enableSkonto()
        abstract fun disableSkonto()
        abstract fun updateSkontoData(skontoData: SkontoData?)
        abstract fun editSkontoDataListItem(skontoListItem: DigitalInvoiceSkontoListItem)
    }

    data class FooterDetails(
        val inaccurateExtraction: Boolean,
        val buttonEnabled: Boolean = true,
        val count: Int = 0,
        val total: Int = 0,
        val skontoSavedAmount: Amount? = null,
        val skontoDiscountPercentage: BigDecimal? = null,
        val totalGrossPriceIntegralAndFractionalParts: Pair<String, String> = Pair("", ""),
    )
}