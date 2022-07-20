package net.gini.android.bank.sdk.capture.digitalinvoice.details

import android.app.Activity
import net.gini.android.capture.GiniCaptureBasePresenter
import net.gini.android.capture.GiniCaptureBaseView
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.bank.sdk.capture.digitalinvoice.ReturnReasonDialogResultCallback

/**
 * Created by Alpar Szotyori on 17.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
interface LineItemDetailsScreenContract {

    /**
     * Internal use only.
     *
     * @suppress
     */
    interface View : GiniCaptureBaseView<Presenter> {
        fun showDescription(description: String)
        fun showQuantity(quantity: Int)
        fun showGrossPrice(displayedGrossPrice: String, currency: String)
        fun showCheckbox(selected: Boolean, quantity: Int, visible: Boolean)
        fun showTotalGrossPrice(integralPart: String, fractionalPart: String)
        fun enableSaveButton()
        fun disableSaveButton()
        fun enableInput()
        fun disableInput()
        fun showReturnReasonDialog(reasons: List<GiniCaptureReturnReason>,
                                   resultCallback: ReturnReasonDialogResultCallback
        )
        fun dismiss()
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    abstract class Presenter(activity: Activity, view: View) :
            GiniCaptureBasePresenter<View>(activity, view), LineItemDetailsFragmentInterface {

        abstract fun selectLineItem()
        abstract fun deselectLineItem()
        abstract fun setDescription(description: String)
        abstract fun setQuantity(quantity: Int)
        abstract fun setGrossPrice(displayedGrossPrice: String)
        abstract fun save(isBack: Boolean)
        abstract fun onQuantityInputFieldFocusLoss(inputFieldText: String)
    }
}
