package net.gini.android.capture.internal.camera.view

import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val onHide: (() -> Unit)? = null,
    private val onScanAnotherQRCode: (() -> Unit)? = null,
    private val onCaptureDocument: (() -> Unit)? = null
) {

    private var unsupportedQrDialog: AlertDialog? = null

    private var qrStatusTxt: TextView = popupView.findViewById(R.id.gc_qr_code_status)
    private var qrImageFrame: ImageView = popupView.findViewById(R.id.gc_camera_frame)
    private var qrCheckImage: ImageView = popupView.findViewById(R.id.gc_qr_code_check)
    private var mInvoiceTxt: TextView = popupView.findViewById(R.id.gc_retrieving_invoice)
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
        if (!supported) {
            showViews()
            return
        }
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
            qrStatusTxt.announceForAccessibility(qrStatusTxt.text)
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
            qrImageFrame.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(popupView.context, R.color.gc_error_02)
            )
            val themedContext = ContextThemeWrapper(popupView.context, R.style.GiniCaptureTheme)
            val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.gc_dialog_unsupported_qr_code, null)
            unsupportedQrDialog = MaterialAlertDialogBuilder(themedContext)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            dialogView.findViewById<Button>(R.id.gc_btn_scan_another_qr_code).setOnClickListener {
                unsupportedQrDialog?.dismiss()
                hide()
                onScanAnotherQRCode?.invoke()
            }
            dialogView.findViewById<Button>(R.id.gc_btn_capture_document).setOnClickListener {
                unsupportedQrDialog?.dismiss()
                hide()
                onCaptureDocument?.invoke()
            }
            unsupportedQrDialog?.show()
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
        unsupportedQrDialog?.dismiss()
        unsupportedQrDialog = null
        qrStatusTxt.visibility = View.GONE
        qrCheckImage.visibility = View.GONE
        qrImageFrame.visibility = View.VISIBLE
        qrImageFrame.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(popupView.context, R.color.gc_light_01))
        loadingIndicatorContainer?.modifyAdapterIfOwned { it.onHidden() }
        mInvoiceTxt.visibility = View.GONE
        supportedBackgroundView?.visibility = View.GONE
        isShown = false
    }

    private fun performHapticFeedback(constant: Int) {
        popupView.performHapticFeedback(constant)
    }

}