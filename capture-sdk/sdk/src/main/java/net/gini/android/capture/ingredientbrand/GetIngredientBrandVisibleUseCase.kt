package net.gini.android.capture.ingredientbrand

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

/**
 * Tells whether the Gini ingredient brand element must be shown on [screen], based on the
 * `ingredientBrandScreens` list of the client configuration.
 *
 * Screen names are matched case-insensitively, and names the SDK does not know are ignored —
 * an unknown entry never turns the brand element on somewhere else.
 */
internal class GetIngredientBrandVisibleUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke(screen: IngredientBrandScreen): Boolean =
        giniBankConfigurationProvider.provide().ingredientBrandScreens
            .any { it.equals(screen.rawValue, ignoreCase = true) }
}
