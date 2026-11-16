package net.gini.android.capture.di

import net.gini.android.capture.ingredientbrand.GetIngredientBrandVisibleUseCase
import org.koin.dsl.module

internal val ingredientBrandModule = module {

    factory {
        GetIngredientBrandVisibleUseCase(
            giniBankConfigurationProvider = get(),
        )
    }
}
