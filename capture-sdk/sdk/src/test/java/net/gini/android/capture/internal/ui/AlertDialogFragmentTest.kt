package net.gini.android.capture.internal.ui

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class AlertDialogFragmentTest {

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `reports the positive button click with the dialog id to the activity`() {
        val (activity, dialog) = showDialog()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.idleMainLooper()

        assertThat(activity.positiveClicks).containsExactly(DIALOG_ID)
        assertThat(activity.negativeClicks).isEmpty()
    }

    @Test
    fun `reports the negative button click with the dialog id to the activity`() {
        val (activity, dialog) = showDialog()

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.idleMainLooper()

        assertThat(activity.negativeClicks).containsExactly(DIALOG_ID)
        assertThat(activity.positiveClicks).isEmpty()
    }

    private fun showDialog(): Pair<ListenerActivity, AlertDialog> {
        val activity = Robolectric.buildActivity(ListenerActivity::class.java).setup().get()
        val fragment = AlertDialogFragment.Builder()
            .setTitle(R.string.gc_title_camera)
            .setMessage(R.string.gc_title_camera)
            .setPositiveButton(android.R.string.ok)
            .setNegativeButton(android.R.string.cancel)
            .setDialogId(DIALOG_ID)
            .disableCancelOnTouchOutside()
            .create()
        fragment.show(activity.supportFragmentManager, "alert")
        ShadowLooper.idleMainLooper()
        return activity to fragment.requireDialog() as AlertDialog
    }

    class ListenerActivity : FragmentActivity(), AlertDialogFragmentListener {

        val positiveClicks = mutableListOf<Int>()
        val negativeClicks = mutableListOf<Int>()

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.GiniCaptureTheme)
            super.onCreate(savedInstanceState)
        }

        override fun onPositiveButtonClicked(dialog: DialogInterface, dialogId: Int) {
            positiveClicks.add(dialogId)
        }

        override fun onNegativeButtonClicked(dialog: DialogInterface, dialogId: Int) {
            negativeClicks.add(dialogId)
        }
    }

    private companion object {
        const val DIALOG_ID = 42
    }
}
