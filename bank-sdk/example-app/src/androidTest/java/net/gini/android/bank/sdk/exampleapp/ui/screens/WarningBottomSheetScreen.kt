package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import net.gini.android.capture.R as CaptureR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import java.util.Locale

/**
 * Page object for the capture SDK's warning bottom sheet (Due Date Hint, Schedule Payment
 * and Credit Note states) shown on the Analysis screen.
 *
 * The two due date states share the same title (gc_due_date_hint_title), and the Credit Note
 * state shares its CTA labels with them in the opposite order, so state assertions always
 * check the description and both CTA labels as well — never the title alone.
 * All texts are resolved from the SDK's string resources at runtime, which keeps the
 * assertions locale-independent.
 */
class WarningBottomSheetScreen {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Waits for the sheet to appear. The sheet only shows after the upload + analysis
     * round trip and the education animation complete, so the wait is generous.
     */
    fun waitForSheet(): Boolean =
        device.findObject(UiSelector().resourceId(AppResources.resId("warningTitle")))
            .waitForExists(SHEET_TIMEOUT)

    fun isSheetDisplayed(): Boolean =
        device.findObject(UiSelector().resourceId(AppResources.resId("warningTitle"))).exists()

    fun assertDueDateHintState(formattedDueDate: String): WarningBottomSheetScreen {
        assertSheetTexts(
            title = targetContext.getString(
                CaptureR.string.gc_due_date_hint_title,
                formattedDueDate
            ),
            description = targetContext.getString(CaptureR.string.gc_due_date_hint_desc),
            primaryButton = targetContext.getString(CaptureR.string.gc_proceed_anyway),
            secondaryButton = targetContext.getString(CaptureR.string.gc_cancel_transfer)
        )
        return this
    }

    fun assertSchedulePaymentState(formattedDueDate: String): WarningBottomSheetScreen {
        assertSheetTexts(
            title = targetContext.getString(
                CaptureR.string.gc_due_date_hint_title,
                formattedDueDate
            ),
            description = targetContext.getString(CaptureR.string.gc_schedule_payment_hint_desc),
            primaryButton = targetContext.getString(CaptureR.string.gc_schedule_payment),
            secondaryButton = targetContext.getString(CaptureR.string.gc_proceed_anyway)
        )
        return this
    }

    /**
     * Asserts the Credit Note warning state: the title, the description and both CTAs.
     *
     * The CTA order is the reverse of the due date states — DOCUMENT_MARKED_AS_CREDIT_NOTE
     * declares "Cancel transfer" as the primary (filled) button and "Proceed anyway" as the
     * secondary, which is what the Xray steps describe as blue and white respectively. Getting
     * this backwards would still match a title-only assertion, so both labels are checked.
     */
    fun assertCreditNoteState(): WarningBottomSheetScreen {
        assertSheetTexts(
            title = targetContext.getString(
                CaptureR.string.gc_document_marked_credit_note_title
            ),
            description = targetContext.getString(
                CaptureR.string.gc_document_marked_credit_note_desc
            ),
            primaryButton = targetContext.getString(CaptureR.string.gc_cancel_transfer),
            secondaryButton = targetContext.getString(CaptureR.string.gc_proceed_anyway)
        )
        return this
    }

    /**
     * [assertCreditNoteState] for a test that sets the app's language itself (`@AppLocale`): the
     * four texts are read from the SDK's resources for [localeTag], not from whatever language the
     * app happens to run in — so a sheet that stayed in the wrong language fails here.
     *
     * For any language but English, each text must also differ from the English one. Without that,
     * a missing translation (the resources falling back to English) would still pass.
     */
    fun assertCreditNoteStateIn(localeTag: String): WarningBottomSheetScreen {
        val expected = CreditNoteTexts.of(localizedContext(localeTag))
        if (localeTag != ENGLISH_LOCALE_TAG) {
            val english = CreditNoteTexts.of(localizedContext(ENGLISH_LOCALE_TAG))
            expected.all().zip(english.all()).forEach { (localized, englishText) ->
                assertNotEquals(
                    "The $localeTag text must differ from the English one",
                    englishText,
                    localized
                )
            }
        }
        assertSheetTexts(
            title = expected.title,
            description = expected.description,
            primaryButton = expected.primaryButton,
            secondaryButton = expected.secondaryButton
        )
        return this
    }

    /**
     * The structural half of "not distorted, no overlap" at a large font size. It proves that
     * nothing is cut off or overlapping, not that the sheet looks right — that stays manual.
     *
     * 1. Neither button's text is ellipsized or taller than the button.
     * 2. Both buttons are completely on screen.
     * 3. The two buttons do not overlap each other.
     * 4. Neither button overlaps the visible part of the text's scroll container.
     * 5. The title and the description are each completely on screen, or the scroll container
     *    can scroll, so the hidden part is reachable.
     *
     * Before any of that, the sheet itself must run at [expectedFontScale]. Without it the checks
     * would pass at the normal size on a device that took the setting but did not apply it.
     */
    fun assertSheetIsNotCutOff(expectedFontScale: Float): WarningBottomSheetScreen {
        onView(withId(CaptureR.id.warningTitle)).check { view, noViewFoundException ->
            if (noViewFoundException != null) throw noViewFoundException
            assertEquals(
                "The sheet is not shown at the font scale under test",
                expectedFontScale,
                view.resources.configuration.fontScale,
                FONT_SCALE_TOLERANCE
            )
        }
        val primary = assertButtonIsNotCutOff(CaptureR.id.primary_button, "Primary button")
        val secondary = assertButtonIsNotCutOff(CaptureR.id.secondary_button, "Secondary button")
        assertFalse(
            "The two buttons overlap: $primary and $secondary",
            Rect.intersects(primary, secondary)
        )

        val textScroll = assertTextIsReachable(CaptureR.id.warningTitle, "Title")
        assertTextIsReachable(CaptureR.id.warningDescription, "Description")
        assertFalse(
            "Primary button $primary overlaps the text $textScroll",
            Rect.intersects(primary, textScroll)
        )
        assertFalse(
            "Secondary button $secondary overlaps the text $textScroll",
            Rect.intersects(secondary, textScroll)
        )
        return this
    }

    /**
     * Sends TAB, as a hardware keyboard would, until "Proceed anyway" (the secondary button) has
     * the focus. "Cancel transfer" (the primary button) must have had it on an earlier press.
     * How many presses that takes is not asserted, only that [maxPresses] are enough.
     */
    fun focusSecondaryButtonWithTab(maxPresses: Int): WarningBottomSheetScreen {
        var primaryWasFocused = false
        repeat(maxPresses) {
            device.pressKeyCode(KeyEvent.KEYCODE_TAB)
            device.waitForIdle()
            if (isFocused(CaptureR.id.secondary_button)) {
                assertTrue(
                    "Cancel transfer did not get the focus before Proceed anyway",
                    primaryWasFocused
                )
                return this
            }
            primaryWasFocused = primaryWasFocused || isFocused(CaptureR.id.primary_button)
        }
        throw AssertionError("Proceed anyway was not reachable with the keyboard")
    }

    /** Sends ENTER, as a hardware keyboard would. It activates the focused button. */
    fun pressEnter(): WarningBottomSheetScreen {
        device.pressKeyCode(KeyEvent.KEYCODE_ENTER)
        device.waitForIdle()
        return this
    }

    fun clickPrimaryButton() {
        onView(withId(CaptureR.id.primary_button)).perform(click())
    }

    fun clickSecondaryButton() {
        onView(withId(CaptureR.id.secondary_button)).perform(click())
    }

    /**
     * Taps the dimmed scrim above the sheet. The tap lands halfway between the top of
     * the screen and the sheet's top edge, floored so it can never hit the status bar
     * (a status-bar tap could pull down the notification shade and break the test).
     */
    fun tapOutsideSheet(): WarningBottomSheetScreen {
        val sheetTop = device.findObject(
            UiSelector().resourceId(AppResources.resId("warningTitle"))
        ).bounds.top
        device.click(device.displayWidth / 2, maxOf(MIN_OUTSIDE_TAP_Y, sheetTop / 2))
        return this
    }

    /**
     * Watches the rotating capture suggestions behind the sheet for [timeoutMs] and
     * reports whether they ever appeared. A GONE view is absent from the accessibility
     * tree, so waitForExists() returning false means they stayed suppressed for the
     * whole window. The window must exceed AnalysisHintsAnimator.HINT_START_DELAY
     * (5000 ms) to be meaningful.
     */
    fun didCaptureSuggestionsAppearWithin(timeoutMs: Long): Boolean =
        device.findObject(
            UiSelector().resourceId(AppResources.resId("gc_analysis_hint_container"))
        ).waitForExists(timeoutMs)

    private fun assertSheetTexts(
        title: String,
        description: String,
        primaryButton: String,
        secondaryButton: String
    ) {
        onView(withId(CaptureR.id.warningTitle)).check(matches(withText(title)))
        onView(withId(CaptureR.id.warningDescription)).check(matches(withText(description)))
        onView(withId(CaptureR.id.primary_button)).check(matches(withText(primaryButton)))
        onView(withId(CaptureR.id.secondary_button)).check(matches(withText(secondaryButton)))
    }

    private fun localizedContext(localeTag: String): Context {
        val configuration = Configuration(targetContext.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(localeTag))
        return targetContext.createConfigurationContext(configuration)
    }

    private fun isFocused(@IdRes id: Int): Boolean {
        var focused = false
        onView(withId(id)).check { view, noViewFoundException ->
            if (noViewFoundException != null) throw noViewFoundException
            focused = hasFocus().matches(view)
        }
        return focused
    }

    /** Checks 1 and 2 of [assertSheetIsNotCutOff]. Returns the button's rectangle in its window. */
    private fun assertButtonIsNotCutOff(@IdRes id: Int, name: String): Rect {
        val bounds = Rect()
        onView(withId(id)).check { view, noViewFoundException ->
            if (noViewFoundException != null) throw noViewFoundException
            val button = view as TextView
            val textLayout = button.layout ?: throw AssertionError("$name has no text")
            for (line in 0 until textLayout.lineCount) {
                assertEquals(
                    "$name: \"${button.text}\" is ellipsized on line $line",
                    0,
                    textLayout.getEllipsisCount(line)
                )
            }
            val textSpace = button.height - button.compoundPaddingTop - button.compoundPaddingBottom
            assertTrue(
                "$name: \"${button.text}\" needs ${textLayout.height} px but has $textSpace px",
                textLayout.height <= textSpace
            )
            assertTrue("$name is not completely on screen", isCompletelyDisplayed().matches(button))
            bounds.set(boundsInWindow(button))
        }
        return bounds
    }

    /**
     * Check 5 of [assertSheetIsNotCutOff]. Returns the visible rectangle, in its window, of the
     * nearest scroll container above the text (`textScroll` in portrait and on tablets,
     * `contentScroll` in landscape).
     */
    private fun assertTextIsReachable(@IdRes id: Int, name: String): Rect {
        val visibleScrollBounds = Rect()
        onView(withId(id)).check { view, noViewFoundException ->
            if (noViewFoundException != null) throw noViewFoundException
            val scrollContainer = nearestScrollContainer(view)
                ?: throw AssertionError("$name is not inside a scroll container")
            assertTrue(
                "$name is cut off and its container cannot scroll",
                isCompletelyDisplayed().matches(view) ||
                    scrollContainer.canScrollVertically(SCROLL_DOWN) ||
                    scrollContainer.canScrollVertically(SCROLL_UP)
            )
            scrollContainer.getGlobalVisibleRect(visibleScrollBounds)
        }
        return visibleScrollBounds
    }

    private fun nearestScrollContainer(view: View): View? {
        var parent = view.parent
        while (parent != null) {
            if (parent is NestedScrollView || parent is ScrollView) return parent as View
            parent = parent.parent
        }
        return null
    }

    private fun boundsInWindow(view: View): Rect {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    /** The four texts of the Credit Note warning state, read from one context's resources. */
    private class CreditNoteTexts(
        val title: String,
        val description: String,
        val primaryButton: String,
        val secondaryButton: String
    ) {
        fun all(): List<String> = listOf(title, description, primaryButton, secondaryButton)

        companion object {
            fun of(context: Context) = CreditNoteTexts(
                title = context.getString(CaptureR.string.gc_document_marked_credit_note_title),
                description = context.getString(CaptureR.string.gc_document_marked_credit_note_desc),
                primaryButton = context.getString(CaptureR.string.gc_cancel_transfer),
                secondaryButton = context.getString(CaptureR.string.gc_proceed_anyway)
            )
        }
    }

    companion object {
        private const val SHEET_TIMEOUT = 30_000L
        private const val MIN_OUTSIDE_TAP_Y = 200
        private const val ENGLISH_LOCALE_TAG = "en-GB"
        private const val SCROLL_DOWN = 1
        private const val SCROLL_UP = -1
        private const val FONT_SCALE_TOLERANCE = 0.01f
    }
}
