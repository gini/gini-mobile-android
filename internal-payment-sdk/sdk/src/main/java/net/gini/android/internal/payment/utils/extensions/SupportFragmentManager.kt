package net.gini.android.internal.payment.utils.extensions

import androidx.fragment.app.FragmentManager
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.installApp.InstallAppBottomSheet
import net.gini.android.internal.payment.review.installApp.InstallAppForwardListener
import net.gini.android.internal.payment.review.openWith.OpenWithBottomSheet
import net.gini.android.internal.payment.review.openWith.OpenWithForwardListener
import net.gini.android.internal.payment.utils.BackListener

fun FragmentManager.showInstallAppBottomSheet(
    paymentComponent: PaymentComponent,
    minHeight: Int? = null,
    backListener: BackListener? = null,
    buttonClickListener: () -> Unit
) {
    val dialog = InstallAppBottomSheet.newInstance(paymentComponent, object :
        InstallAppForwardListener {
        override fun onForwardToBankSelected() {
            buttonClickListener()
        }
    }, backListener, minHeight)
    dialog.show(this, InstallAppBottomSheet::class.simpleName)
}

fun FragmentManager.showOpenWithBottomSheet(
    paymentProviderApp: PaymentProviderApp,
    backListener: BackListener? = null,
    buttonClickListener: () -> Unit
) {
    val dialog = OpenWithBottomSheet.newInstance(paymentProviderApp, object :
        OpenWithForwardListener {
        override fun onForwardSelected() {
            buttonClickListener()
        }
    }, backListener)
    dialog.show(this, OpenWithBottomSheet::class.java.name)
}