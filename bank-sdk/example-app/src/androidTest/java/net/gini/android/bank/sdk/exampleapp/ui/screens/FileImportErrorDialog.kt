package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf

class FileImportErrorDialog {

    // The dialog shows only after the file picker has closed and the SDK has checked the file.
    // For a large file that takes longer than the fixed idling wait, and an Espresso check made
    // while the picker is still in front fails with NoActivityResumedException. Wait for the
    // title first.
    fun waitForDialog(resourceId: Int, timeoutMs: Long = DIALOG_TIMEOUT_MS): Boolean {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val title = instrumentation.targetContext.getString(resourceId)
        return UiDevice.getInstance(instrumentation).findObject(UiSelector().text(title)).waitForExists(timeoutMs)
    }

    fun checkContentIsDisplayed(resourceId: Int, content: String): Boolean {
        var isContentPanelDisplayed = false
        onView(
            allOf(
                withText(resourceId),
                withText(content)
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isContentPanelDisplayed = true
                }
            }
        return isContentPanelDisplayed
    }

    private companion object {
        const val DIALOG_TIMEOUT_MS = 30_000L
    }
}
