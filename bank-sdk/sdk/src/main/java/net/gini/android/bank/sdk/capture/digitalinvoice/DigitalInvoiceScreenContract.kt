package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes
import net.gini.android.capture.GiniCaptureBasePresenter
import net.gini.android.capture.GiniCaptureBaseView
import net.gini.android.capture.network.model.GiniCaptureReturnReason

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
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
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
    }

    data class FooterDetails(
        val totalGrossPriceIntegralAndFractionalParts: Pair<String, String> = Pair("", ""),
        val buttonEnabled: Boolean = true,
        val count: Int = 0,
        val total: Int = 0,
        val inaccurateExtraction: Boolean
    )
}