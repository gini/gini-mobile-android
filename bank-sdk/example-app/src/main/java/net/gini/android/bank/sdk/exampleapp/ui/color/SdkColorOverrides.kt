package net.gini.android.bank.sdk.exampleapp.ui.color

import net.gini.android.capture.R as CaptureR

/**
 * SPIKE palette for judging coverage of the "Override SDK colors" mechanism.
 *
 * Per explicit request, each token family uses a distinct color family:
 *  - accent  -> green
 *  - dark    -> purple  (dark neutral ramp)
 *  - light   -> yellow  (light neutral ramp)
 *  - success -> red
 *  - error   -> green
 *  - warning -> blue
 *
 * The same map is used for light and dark mode so the override is unmistakable in either mode;
 * mode-specific palettes will be defined when we build the real feature.
 */
object SdkColorOverrides {

    private val palette: Map<Int, Int> = mapOf(
        // accent -> green
        CaptureR.color.gc_accent_01 to 0xFF00C853.toInt(),
        CaptureR.color.gc_accent_02 to 0xFF00E676.toInt(),
        CaptureR.color.gc_accent_03 to 0xFF69F0AE.toInt(),
        CaptureR.color.gc_accent_04 to 0xFFB9F6CA.toInt(),
        CaptureR.color.gc_accent_05 to 0xFFE8F5E9.toInt(),
        CaptureR.color.gc_accent_06 to 0x4000C853.toInt(),

        // dark neutrals -> purple ramp (dark -> light)
        CaptureR.color.gc_dark_01 to 0xFF1A0033.toInt(),
        CaptureR.color.gc_dark_02 to 0xFF2E0A4A.toInt(),
        CaptureR.color.gc_dark_03 to 0xFF4A1A6E.toInt(),
        CaptureR.color.gc_dark_04 to 0xFF6A2C96.toInt(),
        CaptureR.color.gc_dark_05 to 0xFF8E4CC4.toInt(),
        CaptureR.color.gc_dark_06 to 0xFFB07FE0.toInt(),

        // light neutrals -> yellow ramp (light -> darker)
        CaptureR.color.gc_light_01 to 0xFFFFFDE7.toInt(),
        CaptureR.color.gc_light_02 to 0xFFFFF9C4.toInt(),
        CaptureR.color.gc_light_03 to 0xFFFFF176.toInt(),
        CaptureR.color.gc_light_04 to 0xFFFFEE58.toInt(),
        CaptureR.color.gc_light_05 to 0xFFFDD835.toInt(),
        CaptureR.color.gc_light_06 to 0xFFFBC02D.toInt(),

        // success -> red
        CaptureR.color.gc_success_01 to 0xFFC62828.toInt(),
        CaptureR.color.gc_success_02 to 0xFFEF5350.toInt(),
        CaptureR.color.gc_success_03 to 0xFFFFCDD2.toInt(),
        CaptureR.color.gc_success_04 to 0xFFFFEBEE.toInt(),
        CaptureR.color.gc_success_05 to 0xFF8E0000.toInt(),

        // error -> green
        CaptureR.color.gc_error_01 to 0xFF2E7D32.toInt(),
        CaptureR.color.gc_error_02 to 0xFF43A047.toInt(),
        CaptureR.color.gc_error_03 to 0xFFC8E6C9.toInt(),
        CaptureR.color.gc_error_04 to 0xFFE8F5E9.toInt(),
        CaptureR.color.gc_error_05 to 0xFF1B5E20.toInt(),

        // warning -> blue
        CaptureR.color.gc_warning_01 to 0xFF1565C0.toInt(),
        CaptureR.color.gc_warning_02 to 0xFF1E88E5.toInt(),
        CaptureR.color.gc_warning_03 to 0xFFBBDEFB.toInt(),
        CaptureR.color.gc_warning_04 to 0xFFE3F2FD.toInt(),

        CaptureR.color.gc_camera_preview_shade to 0xAA1A0033.toInt(),
    )

    val light: Map<Int, Int> = palette
    val dark: Map<Int, Int> = palette
}
