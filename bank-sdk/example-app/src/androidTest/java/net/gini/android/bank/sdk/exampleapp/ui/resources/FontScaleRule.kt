package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Runs the test with the device font size at [scale] (`2.0f` is the 200 % of the accessibility
 * settings). Needs [FontScaleRule] in the test class.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FontScale(val scale: Float)

/**
 * Sets the device font scale for a test annotated with [FontScale], and leaves every other test
 * alone.
 *
 * The font scale is one system setting (`settings put system font_scale`). Where the device
 * refuses it — the value read back is not the one asked for — the test is skipped, not failed,
 * like the dark mode switch in [LandscapeDarkModeRule].
 *
 * Declare it so that it wraps the activity rule — the scale must be set before `MainActivity`
 * starts, and again on every [RetryRule] attempt:
 *
 *     @get:Rule(order = Int.MIN_VALUE + 1)
 *     val fontScaleRule = FontScaleRule()
 *
 * The setting is device-wide, so the previous value is restored in `finally`, also when the test
 * fails.
 */
class FontScaleRule : TestRule {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun apply(base: Statement, description: Description): Statement {
        val fontScale = description.getAnnotation(FontScale::class.java) ?: return base
        return object : Statement() {
            override fun evaluate() {
                // A run that was killed leaves its own scale behind; do not restore to that.
                val previous = readFontScale()
                    ?.takeUnless { it == fontScale.scale }
                    ?: DEFAULT_SCALE
                try {
                    writeFontScale(fontScale.scale)
                    Assume.assumeTrue(
                        "Font scale could not be changed on this device",
                        waitUntil { readFontScale() == fontScale.scale }
                    )
                    SystemClock.sleep(SETTLE_MS)
                    base.evaluate()
                } finally {
                    writeFontScale(previous)
                }
            }
        }
    }

    /** The current value, or `null` when the setting was never written (the device default). */
    private fun readFontScale(): Float? =
        device.executeShellCommand("settings get system font_scale").trim().toFloatOrNull()

    private fun writeFontScale(scale: Float) {
        device.executeShellCommand("settings put system font_scale $scale")
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + APPLY_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(POLL_MS)
        }
        return condition()
    }

    private companion object {
        const val DEFAULT_SCALE = 1.0f
        const val APPLY_TIMEOUT_MS = 5_000L
        const val POLL_MS = 200L

        /** Lets the configuration change finish before the app starts. */
        const val SETTLE_MS = 1_500L
    }
}
