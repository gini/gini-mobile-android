package net.gini.android.bank.sdk.di.koin

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return getGiniBankKoin().get(
                clazz = modelClass.kotlin,
                qualifier = qualifier,
                parameters = parameters
            ) as T
        }
    }

    return ViewModelProvider(owner, factory)[T::class.java]
}
