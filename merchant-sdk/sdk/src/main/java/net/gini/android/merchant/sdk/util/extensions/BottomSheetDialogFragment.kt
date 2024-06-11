package net.gini.android.merchant.sdk.util.extensions

import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.merchant.sdk.util.BackListener

fun BottomSheetDialog.setBackListener(backListener: BackListener) {
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dismiss()
            backListener.backCalled()
            remove()
        }
    })
}