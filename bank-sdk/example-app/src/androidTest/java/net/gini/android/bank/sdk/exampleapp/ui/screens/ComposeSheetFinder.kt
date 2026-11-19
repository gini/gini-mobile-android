package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import java.util.regex.Pattern

/**
 * UiAutomator lookups inside one Compose modal window (a `ModalBottomSheet` or a `Dialog`) that
 * has no test tags, such as the capture SDK's fraud warning and IBAN selection sheet.
 *
 * ## Why every lookup is scoped to the sheet
 *
 * The IBAN selection sheet opens in its own window on top of the warning's window, and both list
 * the same labels and IBANs. A global `By.text(...)` may return the node in the warning behind,
 * and a click on its coordinates would land on the selection sheet's scrim and dismiss it. So
 * every lookup first finds the sheet's container and searches inside it only.
 *
 * The container is found from an anchor text the sheet always shows (its title, its buttons):
 * walking up from the anchor, it is the highest ancestor that does not start at the top of the
 * window. Both sheets sit below the status bar, while their window root and the scrim span the
 * whole window from y = 0.
 *
 * ## Scrolling
 *
 * Nodes scrolled out of the viewport are not visible to UiAutomator, so a lookup scrolls the
 * sheet's scrollable column until the node shows up. [canScrollUp] must be `false` for a sheet
 * that can be dragged down: a downward swipe on content that is already at the top would drag
 * the sheet and could dismiss it.
 *
 * @param anchorTexts texts of which at least one is on screen whenever the sheet is open
 * @param canScrollUp whether a downward swipe is safe on this sheet
 */
internal class ComposeSheetFinder(
    private val anchorTexts: () -> List<String>,
    private val canScrollUp: Boolean,
) {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private var lastContainer: UiObject2? = null

    fun isShown(): Boolean = anchor() != null

    fun waitUntilShown(timeoutMs: Long): Boolean = waitUntil(timeoutMs) { isShown() }

    fun waitUntilGone(timeoutMs: Long): Boolean = waitUntil(timeoutMs) { !isShown() }

    /**
     * The sheet's container, or `null` when the sheet is not open. See the class KDoc for how it
     * is found.
     */
    private fun container(): UiObject2? {
        var node = anchor() ?: return lastContainerIfAttached()
        try {
            while (true) {
                val parent = node.parent ?: break
                if (parent.visibleBounds.top <= 0) break
                node = parent
            }
        } catch (_: StaleObjectException) {
            return lastContainerIfAttached()
        }
        lastContainer = node
        return node
    }

    /**
     * The container found last, while it is still on screen. In landscape the sheet's middle
     * part (sender, IBAN list) can fill the whole window with no anchor text in view; the
     * container is then still the one found before the scroll.
     */
    private fun lastContainerIfAttached(): UiObject2? =
        try {
            lastContainer?.takeIf { !it.visibleBounds.isEmpty }
        } catch (_: StaleObjectException) {
            null
        }

    /** The node with exactly [text], or whose content description contains it; no scrolling. */
    fun findTextNow(text: String): UiObject2? =
        findNow(By.text(text)) ?: findNow(By.descContains(text))

    /**
     * Like [findTextNow], but scrolls the sheet until the node is on screen. Returns `null` when
     * the sheet holds no such text anywhere.
     */
    fun scrollToText(text: String): UiObject2? {
        findTextNow(text)?.let { return it }
        if (canScrollUp) {
            var upSteps = 0
            while (upSteps < MAX_SCROLLS && scrollStep(Direction.UP)) {
                upSteps++
                findTextNow(text)?.let { return it }
            }
            findTextNow(text)?.let { return it }
        }
        var downSteps = 0
        while (downSteps < MAX_SCROLLS && scrollStep(Direction.DOWN)) {
            downSteps++
            findTextNow(text)?.let { return it }
        }
        return findTextNow(text)
    }

    /** Scrolls to the node with [text] and taps it. Returns whether a node was tapped. */
    fun clickText(text: String): Boolean {
        val node = scrollToText(text) ?: return false
        return try {
            node.click()
            true
        } catch (_: StaleObjectException) {
            false
        }
    }

    private fun anchor(): UiObject2? =
        anchorTexts().firstNotNullOfOrNull { text ->
            try {
                device.findObject(By.text(text))?.takeIf { !it.visibleBounds.isEmpty }
            } catch (_: StaleObjectException) {
                null
            }
        }

    private fun findNow(selector: BySelector): UiObject2? =
        try {
            container()?.findObject(selector)?.takeIf { !it.visibleBounds.isEmpty }
        } catch (_: StaleObjectException) {
            null
        }

    /**
     * Scrolls the sheet one step. Returns whether the content moved, judged by the texts on
     * screen: `UiObject2.scroll`'s own "can scroll further" answer is unreliable for Compose and
     * stopped the search before the footer buttons in landscape.
     */
    private fun scrollStep(direction: Direction): Boolean {
        return try {
            val box = container() ?: return false
            val scrollable = box.findObject(By.scrollable(true))
                ?: box.takeIf { it.isScrollable }
                ?: return false
            val before = visibleTexts(box)
            scrollable.scroll(direction, SCROLL_PERCENT)
            val after = container()?.let(::visibleTexts) ?: return false
            after != before
        } catch (_: StaleObjectException) {
            false
        }
    }

    /** The texts in [box] that are on screen, with their positions, as a scroll snapshot. */
    private fun visibleTexts(box: UiObject2): List<String> =
        box.findObjects(By.text(ANY_TEXT))
            .filterNot { it.visibleBounds.isEmpty }
            .map { "${it.text}@${it.visibleBounds.top}" }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(POLL_INTERVAL)
        }
        return condition()
    }

    private companion object {
        const val MAX_SCROLLS = 10
        const val SCROLL_PERCENT = 0.6f
        const val POLL_INTERVAL = 200L
        val ANY_TEXT: Pattern = Pattern.compile(".+", Pattern.DOTALL)
    }
}
