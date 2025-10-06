package net.gini.android.bank.sdk.di.koin

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import net.gini.android.bank.sdk.di.getGiniBankKoin
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.Qualifier

@MainThread
inline fun <reified T : ViewModel> Fragment.giniBankViewModel(
    qualifier: Qualifier? = null,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline parameters: (() -> ParametersHolder)? = null,
): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) {
        getGiniBankViewModel(qualifier, ownerProducer, parameters)
    }
}

@MainThread
inline fun <reified T : ViewModel> Fragment.getGiniBankViewModel(
    qualifier: Qualifier? = null,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline parameters: (() -> ParametersHolder)? = null,
): T {
    val owner = ownerProducer()

    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return getGiniBankKoin().get<T>(
                clazz = modelClass.kotlin,
                qualifier = qualifier,
                parameters = parameters
            )
        }
    }

    return ViewModelProvider(owner, factory)[T::class.java]
}
