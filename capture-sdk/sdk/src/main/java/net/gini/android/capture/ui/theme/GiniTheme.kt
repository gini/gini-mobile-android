package net.gini.android.capture.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives
import net.gini.android.capture.ui.theme.colors.GiniColorScheme
import net.gini.android.capture.ui.theme.colors.giniDarkColorScheme
import net.gini.android.capture.ui.theme.colors.giniLightColorScheme
import net.gini.android.capture.ui.theme.typography.GiniTypography
import net.gini.android.capture.ui.theme.typography.extractGiniTypography

internal val LocalGiniColors = staticCompositionLocalOf {
    GiniColorScheme()
}
internal val LocalGiniTypography = staticCompositionLocalOf {
    GiniTypography()
}

@Composable
fun GiniTheme(
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
            colorScheme = giniColorSchemeBridge(giniPrimitives),
            typography = MaterialTheme.typography.copy(),
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun giniColorSchemeBridge(
    giniColorPrimitives: GiniColorPrimitives,
    isLight: Boolean = !isSystemInDarkTheme()
) = with(giniColorPrimitives) {
    MaterialTheme.colorScheme.copy(
        primary = accent01,
        onPrimary = if (isLight) light01 else light01,
        background = if (isLight) light02 else dark01,
        onBackground = if (isLight) dark02 else light01,
        surface = if (isLight) light01 else dark02,
        onSurface = if (isLight) dark02 else light02,
        onSurfaceVariant = if (isLight) dark02 else light02,
        surfaceVariant = if (isLight) light01 else dark02,
        inverseOnSurface = if (isLight) dark02 else light02,
    )
}

object GiniTheme {

    val typography: GiniTypography
        @Composable
        get() = LocalGiniTypography.current

    val colorScheme: GiniColorScheme
        @Composable
        get() = LocalGiniColors.current
}
