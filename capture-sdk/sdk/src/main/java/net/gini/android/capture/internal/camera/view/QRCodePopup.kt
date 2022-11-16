package net.gini.android.capture.internal.camera.view

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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
    private var onClicked: ((T?) -> Unit)? = {},
    private val onHide: (() -> Unit)? = null
) {

    private var qrStatusTxt: TextView = popupView.findViewById(R.id.gc_qr_code_status)
    private var qrImageFrame: ImageView = popupView.findViewById(R.id.gc_camera_frame)
    private var qrCheckImage: ImageView = popupView.findViewById(R.id.gc_qr_code_check)
    private var mProgressBar: ProgressBar = popupView.findViewById(R.id.gc_activity_indicator)
    private var mInvoiceTxt: TextView = popupView.findViewById(R.id.gc_retrieving_invoice)
    private var mUnknownQRCodeWrapper: ConstraintLayout =
        popupView.findViewById(R.id.gc_unknown_qr_wrapper);

    private val hideRunnable: Runnable = Runnable {

        if (qrCodeContent != null) {
            onClicked?.let { it(qrCodeContent) }
        }

        //Wait for a second to reset the QR Code content value
        Handler(Looper.getMainLooper()).postDelayed({
            onHide?.invoke()
        }, 1000)

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

        supportedBackgroundView?.visibility = if (supported) View.VISIBLE else View.GONE

        if (supported) {
            qrStatusTxt.visibility = View.VISIBLE
            qrStatusTxt.text = popupView.context.getString(R.string.gc_qr_code_detected)
            qrStatusTxt.background = ContextCompat.getDrawable(
                popupView.context,
                R.drawable.gc_qr_code_detected_background
            )
            qrCheckImage.visibility = View.VISIBLE
            qrStatusTxt.setTextColor(ContextCompat.getColor(popupView.context, R.color.Light_01))
            qrImageFrame.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    popupView.context,
                    R.color.Success_01
                )
            )
        } else {
            mUnknownQRCodeWrapper.visibility = View.VISIBLE
            qrImageFrame.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    popupView.context,
                    R.color.Warning_01
                )
            )
        }

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
        mUnknownQRCodeWrapper.visibility = View.GONE
        isShown = false
    }

}