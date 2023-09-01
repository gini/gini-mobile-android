package net.gini.android.capture.internal.camera.view

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.InjectedViewContainer

/**
 * Internal use only.
 *
 * @suppress
 */
internal class QRCodePopup<T> @JvmOverloads constructor(
    private val fragmentImplCallback: FragmentImplCallback,
    private val popupView: View,
    private val supportedBackgroundView: View? = null,
    private val loadingIndicatorContainer: InjectedViewContainer<CustomLoadingIndicatorAdapter>?,
    private val hideDelayMs: Long,
    private val supported: Boolean,
    private var onClicked: ((T?) -> Unit)? = {},
    private val onHide: (() -> Unit)? = null
) {

    private var qrStatusTxt: TextView = popupView.findViewById(R.id.gc_qr_code_status)
    private var qrImageFrame: ImageView = popupView.findViewById(R.id.gc_camera_frame)
    private var qrCheckImage: ImageView = popupView.findViewById(R.id.gc_qr_code_check)
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
            qrStatusTxt.setTextColor(ContextCompat.getColor(popupView.context, R.color.gc_light_01))
            qrImageFrame.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    popupView.context,
                    R.color.gc_success_05
                )
            )
            performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            performHapticFeedback(HapticFeedbackConstants.REJECT)
            mUnknownQRCodeWrapper.visibility = View.VISIBLE
            qrImageFrame.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    popupView.context,
                    R.color.gc_warning_02
                )
            )
        }

        isShown = true
    }

    private fun progressViews() {
        qrCheckImage.visibility = View.GONE
        qrImageFrame.visibility = View.INVISIBLE
        loadingIndicatorContainer?.modifyAdapterIfOwned { it.onVisible() }
        mInvoiceTxt.visibility = View.VISIBLE
        supportedBackgroundView?.visibility = View.VISIBLE
    }

    private fun hideViews() {
        qrStatusTxt.visibility = View.GONE
        qrCheckImage.visibility = View.GONE
        qrImageFrame.visibility = View.VISIBLE
        qrImageFrame.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(popupView.context, R.color.gc_light_01))
        loadingIndicatorContainer?.modifyAdapterIfOwned { it.onHidden() }
        mInvoiceTxt.visibility = View.GONE
        supportedBackgroundView?.visibility = View.GONE
        mUnknownQRCodeWrapper.visibility = View.GONE
        isShown = false
    }

    private fun performHapticFeedback(constant: Int) {
        popupView.performHapticFeedback(constant)
    }

}