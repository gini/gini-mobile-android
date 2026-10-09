package net.gini.android.bank.sdk.exampleapp.ui.data

import net.gini.android.capture.ui.theme.colors.CustomResourceProvider

/**
 * A [CustomResourceProvider] with a deliberately loud test palette, used to check that every SDK
 * screen applies runtime colors. Light and dark mode use different hues, so a view that ignores
 * the mode is visible too.
 *
 * In a real app the colors would come from your server and be loaded before starting the SDK.
 */
class ExampleRuntimeColorsProvider : CustomResourceProvider {

    override fun customPreferredColor(name: String, isDarkMode: Boolean): Int? =
        (if (isDarkMode) DARK_PALETTE else LIGHT_PALETTE)[name]?.toInt()

    private companion object {
        val LIGHT_PALETTE: Map<String, Long> = mapOf(
            "gc_accent_01" to 0xFF8E24AA, "gc_accent_02" to 0xFFAB47BC, "gc_accent_03" to 0xFFCE93D8,
            "gc_accent_04" to 0xFFE1BEE7, "gc_accent_05" to 0xFFF3E5F5,
            "gc_dark_01" to 0xFF0D1B2A, "gc_dark_02" to 0xFF1B263B, "gc_dark_03" to 0xFF273A56,
            "gc_dark_04" to 0xFF34495E, "gc_dark_05" to 0xFF415A77, "gc_dark_06" to 0xFF5C6F86,
            "gc_light_01" to 0xFFFFF8E1, "gc_light_02" to 0xFFFFECB3, "gc_light_03" to 0xFFFFE082,
            "gc_light_04" to 0xFFFFD54F, "gc_light_05" to 0xFFFFCA28, "gc_light_06" to 0xFFFFC107,
            "gc_success_01" to 0xFF00695C, "gc_success_02" to 0xFF26A69A, "gc_success_03" to 0xFF80CBC4,
            "gc_success_04" to 0xFFE0F2F1, "gc_success_05" to 0xFF004D40,
            "gc_error_01" to 0xFFB71C1C, "gc_error_02" to 0xFFE53935, "gc_error_03" to 0xFFEF9A9A,
            "gc_error_04" to 0xFFFFEBEE, "gc_error_05" to 0xFF7F0000,
            "gc_warning_01" to 0xFFE65100, "gc_warning_02" to 0xFFFF9800, "gc_warning_03" to 0xFFFFCC80,
            "gc_warning_04" to 0xFFFFF3E0, "gc_warning_05" to 0xFFBF360C,
        )

        val DARK_PALETTE: Map<String, Long> = mapOf(
            "gc_accent_01" to 0xFFFF7043, "gc_accent_02" to 0xFFFF8A65, "gc_accent_03" to 0xFFFFAB91,
            "gc_accent_04" to 0xFFFFCCBC, "gc_accent_05" to 0xFFFBE9E7,
            "gc_dark_01" to 0xFF1A0F2E, "gc_dark_02" to 0xFF261640, "gc_dark_03" to 0xFF331E52,
            "gc_dark_04" to 0xFF402764, "gc_dark_05" to 0xFFB39DDB, "gc_dark_06" to 0xFF9575CD,
            "gc_light_01" to 0xFFE8F5E9, "gc_light_02" to 0xFFC8E6C9, "gc_light_03" to 0xFFA5D6A7,
            "gc_light_04" to 0xFF81C784, "gc_light_05" to 0xFF66BB6A, "gc_light_06" to 0xFF4CAF50,
            "gc_success_01" to 0xFF64FFDA, "gc_success_02" to 0xFF1DE9B6, "gc_success_03" to 0xFF00BFA5,
            "gc_success_04" to 0xFF004D40, "gc_success_05" to 0xFF00796B,
            "gc_error_01" to 0xFFFF8A80, "gc_error_02" to 0xFFFF5252, "gc_error_03" to 0xFFFF1744,
            "gc_error_04" to 0xFF4A0000, "gc_error_05" to 0xFFD50000,
            "gc_warning_01" to 0xFFFFD180, "gc_warning_02" to 0xFFFFAB40, "gc_warning_03" to 0xFFFF9100,
            "gc_warning_04" to 0xFF3E2723, "gc_warning_05" to 0xFFFF6D00,
        )
    }
}
