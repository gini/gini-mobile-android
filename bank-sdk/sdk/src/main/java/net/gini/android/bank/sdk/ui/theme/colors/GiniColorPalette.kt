package net.gini.android.bank.sdk.ui.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color scheme of Gini.
 * All colors are [Color.Transparent] by default
 * Represents a Figma color variables and structure.
 */
@Immutable
internal data class GiniColorScheme(
    val background: Background = Background(),
    val text: Text = Text(),
    val icons: Icons = Icons(),
    val chips: Chips = Chips(),
) {
    interface Component

    @Immutable
    data class Background(
        val background: Color = Color.Transparent,
        val surface: Color = Color.Transparent,
        val bar: Color = Color.Transparent,
        val listNormal: Color = Color.Transparent,
        val buttonEnabled: Color = Color.Transparent,
        val buttonFilled: Color = Color.Transparent,
        val inputUnfocused: Color = Color.Transparent,
        val inputFocused: Color = Color.Transparent,
        val divider: Color = Color.Transparent,
        val border: Color = Color.Transparent,
    ) : Component

    @Immutable
    data class Text(
        val primary: Color = Color.Transparent,
        val secondary: Color = Color.Transparent,
        val chipsAssistEnabled: Color = Color.Transparent,
        val chipsSuggestionEnabled: Color = Color.Transparent,
        val buttonEnabled: Color = Color.Transparent,
        val status: Color = Color.Transparent,
    ) : Component

    @Immutable
    data class Icons(
        val standardPrimary: Color = Color.Transparent,
        val standardSecondary: Color = Color.Transparent,
        val standardTertiary: Color = Color.Transparent,
    ) : Component

    @Immutable
    data class Chips(
        val suggestionEnabled: Color = Color.Transparent,
        val assistEnabled: Color = Color.Transparent,
    )
}

/**
 * Created a light color scheme based on primitives.
 */
internal fun giniLightColorScheme(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = GiniColorScheme(
    background = GiniColorScheme.Background(
        background = giniColorPrimitives.light02,
        surface = giniColorPrimitives.light01,
        bar = giniColorPrimitives.light01,
        listNormal = giniColorPrimitives.light01,
        buttonEnabled = giniColorPrimitives.accent01,
        inputUnfocused = giniColorPrimitives.light01,
        inputFocused = giniColorPrimitives.light01,
        divider = giniColorPrimitives.light03,
        border = giniColorPrimitives.light03,
        buttonFilled = giniColorPrimitives.light02,
    ), text = GiniColorScheme.Text(
        primary = giniColorPrimitives.dark02,
        secondary = giniColorPrimitives.dark06,
        chipsAssistEnabled = giniColorPrimitives.success02,
        chipsSuggestionEnabled = giniColorPrimitives.light01,
        buttonEnabled = giniColorPrimitives.light01,
        status = giniColorPrimitives.success01
    ), icons = GiniColorScheme.Icons(
        standardPrimary = giniColorPrimitives.dark01,
        standardSecondary = giniColorPrimitives.dark01,
        standardTertiary = giniColorPrimitives.dark05
    ), chips = GiniColorScheme.Chips(
        suggestionEnabled = giniColorPrimitives.success01,
        assistEnabled = giniColorPrimitives.success04
    )
)

/**
 * Created a dark color scheme based on primitives.
 */
internal fun giniDarkColorScheme(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = GiniColorScheme(
    background = GiniColorScheme.Background(
        background = giniColorPrimitives.dark01,
        surface = giniColorPrimitives.dark02,
        bar = giniColorPrimitives.dark02,
        listNormal = giniColorPrimitives.dark03,
        buttonEnabled = giniColorPrimitives.accent01,
        inputUnfocused = giniColorPrimitives.dark02,
        inputFocused = giniColorPrimitives.dark02,
        divider = giniColorPrimitives.dark03,
        border = giniColorPrimitives.dark03,
        buttonFilled = giniColorPrimitives.dark04,
    ), text = GiniColorScheme.Text(
        primary = giniColorPrimitives.light01,
        secondary = giniColorPrimitives.light06,
        chipsAssistEnabled = giniColorPrimitives.success02,
        chipsSuggestionEnabled = giniColorPrimitives.light01,
        buttonEnabled = giniColorPrimitives.light01,
        status = giniColorPrimitives.success01
    ), icons = GiniColorScheme.Icons(
        standardPrimary = giniColorPrimitives.light01,
        standardSecondary = giniColorPrimitives.light02,
        standardTertiary = giniColorPrimitives.light05,
    ), chips = GiniColorScheme.Chips(
        suggestionEnabled = giniColorPrimitives.success01,
        assistEnabled = giniColorPrimitives.success04,
    )
)

