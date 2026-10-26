package net.gini.android.capture.internal.camera.view

import android.content.res.Configuration
import android.util.TypedValue
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the unsupported QR code dialog on a real window. Robolectric cannot cover this: the
 * framework first measures a wrap-content dialog at its 320dp preferred width, so a fixed width
 * that passes in a unit test still collapses to 320dp on a device.
 */
@RunWith(AndroidJUnit4::class)
class QRCodePopupInstrumentedTest {

    @Test
    fun unsupportedQrDialog_inLandscape_hasTheConfiguredFractionOfTheScreenWidth() {
        val scenario: ActivityScenario<QRCodePopupTestActivity> =
            ActivityScenario.launch(QRCodePopupTestActivity::class.java)
        scenario.use {
            scenario.onActivity { activity -> activity.showUnsupportedQrDialog() }

            var orientation = Configuration.ORIENTATION_UNDEFINED
            var windowWidth = 0
            var expectedWidth = 0
            onView(withId(R.id.gc_unsupported_qr_dialog_title)).inRoot(isDialog())
                .check { view, noViewFoundException ->
                    noViewFoundException?.let { throw it }
                    val resources = view.resources
                    orientation = resources.configuration.orientation
                    windowWidth = view.rootView.width
                    // DecorView resolves the fraction against screenWidthDp.
                    val availableWidth = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        resources.configuration.screenWidthDp.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                    expectedWidth = resources.getFraction(
                        R.dimen.gc_unsupported_qr_code_dialog_min_width, availableWidth, availableWidth
                    ).toInt()
                }

            assertThat(orientation).isEqualTo(Configuration.ORIENTATION_LANDSCAPE)
            assertThat(windowWidth).isAtLeast(expectedWidth - 2)
            assertThat(windowWidth).isAtMost(expectedWidth + 2)
        }
    }
}
