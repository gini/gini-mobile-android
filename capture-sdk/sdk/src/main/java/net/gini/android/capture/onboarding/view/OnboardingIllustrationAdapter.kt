package net.gini.android.capture.onboarding.view

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * TODO: add doc
 */
interface OnboardingIllustrationAdapter : InjectedViewAdapter, Parcelable {
    fun onVisible()
    fun onHidden()
}

/**
 * TODO: add doc
 */
@Parcelize
class ImageOnboardingIllustrationAdapter(
    @DrawableRes private val drawableRes: Int,
    @StringRes private val contentDescriptionRes: Int
) : OnboardingIllustrationAdapter {

    override fun onVisible() {}

    override fun onHidden() {}

    override fun getView(container: ViewGroup): View {
        return ImageView(container.context).apply {
            setImageDrawable(ContextCompat.getDrawable(container.context, drawableRes))
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            contentDescription = container.context.getString(contentDescriptionRes)
        }
    }

    override fun onDestroy() {}
}