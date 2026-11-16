package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.graphics.Rect
import android.os.SystemClock
import android.widget.ProgressBar
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import kotlin.math.abs

/**
 * The capture SDK's Analysis screen, and the parts of the camera screen's QR code overlay that
 * show the same Gini ingredient brand elements.
 *
 * Everything is looked up with UiAutomator and explicit timeouts, like [ExtractionScreen]: these
 * elements appear and disappear while an analysis runs, so a lookup must wait for them instead of
 * failing on the first frame. Only views that are VISIBLE are in the UiAutomator tree, so "not
 * displayed" below means GONE or INVISIBLE.
 *
 * The ids and strings belong to capture-sdk; they are merged into the app, so they resolve through
 * [AppResources] like the example app's own ids.
 */
class AnalysisScreen {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val poweredByGini: BySelector
        get() = By.res(AppResources.resId("gc_powered_by_gini"))

    /** The Gini mark is found by its TalkBack description, which is also what R8 asserts on. */
    private val giniLoadingIndicator: BySelector
        get() = By.desc(giniLoadingIndicatorDescription)

    val giniLoadingIndicatorDescription: String
        get() = targetContext.getString(
            net.gini.android.capture.R.string.gc_gini_loading_indicator_content_description
        )

    fun waitForAnalysisScreen(timeoutMs: Long = SCREEN_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { device.hasObject(By.res(AppResources.resId("gc_analysis_overlay"))) }

    fun isPoweredByGiniDisplayed(): Boolean = device.hasObject(poweredByGini)

    fun waitForPoweredByGini(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { isPoweredByGiniDisplayed() }

    fun waitForPoweredByGiniGone(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { !isPoweredByGiniDisplayed() }

    fun poweredByGiniBounds(): Rect? = device.findObject(poweredByGini)?.visibleBounds

    fun rootBounds(): Rect? =
        device.findObject(By.res(AppResources.resId("gc_layout_root")))?.visibleBounds

    fun analysisMessageBounds(): Rect? =
        device.findObject(By.res(AppResources.resId("gc_analysis_message")))?.visibleBounds

    fun isGiniLoadingIndicatorDisplayed(): Boolean = device.hasObject(giniLoadingIndicator)

    fun waitForGiniLoadingIndicator(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { isGiniLoadingIndicatorDisplayed() }

    fun giniLoadingIndicator(): UiObject2? = device.findObject(giniLoadingIndicator)

    /**
     * The capture SDK's own indicator: a plain ProgressBar inside the injected container. The
     * Analysis screen and the camera screen name that container differently.
     */
    fun isDefaultLoadingIndicatorDisplayed(): Boolean =
        LOADING_INDICATOR_CONTAINERS.any { container ->
            device.findObject(By.res(AppResources.resId(container)))
                ?.hasObject(By.clazz(ProgressBar::class.java)) == true
        }

    fun waitForDefaultLoadingIndicator(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { isDefaultLoadingIndicatorDisplayed() }

    /** The example app's Lottie indicator, used when "Screen custom loading indicator" is on. */
    fun isCustomLoadingIndicatorDisplayed(): Boolean =
        device.hasObject(By.res(AppResources.resId("animationView")))

    fun waitForCustomLoadingIndicator(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { isCustomLoadingIndicatorDisplayed() }

    fun isTipDisplayed(): Boolean =
        device.hasObject(By.res(AppResources.resId("gc_analysis_hint_container")))

    fun waitForTip(timeoutMs: Long = TIP_TIMEOUT): Boolean = waitUntil(timeoutMs) { isTipDisplayed() }

    fun currentTipHeadline(): String? =
        device.findObject(By.res(AppResources.resId("gc_analysis_hint_headline")))?.text

    /**
     * Waits until the tip headline differs from [previous], i.e. the next tip has slid in. The
     * tips cycle every few seconds, so this bounds the wait for one full cycle.
     */
    fun waitForNextTip(previous: String?, timeoutMs: Long = TIP_TIMEOUT): Boolean =
        waitUntil(timeoutMs) {
            val current = currentTipHeadline()
            current != null && current != previous
        }

    /**
     * The invoice education on the Analysis screen, shown by `AnimatedEducationMessageWithIntro`.
     * It is Compose text, which UiAutomator reports either as text or merged into a content
     * description, so both are checked.
     */
    fun isInvoiceEducationDisplayed(): Boolean = educationTexts().any { hasTextOrDescription(it) }

    fun waitForInvoiceEducation(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { isInvoiceEducationDisplayed() }

    fun waitForInvoiceEducationGone(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { !isInvoiceEducationDisplayed() }

    /**
     * The QR code education on the camera screen: the 1.5 s intro, then one of the two 3 s
     * messages. All three are checked — the intro alone is on screen too briefly to be caught
     * reliably by polling.
     */
    fun waitForQrCodeEducation(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean {
        val texts = listOf(
            net.gini.android.capture.R.string.gc_qr_education_intro_message,
            net.gini.android.capture.R.string.gc_qr_education_photo_doc_message,
            net.gini.android.capture.R.string.gc_qr_education_upload_picture_message
        ).map { targetContext.getString(it) }
        return waitUntil(timeoutMs) { texts.any { hasTextOrDescription(it) } }
    }

    /** The "QR code detected" popup on the camera screen. */
    fun waitForQrCodeDetected(timeoutMs: Long): Boolean =
        waitUntil(timeoutMs) {
            hasTextOrDescription(
                targetContext.getString(net.gini.android.capture.R.string.gc_qr_code_detected)
            )
        }

    /** Waits for any view whose text or content description contains the string [resId]. */
    fun waitForText(resId: Int, timeoutMs: Long = SCREEN_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { hasTextOrDescription(targetContext.getString(resId)) }

    /** Waits for the view with the resource id [idName] (a capture-sdk id), e.g. a screen header. */
    fun waitForView(idName: String, timeoutMs: Long = SCREEN_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { device.hasObject(By.res(AppResources.resId(idName))) }

    fun poweredByGiniContentDescription(): String? = device.findObject(poweredByGini)?.contentDescription

    /**
     * Checks the badge against the design (Figma 35002:11096, portrait and landscape): centred,
     * [BADGE_BOTTOM_MARGIN_DP] above the bottom edge of the Analysis screen, clear of the analysis
     * message. Returns what is wrong, or `null` when it is in place.
     */
    fun badgePlacementProblem(): String? {
        val badge = poweredByGiniBounds() ?: return "Powered by Gini badge is not shown"
        val root = rootBounds() ?: return "Analysis screen root not found"
        val tolerance = dpToPx(POSITION_TOLERANCE_DP)
        if (abs(badge.centerX() - root.centerX()) > tolerance) {
            return "Badge is not horizontally centred: badge=$badge root=$root"
        }
        val bottomGap = root.bottom - badge.bottom
        if (abs(bottomGap - dpToPx(BADGE_BOTTOM_MARGIN_DP)) > tolerance) {
            return "Badge must sit ${BADGE_BOTTOM_MARGIN_DP}dp above the bottom edge, gap was ${bottomGap}px"
        }
        val message = analysisMessageBounds()
        if (message != null && Rect.intersects(badge, message)) {
            return "Badge overlaps the analysis message: badge=$badge message=$message"
        }
        return null
    }

    fun tipBounds(): Rect? =
        device.findObject(By.res(AppResources.resId("gc_analysis_hint_container")))?.visibleBounds

    /**
     * The loading indicator on screen, whichever one it is: the Gini mark, the capture SDK's
     * ProgressBar, or the example app's Lottie indicator.
     */
    fun loadingIndicatorBounds(): Rect? =
        giniLoadingIndicator()?.visibleBounds
            ?: LOADING_INDICATOR_CONTAINERS.firstNotNullOfOrNull { container ->
                device.findObject(By.res(AppResources.resId(container)))
                    ?.findObject(By.clazz(ProgressBar::class.java))?.visibleBounds
            }
            ?: device.findObject(By.res(AppResources.resId("animationView")))?.visibleBounds

    /**
     * Waits until the tip card has stopped sliding in: it animates up from below the screen, so
     * its bounds are only final once two reads in a row agree.
     */
    fun waitForTipSettled(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean {
        var previous: Rect? = null
        return waitUntil(timeoutMs) {
            val current = tipBounds()
            val settled = current != null && current == previous
            previous = current
            settled
        }
    }

    /**
     * Checks that the tip card leaves the loading indicator and the analysis message uncovered,
     * as in the design (Figma 35002:11528, landscape with a tip). Call it after
     * [waitForTipSettled]. Returns what is wrong, or `null` when nothing is covered.
     */
    fun tipOverlapProblem(): String? {
        val tip = tipBounds() ?: return "Tip card is not shown"
        val indicator = loadingIndicatorBounds()
        if (indicator != null && Rect.intersects(tip, indicator)) {
            return "Tip card covers the loading indicator: tip=$tip indicator=$indicator"
        }
        val message = analysisMessageBounds()
        if (message != null && Rect.intersects(tip, message)) {
            return "Tip card covers the analysis message: tip=$tip message=$message"
        }
        return null
    }

    fun dpToPx(dp: Int): Int = (dp * targetContext.resources.displayMetrics.density).toInt()

    private fun educationTexts(): List<String> = listOf(
        targetContext.getString(net.gini.android.capture.R.string.gc_qr_education_intro_message),
        targetContext.getString(net.gini.android.capture.R.string.gc_invoice_education_message)
    )

    private fun hasTextOrDescription(value: String): Boolean =
        device.hasObject(By.textContains(value)) || device.hasObject(By.descContains(value))

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(POLL_INTERVAL)
        }
        return condition()
    }

    private companion object {
        /** Analysis screen, then camera screen (QR code overlay). */
        val LOADING_INDICATOR_CONTAINERS = listOf(
            "gc_injected_loading_indicator_container",
            "gc_injected_loading_indicator"
        )

        const val BADGE_BOTTOM_MARGIN_DP = 16
        const val POSITION_TOLERANCE_DP = 2

        const val SCREEN_TIMEOUT = 30_000L
        const val ELEMENT_TIMEOUT = 10_000L

        // The first tip slides in 5 s after the Analysis screen starts; the cycle adds 4.5 s.
        const val TIP_TIMEOUT = 15_000L
        const val POLL_INTERVAL = 200L
    }
}
