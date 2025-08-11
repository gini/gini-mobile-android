package net.gini.android.internal.payment.utils.extensions

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
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
    class ViewModelAbsent : RuntimeException()

    val probeFactory = object : ViewModelProvider.Factory {
        // Newer API
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            throw ViewModelAbsent()
        }
        // Old API (still called on older artifacts)
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            throw ViewModelAbsent()
        }
    }

    return try {
        // If a VM with the default key already exists, this returns it and doesn't hit the factory.
        ViewModelProvider(this, probeFactory)[viewModelClass.java]
        true
    } catch (_: ViewModelAbsent) {
        false
    }
}