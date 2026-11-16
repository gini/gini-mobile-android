package net.gini.android.capture.ui.theme.colors

import androidx.annotation.ColorInt

/**
 * Provides the colors of the Gini palette at runtime, for example from your server.
 *
 * Set it with [net.gini.android.capture.GiniCapture.Builder.setCustomResourceProvider] (or
 * `CaptureConfiguration.customResourceProvider` in the Gini Bank SDK) before you start the SDK.
 * The SDK asks for every palette color it uses and applies your answer on all screens, both on
 * XML and on Compose screens. Return `null` to keep the default Gini color for a name, including
 * any override you made in your own `colors.xml`.
 *
 * The SDK asks for these 32 palette names:
 * - `gc_accent_01` … `gc_accent_05`
 * - `gc_dark_01` … `gc_dark_06`
 * - `gc_light_01` … `gc_light_06`
 * - `gc_success_01` … `gc_success_05`
 * - `gc_error_01` … `gc_error_05`
 * - `gc_warning_01` … `gc_warning_05`
 *
 * Rules:
 * - The method is called on the main thread and must answer synchronously. Load your colors
 *   before you start the SDK and answer from memory.
 * - Each name is asked at most once per mode for an SDK instance. To use other colors, configure
 *   the SDK again (a new SDK instance asks again).
 * - Colors are used exactly as returned. The contrast and accessibility of the colors you provide
 *   are your responsibility.
 * - If the method throws, the SDK logs the error and uses the default color.
 */
fun interface CustomResourceProvider {

    /**
     * Returns the color for a palette name.
     *
     * @param name the palette name, for example `gc_accent_01`
     * @param isDarkMode `true` when the SDK screen is shown in dark mode
     * @return an ARGB color, or `null` to use the default Gini color
     */
    @ColorInt
    fun customPreferredColor(name: String, isDarkMode: Boolean): Int?
}
