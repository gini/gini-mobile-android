package net.gini.android.bank.sdk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import net.gini.android.bank.sdk.ui.theme.colors.GiniColorPrimitives
import net.gini.android.bank.sdk.ui.theme.colors.GiniColorScheme
import net.gini.android.bank.sdk.ui.theme.colors.giniDarkColorScheme
import net.gini.android.bank.sdk.ui.theme.colors.giniLightColorScheme
import net.gini.android.bank.sdk.ui.theme.typography.GiniTypography
import net.gini.android.bank.sdk.ui.theme.typography.extractGiniTypography

internal val LocalGiniColors = staticCompositionLocalOf {
    GiniColorScheme()
}
internal val LocalGiniTypography = staticCompositionLocalOf {
    GiniTypography()
}

@Composable
internal fun GiniTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val giniPrimitives =
        remember { GiniColorPrimitives.buildColorPrimitivesBasedOnResources(context) }

    val colors = if (isSystemInDarkTheme()) {
        giniDarkColorScheme(giniPrimitives)
    } else {
        giniLightColorScheme(giniPrimitives)
    }

    val typography = extractGiniTypography()

    CompositionLocalProvider(
        LocalGiniTypography provides typography,
        LocalGiniColors provides colors
    ) {
        MaterialTheme(
            /* colors = ... */
            content = content,
            colorScheme = MaterialTheme.colorScheme.copy(),
            typography = MaterialTheme.typography.copy()
        )
    }
}

internal object GiniTheme {

    val typography: GiniTypography
        @Composable
        get() = LocalGiniTypography.current

    val colorScheme: GiniColorScheme
        @Composable
        get() = LocalGiniColors.current
}
