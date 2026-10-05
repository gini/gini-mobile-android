package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.ExternalResource

/**
 * Dark mode for the whole test, and a way to turn the device to landscape once the SDK is open.
 *
 * Dark mode and portrait are both set before the app starts (the rule wraps the activity rule),
 * so no screen is recreated by them mid-test. Landscape is *not* set up front: the example app's main
 * screen hides its "Photo payment" button in landscape. The test enters the SDK in portrait and
 * calls [rotateToLandscape] on the camera screen, so the Analysis screen that follows opens
 * already in landscape and is never rotated — a rotation there rebuilds it, and during the invoice
 * education it would also hit the SDK's education-lock freeze (see IngredientBrandEducationTests).
 *
 * Both settings are system-wide, so [after] restores them even when the test fails.
 */
class LandscapeDarkModeRule : ExternalResource() {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun before() {
        // The tests open the SDK in portrait and turn the device on the camera screen, so the
        // app must start in portrait. Nothing else guarantees it: after() hands rotation back to
        // the device, and a device can still be in landscape when the next test (or the next
        // RetryRule attempt) launches the app — on BrowserStack that left the main screen in
        // landscape with the Photopayment button partly off screen. Portrait rather than
        // "natural": a tablet's natural orientation is landscape.
        device.setOrientationPortrait()
        waitUntil { !isLandscape() }
        device.executeShellCommand("cmd uimode night yes")
        waitUntil { isDarkModeOn() }
        SystemClock.sleep(SETTLE_MS)
    }

    override fun after() {
        device.executeShellCommand("cmd uimode night no")
        device.setOrientationNatural()
        device.unfreezeRotation()
    }

    /**
     * Turns the device to landscape and waits until the current screen has been rebuilt in it.
     * Call it on the camera screen, before a photo is taken or a QR code is scanned.
     */
    fun rotateToLandscape() {
        // setOrientationLandscape, not setOrientationLeft: "left" turns the device 90° from its
        // natural orientation, which is portrait on a tablet (whose natural orientation is
        // landscape). This call gives landscape on phones and tablets alike.
        device.setOrientationLandscape()
        waitUntil { isLandscape() }
        device.waitForIdle()
        SystemClock.sleep(SETTLE_MS)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /** `true` when the device reports dark mode, so a test can prove its own precondition. */
    fun isDarkModeOn(): Boolean =
        device.executeShellCommand("cmd uimode night").contains("yes")

    fun isLandscape(): Boolean = device.displayWidth > device.displayHeight

    private fun waitUntil(condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + APPLY_TIMEOUT_MS
        while (!condition() && SystemClock.uptimeMillis() < end) {
            SystemClock.sleep(POLL_MS)
        }
    }

    private companion object {
        const val APPLY_TIMEOUT_MS = 5_000L
        const val POLL_MS = 200L

        /** Lets a configuration change finish before the next step. */
        const val SETTLE_MS = 1_500L
    }
}
