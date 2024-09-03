package net.gini.android.merchant.sdk.util.extensions

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.review.installApp.InstallAppBottomSheet
import net.gini.android.internal.payment.review.installApp.InstallAppForwardListener
import net.gini.android.merchant.sdk.review.openWith.OpenWithBottomSheet
import net.gini.android.merchant.sdk.review.openWith.OpenWithForwardListener
import net.gini.android.internal.payment.utils.BackListener

internal fun FragmentManager.add(@IdRes containerId: Int, fragment: Fragment, addToBackStack: Boolean) {
    beginTransaction()
     .add(containerId, fragment, fragment::class.java.name)
     .apply { if (addToBackStack) addToBackStack(fragment::class.java.name) }
     .commit()
}

internal fun FragmentManager.showInstallAppBottomSheet(paymentComponent: PaymentComponent, minHeight: Int? = null, backListener: BackListener? = null, buttonClickListener: () -> Unit) {
    val dialog = InstallAppBottomSheet.newInstance(paymentComponent, object :
        InstallAppForwardListener {
        override fun onForwardToBankSelected() {
            buttonClickListener()
        }
    }, backListener, minHeight)
    dialog.show(this, InstallAppBottomSheet::class.simpleName)
}

internal fun FragmentManager.showOpenWithBottomSheet(paymentProviderApp: PaymentProviderApp, backListener: BackListener? = null, buttonClickListener: () -> Unit) {
    val dialog = OpenWithBottomSheet.newInstance(paymentProviderApp, object: OpenWithForwardListener {
        override fun onForwardSelected() {
            buttonClickListener()
        }
    }, backListener)
    dialog.show(this, OpenWithBottomSheet::class.java.name)
}