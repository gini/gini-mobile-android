package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import net.gini.android.capture.view.InjectedViewAdapter

interface DigitalInvoiceOnboardingIllustrationAdapter: InjectedViewAdapter {

    fun onVisible()

    fun onHidden()

    fun onIllustrationSet(@DrawableRes drawableRes: Int)
}

class ImageDigitalInvoiceOnboardingIllustrationAdapter(): DigitalInvoiceOnboardingIllustrationAdapter {

    private var illustration: ImageView? = null

    override fun onVisible() {}

    override fun onHidden() {}

    override fun onIllustrationSet(drawableRes: Int) {
        illustration?.setImageResource(drawableRes)
    }

    override fun onCreateView(container: ViewGroup): View {
        illustration = ImageView(container.context).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        return illustration!!
    }

    override fun onDestroy() {}

}