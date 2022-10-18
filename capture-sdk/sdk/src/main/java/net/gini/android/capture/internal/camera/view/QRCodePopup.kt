package net.gini.android.capture.internal.camera.view

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
        private val animationDuration: Long,
        private val hideDelayMs: Long,
        private val showAgainDelayMs: Long,
        private val supported: Boolean,
        private val onClicked: (T?) -> Unit = {}) {

    private var animation: ViewPropertyAnimatorCompat? = null

    private var qrStatusTxt: TextView = popupView.findViewById(R.id.gc_qr_code_status)
    private var qrImageFrame: ImageView = popupView.findViewById(R.id.gc_camera_frame)
    private var qrCheckImage: ImageView = popupView.findViewById(R.id.gc_qr_code_check)

    private val hideRunnable: Runnable = Runnable {
        hide()
    }

    var qrCodeContent: T? = null
        private set

    var isShown = false
        private set

    init {
        popupView.setOnClickListener {
            onClicked(qrCodeContent)
            hide()
        }
    }

    @JvmOverloads
    fun show(qrCodeContent: T, startDelay: Long = 0) {
        if (this.qrCodeContent != null && qrCodeContent != this.qrCodeContent) {
            hide(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View) {
                    show(showAgainDelayMs)
                }
            })
        } else {
            show(startDelay)
        }

        this.qrCodeContent = qrCodeContent
    }

    private fun show(startDelay: Long = 0) {
        if (popupView.alpha != 0f) {
            fragmentImplCallback.view?.removeCallbacks(hideRunnable)
            fragmentImplCallback.view?.postDelayed(hideRunnable, hideDelayMs)
            return
        }

        clearQRCodeDetectedPopUpAnimation()
        popupView.visibility = View.VISIBLE
        animation = ViewCompat.animate(popupView)
                .alpha(1.0f)
                .setStartDelay(startDelay)
                .setDuration(animationDuration)
                .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                    override fun onAnimationEnd(view: View) {
                        isShown = true
                    }
                })
                .apply {
                    start()
                }
        showViews()
        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
        fragmentImplCallback.view?.postDelayed(hideRunnable, hideDelayMs)
    }

    @JvmOverloads
    fun hide(animatorListener: ViewPropertyAnimatorListener? = null) {
        qrCodeContent = null

        if (popupView.alpha != 1f) {
            animatorListener?.onAnimationEnd(popupView)
            return
        }
        clearQRCodeDetectedPopUpAnimation()
        animation = ViewCompat.animate(popupView)
                .alpha(0.0f)
                .setDuration(animationDuration)
                .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                    override fun onAnimationEnd(view: View) {
                        popupView.visibility = View.GONE
                        isShown = false
                        animatorListener?.onAnimationEnd(view)
                    }
                })
                .apply {
                    start()
                }

        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
    }

    private fun clearQRCodeDetectedPopUpAnimation() {
        animation?.apply {
            cancel()
            popupView.clearAnimation()
            setListener(null)
        }
        fragmentImplCallback.view?.removeCallbacks(hideRunnable)
    }

    private fun showViews() {
        qrStatusTxt.visibility = View.VISIBLE

        qrStatusTxt.text = if (supported) popupView.context.getString(R.string.gc_qr_code_detected)
        else popupView.context.getString(R.string.gc_unknown_qr_code)

        qrStatusTxt.background = if (supported) ContextCompat.getDrawable(popupView.context, R.drawable.gc_qr_code_detected_background)
        else ContextCompat.getDrawable(popupView.context, R.drawable.gc_qr_code_warning_background)

        qrStatusTxt.setTextColor(if (supported) ContextCompat.getColor(popupView.context, R.color.Light_01) else ContextCompat.getColor(popupView.context, R.color.Dark_01))

        qrCheckImage.visibility = if(supported) View.VISIBLE else View.GONE

        qrImageFrame.imageTintList = if (supported) ColorStateList.valueOf(ContextCompat.getColor(popupView.context, R.color.Success_01))
        else ColorStateList.valueOf(ContextCompat.getColor(popupView.context, R.color.Warning_01))
    }
}