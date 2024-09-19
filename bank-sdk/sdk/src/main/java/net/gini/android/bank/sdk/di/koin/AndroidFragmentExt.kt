package net.gini.android.bank.sdk.di.koin

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import net.gini.android.bank.sdk.di.getGiniBankKoin
import org.koin.androidx.viewmodel.resolveViewModel
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.Qualifier

@MainThread
inline fun <reified T : ViewModel> Fragment.giniBankViewModel(
    qualifier: Qualifier? = null,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline parameters: (() -> ParametersHolder)? = null,
): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) {
        getGiniBankViewModel(qualifier, ownerProducer, extrasProducer, parameters)
    }
}

@OptIn(KoinInternalApi::class)
@MainThread
inline fun <reified T : ViewModel> Fragment.getGiniBankViewModel(
    qualifier: Qualifier? = null,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline parameters: (() -> ParametersHolder)? = null,
): T {
    return resolveViewModel(
        T::class,
        ownerProducer().viewModelStore,
        extras = extrasProducer?.invoke() ?: this.defaultViewModelCreationExtras,
        qualifier = qualifier,
        parameters = parameters,
        scope = getGiniBankKoin().scopeRegistry.rootScope
    )
}
