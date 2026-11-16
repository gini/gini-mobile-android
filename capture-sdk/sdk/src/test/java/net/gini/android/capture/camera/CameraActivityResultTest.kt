package net.gini.android.capture.camera

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for the result intent [CameraActivity] returns from
 * [CameraActivity.onFinishedWithResult]: the extraction bundles are filled from the result and
 * the deprecated return reasons extra is always an empty list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
class CameraActivityResultTest {

    private val amountToPay =
        GiniCaptureSpecificExtraction("amountToPay", "12.00:EUR", "amount", null, emptyList())
    private val lineItems =
        GiniCaptureCompoundExtraction("lineItems", listOf(mapOf("description" to amountToPay)))

    private lateinit var activity: CameraActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(CameraActivity::class.java).get()
    }

    @Test
    fun `Success returns the extractions and an empty return reasons extra`() {
        activity.onFinishedWithResult(
            CaptureSDKResult.Success(
                mapOf("amountToPay" to amountToPay),
                mapOf("lineItems" to lineItems)
            )
        )

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(activity.isFinishing).isTrue()
        val intent = shadowActivity.resultIntent
        val extractions = intent.getBundleExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS)!!
        assertThat(extractions.getParcelable<GiniCaptureSpecificExtraction>("amountToPay"))
            .isEqualTo(amountToPay)
        val compoundExtractions = intent.getBundleExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS)!!
        assertThat(compoundExtractions.getParcelable<GiniCaptureCompoundExtraction>("lineItems"))
            .isEqualTo(lineItems)
        assertThat(
            intent.getParcelableArrayListExtra<GiniCaptureReturnReason>(CameraActivity.EXTRA_OUT_RETURN_REASONS)
        ).isEmpty()
    }

    @Test
    fun `Empty returns empty extraction bundles and an empty return reasons extra`() {
        activity.onFinishedWithResult(CaptureSDKResult.Empty)

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(activity.isFinishing).isTrue()
        val intent = shadowActivity.resultIntent
        assertThat(intent.getBundleExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS)!!.isEmpty).isTrue()
        assertThat(intent.getBundleExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS)!!.isEmpty).isTrue()
        assertThat(
            intent.getParcelableArrayListExtra<GiniCaptureReturnReason>(CameraActivity.EXTRA_OUT_RETURN_REASONS)
        ).isEmpty()
    }
}
