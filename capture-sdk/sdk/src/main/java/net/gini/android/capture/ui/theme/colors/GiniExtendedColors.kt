package net.gini.android.capture.ui.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color roles of the design that [GiniColorScheme] does not have. They live here, internal to
 * the SDK, because [GiniColorScheme] is public API and a new property there would break it.
 * Like the scheme, every role has a light and a dark value built from the same primitives.
 *
 * @property textLabel small labels above a value (`android/text/label` in the design)
 * @property dialogContainer background of a dialog shown above a sheet
 * (`android/dialogs/container` in the design)
 * @property infoContainer tinted background of an informational row
 * @property radioButtonErrorContainer fill inside the ring of a radio button in the error state
 */
@Immutable
internal data class GiniExtendedColors(
    val textLabel: Color = Color.Unspecified,
    val dialogContainer: Color = Color.Unspecified,
    val infoContainer: Color = Color.Unspecified,
    val radioButtonErrorContainer: Color = Color.Unspecified,
)

internal fun giniLightExtendedColors(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = with(giniColorPrimitives) {
    GiniExtendedColors(
        textLabel = dark04,
        dialogContainer = light03,
        infoContainer = accent05.copy(alpha = INFO_CONTAINER_ALPHA),
        radioButtonErrorContainer = error03,
    )
}

internal fun giniDarkExtendedColors(
    giniColorPrimitives: GiniColorPrimitives = GiniColorPrimitives()
) = with(giniColorPrimitives) {
    GiniExtendedColors(
        textLabel = light03,
        dialogContainer = dark03,
        infoContainer = accent05.copy(alpha = INFO_CONTAINER_ALPHA),
        radioButtonErrorContainer = error03,
    )
}

private const val INFO_CONTAINER_ALPHA = 0.2f
