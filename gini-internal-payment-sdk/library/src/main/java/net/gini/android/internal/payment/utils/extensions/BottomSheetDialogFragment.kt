package net.gini.android.internal.payment.utils.extensions

import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.internal.payment.utils.BackListener

internal fun BottomSheetDialog.setBackListener(backListener: BackListener) {
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dismiss()
            backListener.backCalled()
            remove()
        }
    })
}