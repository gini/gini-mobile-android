package net.gini.android.capture.internal.camera.view

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.FragmentImplCallback

/**
 * Internal use only.
 *
 * @suppress
 */
internal class QRCodePopup<T> @JvmOverloads constructor(
    private val fragmentImplCallback: FragmentImplCallback,
    private val popupView: View,
    private val supportedBackgroundView: View? = null,
    private val hideDelayMs: Long,
    private val supported: Boolean,
    private val onClicked: (T?) -> Unit = {}
) {

    private var qrStatusTxt: TextView = popupView.findViewById(R.id.gc_qr_code_status)
    private var qrImageFrame: ImageView = popupView.findViewById(R.id.gc_camera_frame)
    private var qrCheckImage: ImageView = popupView.findViewById(R.id.gc_qr_code_check)
    private var mProgressBar: ProgressBar = popupView.findViewById(R.id.gc_activity_indicator)
    private var mInvoiceTxt: TextView = popupView.findViewById(R.id.gc_retrieving_invoice)

    private val hideRunnable: Runnable = Runnable {

        if (qrCodeContent != null) {
            onClicked(qrCodeContent)
        }

        if (supported) {
            progressViews()
        } else {
            hide()
        }
    }

    var qrCodeContent: T? = null
        private set

    var isShown = false
        private set

    fun show(qrCodeContent: T) {

        if (isShown) {
            return
        }
        show()
        this.qrCodeContent = qrCodeContent
    }

    private fun show() {
        if (qrStatusTxt.visibility == View.VISIBLE) {
            fragmentImplCallback.view?.removeCallbacks(hideRunnable)
            fragmentImplCallback.view?.postDelayed(hideRunnable, hideDelayMs)
            return
        }
        isShown = true
        showViews()
        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
        fragmentImplCallback.view?.postDelayed(hideRunnable, hideDelayMs)
    }

    fun hide() {
        qrCodeContent = null
        hideViews()
        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
    }

    private fun showViews() {
        qrStatusTxt.visibility = View.VISIBLE

        qrStatusTxt.text = if (supported) popupView.context.getString(R.string.gc_qr_code_detected)
        else popupView.context.getString(R.string.gc_unknown_qr_code)

        qrStatusTxt.background = if (supported) ContextCompat.getDrawable(
            popupView.context,
            R.drawable.gc_qr_code_detected_background
        )
        else ContextCompat.getDrawable(popupView.context, R.drawable.gc_qr_code_warning_background)

        qrStatusTxt.setTextColor(
            if (supported) ContextCompat.getColor(
                popupView.context,
                R.color.Light_01
            ) else ContextCompat.getColor(popupView.context, R.color.Dark_01)
        )

        qrCheckImage.visibility = if (supported) View.VISIBLE else View.GONE

        qrImageFrame.imageTintList = if (supported) ColorStateList.valueOf(
            ContextCompat.getColor(
                popupView.context,
                R.color.Success_01
            )
        )
        else ColorStateList.valueOf(ContextCompat.getColor(popupView.context, R.color.Warning_01))

        isShown = true
    }

    private fun progressViews() {

        qrCheckImage.visibility = View.GONE
        qrImageFrame.visibility = View.INVISIBLE
        mProgressBar.visibility = View.VISIBLE
        mInvoiceTxt.visibility = View.VISIBLE
        supportedBackgroundView?.visibility = View.VISIBLE
    }

    private fun hideViews() {

        qrStatusTxt.visibility = View.GONE
        qrCheckImage.visibility = View.GONE
        qrImageFrame.visibility = View.VISIBLE
        qrImageFrame.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(popupView.context, R.color.Light_01))
        mProgressBar.visibility = View.GONE
        mInvoiceTxt.visibility = View.GONE
        supportedBackgroundView?.visibility = View.GONE

        isShown = false
    }

}