package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Builds UiAutomator resource-id selectors against the app under test.
 *
 * The example-app's applicationId varies by flavor (e.g. the paymentProvider flavors
 * override it), so the package must be resolved at runtime rather than hard-coded into the
 * resource-id string. Keeping the resolution here means it lives in a single place.
 */
object AppResources {

    /** The app-under-test package (applicationId), e.g. "net.gini.android.bank.sdk.exampleapp". */
    val packageName: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

    /** Full UiAutomator resource-id, e.g. resId("transfer_summary") -> "<pkg>:id/transfer_summary". */
    fun resId(name: String): String = "$packageName:id/$name"
}
