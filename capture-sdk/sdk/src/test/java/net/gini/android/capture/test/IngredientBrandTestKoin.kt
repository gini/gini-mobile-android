package net.gini.android.capture.test

import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Registers the dependencies the ingredient brand code resolves from the SDK's isolated Koin
 * context.
 *
 * [GiniBankConfigurationProvider] is normally registered by the Bank SDK's DI bridge, so a
 * capture-sdk unit test has to provide its own definition. The Analysis screen resolves
 * `GetIngredientBrandVisibleUseCase` — which depends on it — while creating its view, so any test
 * that inflates that screen needs this.
 *
 * Exposed as an object with `@JvmStatic` members because the oldest Analysis tests are still Java
 * and cannot use Koin's Kotlin DSL.
 */
object IngredientBrandTestKoin {

    private var testModule: Module? = null

    @JvmStatic
    fun load() {
        val newModule = module {
            single { GiniBankConfigurationProvider() }
        }
        testModule = newModule
        getGiniCaptureKoin().loadModules(listOf(newModule))
    }

    @JvmStatic
    fun unload() {
        testModule?.let { getGiniCaptureKoin().unloadModules(listOf(it)) }
        testModule = null
        // Koin's unloadModules drops the overriding definition instead of restoring the previous
        // one, and getGiniCaptureKoin() is a process-wide isolated context — so the definition is
        // re-loaded for later test classes running in the same JVM.
        getGiniCaptureKoin().loadModules(
            listOf(module { single { GiniBankConfigurationProvider() } })
        )
    }
}
