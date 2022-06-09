package net.gini.android.capture.onboarding.view

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

interface OnboardingIllustrationAdapter : InjectedViewAdapter, Parcelable {
    fun onVisible()
    fun onHidden()
}

internal open class DefaultOnboardingIllustrationAdapter(@DrawableRes private val icon: Int) : OnboardingIllustrationAdapter {

    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun onVisible() {
    }

    override fun onHidden() {

    }

    override fun getView(container: ViewGroup): View {
        return ImageView(container.context).apply {
            setImageDrawable(ContextCompat.getDrawable(container.context, icon))
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onDestroy() {

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(icon)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DefaultOnboardingIllustrationAdapter> {
        override fun createFromParcel(parcel: Parcel): DefaultOnboardingIllustrationAdapter {
            return DefaultOnboardingIllustrationAdapter(parcel)
        }

        override fun newArray(size: Int): Array<DefaultOnboardingIllustrationAdapter?> {
            return arrayOfNulls(size)
        }
    }

}