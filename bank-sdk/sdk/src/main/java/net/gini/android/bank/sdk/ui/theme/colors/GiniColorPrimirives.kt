package net.gini.android.bank.sdk.ui.theme.colors

import android.content.Context
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.R

/**
 * Color primitives of Gini based on Figma.
 *
 * Create a new instance of this class carefully because it contains hardcoded values as default.
 *
 * Use [GiniColorPrimitives.buildColorPrimitivesBasedOnResources] to create a bridge between XML
 * resource colors and this class
 */
internal data class GiniColorPrimitives(
    val accent01: Color = Color(0xFF0A84FF),
    val accent02: Color = Color(0xFF3193FD),
    val accent03: Color = Color(0xFF62ACFB),
    val accent04: Color = Color(0xFF93C4F9),
    val accent05: Color = Color(0xFFC4DDF7),

    val dark01: Color = Color(0xFF000000),
    val dark02: Color = Color(0xFF121212),
    val dark03: Color = Color(0xFF313131),
    val dark04: Color = Color(0xFF4A4A4A),
    val dark05: Color = Color(0xFF626262),
    val dark06: Color = Color(0xFF7A7A7A),

    val light01: Color = Color(0xFFFFFFFF),
    val light02: Color = Color(0xFFF2F2F2),
    val light03: Color = Color(0xFFE5E5E5),
    val light04: Color = Color(0xFFD9D9D9),
    val light05: Color = Color(0xFFCCCCCC),
    val light06: Color = Color(0xFFBFBFBF),

    val success01: Color = Color(0xFF09B523),
    val success02: Color = Color(0xFF32D74B),
    val success03: Color = Color(0xFFD0ECD4),
    val success04: Color = Color(0xFFDEEEE1),
    val success05: Color = Color(0xFF048016),

    val error01: Color = Color(0xFFD9190E),
    val error02: Color = Color(0xFFFF453A),
    val error03: Color = Color(0xFFECD0D0),
    val error04: Color = Color(0xFFEEDEDE),
    val error05: Color = Color(0xFF830801),

    val warning01: Color = Color(0xFFD9A00E),
    val warning02: Color = Color(0xFFFFC73A),
    val warning03: Color = Color(0xFFECE4D0),
    val warning04: Color = Color(0xFFEEEADE),
    val warning05: Color = Color(0xFFA17503),
) {
    companion object {
        /**
         * Bridge between old way of defining colors by overridden resources and Compose
         *
         * This function will define color primitives based on resources value at res/colors.xml
         */
        internal fun buildColorPrimitivesBasedOnResources(context: Context) = GiniColorPrimitives(
            accent01 = Color(context.getColor(R.color.gc_accent_01)),
            accent02 = Color(context.getColor(R.color.gc_accent_02)),
            accent03 = Color(context.getColor(R.color.gc_accent_03)),
            accent04 = Color(context.getColor(R.color.gc_accent_04)),
            accent05 = Color(context.getColor(R.color.gc_accent_05)),

            dark01 = Color(context.getColor(R.color.gc_dark_01)),
            dark02 = Color(context.getColor(R.color.gc_dark_02)),
            dark03 = Color(context.getColor(R.color.gc_dark_03)),
            dark04 = Color(context.getColor(R.color.gc_dark_04)),
            dark05 = Color(context.getColor(R.color.gc_dark_05)),
            dark06 = Color(context.getColor(R.color.gc_dark_06)),

            light01 = Color(context.getColor(R.color.gc_light_01)),
            light02 = Color(context.getColor(R.color.gc_light_02)),
            light03 = Color(context.getColor(R.color.gc_light_03)),
            light04 = Color(context.getColor(R.color.gc_light_04)),
            light05 = Color(context.getColor(R.color.gc_light_05)),
            light06 = Color(context.getColor(R.color.gc_light_06)),

            success01 = Color(context.getColor(R.color.gc_success_01)),
            success02 = Color(context.getColor(R.color.gc_success_02)),
            success03 = Color(context.getColor(R.color.gc_success_03)),
            success04 = Color(context.getColor(R.color.gc_success_04)),
            success05 = Color(context.getColor(R.color.gc_success_05)),

            error01 = Color(context.getColor(R.color.gc_error_01)),
            error02 = Color(context.getColor(R.color.gc_error_02)),
            error03 = Color(context.getColor(R.color.gc_error_03)),
            error04 = Color(context.getColor(R.color.gc_error_04)),
            error05 = Color(context.getColor(R.color.gc_error_05)),

            warning01 = Color(context.getColor(R.color.gc_warning_01)),
            warning02 = Color(context.getColor(R.color.gc_warning_02)),
            warning03 = Color(context.getColor(R.color.gc_warning_03)),
            warning04 = Color(context.getColor(R.color.gc_warning_04)),
        )
    }
}


