package net.gini.android.internal.payment.utils.extensions

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.internal.payment.utils.BackListener
import kotlin.reflect.KClass

internal fun BottomSheetDialog.setBackListener(backListener: BackListener) {
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dismiss()
            backListener.backCalled()
            remove()
        }
    })
}

fun Fragment.isViewModelInitialized(viewModelClass: KClass<out ViewModel>): Boolean {
    return try {
        ViewModelProvider(this)[viewModelClass.java]
        true
    } catch (e: IllegalArgumentException) {
        false
    } catch (e: RuntimeException) {
        // Swallowed intentionally: absence of ViewModel is a valid case here
        false
    }
}