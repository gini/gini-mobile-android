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
import androidx.constraintlayout.widget.ConstraintLayout
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
    private val onCaptureDocument: (() -> Unit)? = null,
    // Supplier instead of a captured Boolean: the warning type may not be known yet when the
    // popup is created (the persisted configuration loads asynchronously), so it is resolved
    // when the popup is actually shown.
    private val isNewWarningEnabled: () -> Boolean = { false }
) {

    private var unsupportedQrDialog: AlertDialog? = null

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
        // Resolve once per show so all decisions within this show cycle agree.
        val newWarningEnabled = isNewWarningEnabled()
        if (!supported && newWarningEnabled) {
            // New dialog requires explicit user interaction — no auto-dismiss timer
            showViews(newWarningEnabled)
            return
        }
        if (qrStatusTxt.visibility == View.VISIBLE) {
            fragmentImplCallback.view?.removeCallbacks(hideRunnable)
            fragmentImplCallback.view?.postDelayed(hideRunnable, hideDelayMs)
            return
        }
        showViews(newWarningEnabled)
        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
        fragmentImplCallback.view?.postDelayed(hideRunnable, hideDelayMs)
    }

    fun hide() {
        qrCodeContent = null
        hideViews()
        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
    }


    private fun showViews(newWarningEnabled: Boolean) {

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
            if (newWarningEnabled) {
                qrImageFrame.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(popupView.context, R.color.gc_error_02)
                )
                val themedContext = ContextThemeWrapper(popupView.context, R.style.GiniCaptureTheme)
                val dialogView = LayoutInflater.from(themedContext)
                    .inflate(R.layout.gc_dialog_unsupported_qr_code, null)
                unsupportedQrDialog = MaterialAlertDialogBuilder(themedContext)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()
                allowMultilineLabel(dialogView.findViewById(R.id.gc_btn_scan_another_qr_code))
                    .setOnClickListener {
                        unsupportedQrDialog?.dismiss()
                        hide()
                        onScanAnotherQRCode?.invoke()
                    }
                allowMultilineLabel(dialogView.findViewById(R.id.gc_btn_capture_document))
                    .setOnClickListener {
                        unsupportedQrDialog?.dismiss()
                        hide()
                        onCaptureDocument?.invoke()
                    }
                unsupportedQrDialog?.show()
            } else {
                mUnknownQRCodeWrapper.visibility = View.VISIBLE
                qrImageFrame.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(popupView.context, R.color.gc_warning_02)
                )
            }
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
        mUnknownQRCodeWrapper.visibility = View.GONE
        isShown = false
    }

    private fun performHapticFeedback(constant: Int) {
        popupView.performHapticFeedback(constant)
    }

    companion object {
        // The Material dialog button style (buttonBarButtonStyle) sets android:lines="1",
        // which overrides any android:maxLines from the layout because TextView applies the
        // "lines" attribute after "maxLines". Overriding it here after inflation lets long
        // labels wrap at large accessibility font scales instead of being ellipsized.
        fun allowMultilineLabel(button: Button): Button = button.apply {
            setSingleLine(false)
            maxLines = MAX_BUTTON_LABEL_LINES
        }

        private const val MAX_BUTTON_LABEL_LINES = 3
    }
}
