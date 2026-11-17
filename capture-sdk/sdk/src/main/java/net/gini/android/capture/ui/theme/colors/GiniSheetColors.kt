package net.gini.android.capture.ui.theme.colors

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of the fraud warning and IBAN selection sheets, each read from the `gc_*` color resource
 * that the Bank SDK UI customisation guide names for it, in light and dark mode.
 *
 * They come straight from [GiniTheme.primitives] rather than from a [GiniColorScheme] token: no
 * token pairs these resources the way the design does, and a new token would change the released
 * [GiniColorScheme] classes.
 */
internal object GiniSheetColors {

    /** Sheet background: `gc_light_01` / `gc_dark_02`. */
    val container: Color
        @Composable
        get() = lightOrDark(light = GiniTheme.primitives.light01, dark = GiniTheme.primitives.dark02)

    /** The dim layer behind a sheet: `gc_dark_01` at the Material 3 scrim alpha. */
    val scrim: Color
        @Composable
        get() = GiniTheme.primitives.dark01.copy(alpha = SCRIM_ALPHA)

    /** The selection sheet's drag handle: `gc_dark_06` / `gc_light_06`. */
    val dragHandle: Color
        @Composable
        get() = lightOrDark(light = GiniTheme.primitives.dark06, dark = GiniTheme.primitives.light06)

    /** Descriptions, labels and the footnote: `gc_dark_05` / `gc_light_05`. */
    val secondaryText: Color
        @Composable
        get() = lightOrDark(light = GiniTheme.primitives.dark05, dark = GiniTheme.primitives.light05)

    /**
     * The "How do I protect myself" row: `gc_accent_05` at 20%, as designed, in light mode.
     * Dark mode uses 12%: at 20% the row turns light enough that its caption text drops to 6.7:1
     * and the shield icon to 2.7:1, below the 7:1 and 3:1 targets.
     */
    val infoRowContainer: Color
        @Composable
        get() = GiniTheme.primitives.accent05.copy(
            alpha = lightOrDark(
                light = INFO_ROW_CONTAINER_ALPHA_LIGHT,
                dark = INFO_ROW_CONTAINER_ALPHA_DARK,
            )
        )

    @Composable
    private fun <T> lightOrDark(light: T, dark: T): T =
        if (GiniTheme.isDarkMode) dark else light

    private const val SCRIM_ALPHA = 0.32f
    private const val INFO_ROW_CONTAINER_ALPHA_LIGHT = 0.2f
    private const val INFO_ROW_CONTAINER_ALPHA_DARK = 0.12f
}
