package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Runs the test with the example app in the language [tag] (a BCP 47 tag, e.g. `de-DE`). Needs
 * [AppLocaleRule] in the test class. The tag must be one of the app's own locales in
 * `res/xml/locale_configs.xml`.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class AppLocale(val tag: String)

/**
 * Switches the example app's own language for a test annotated with [AppLocale], and leaves every
 * other test alone. The device language is not touched.
 *
 * It uses the per-app language of Android 13 (`LocaleManager.applicationLocales`). The
 * instrumentation runs in the app's process, so this is the app setting its own language: no shell
 * command and no permission. Below API 33 the test is skipped, not failed.
 *
 * Declare it so that it wraps the activity rule — the language must be set before `MainActivity`
 * starts, and again on every [RetryRule] attempt:
 *
 *     @get:Rule(order = Int.MIN_VALUE + 1)
 *     val appLocaleRule = AppLocaleRule()
 *
 * The setting is kept by the system, not in the app's data, so it is reset in `finally`, also
 * when the test fails, and does not rely on `clearPackageData`.
 */
class AppLocaleRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        val appLocale = description.getAnnotation(AppLocale::class.java) ?: return base
        return object : Statement() {
            override fun evaluate() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    throw AssumptionViolatedException("App locale needs API 33")
                }
                val requested = LocaleList.forLanguageTags(appLocale.tag)
                try {
                    setApplicationLocales(requested)
                    assertEquals(
                        "The app locale was not applied",
                        requested,
                        applicationLocales()
                    )
                    base.evaluate()
                } finally {
                    setApplicationLocales(LocaleList.getEmptyLocaleList())
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setApplicationLocales(locales: LocaleList) {
        localeManager().applicationLocales = locales
        // The new configuration reaches the app's process on the main thread.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun applicationLocales(): LocaleList = localeManager().applicationLocales

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun localeManager(): LocaleManager =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(LocaleManager::class.java)
}
