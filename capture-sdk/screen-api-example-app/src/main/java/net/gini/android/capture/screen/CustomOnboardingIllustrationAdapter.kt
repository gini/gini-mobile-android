package net.gini.android.capture.screen

import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import net.gini.android.capture.screen.databinding.AnimationOnboardingLottieBinding

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class CustomOnboardingIllustrationAdapter(@RawRes val animationRes: Int) : OnboardingIllustrationAdapter {

    private var viewBinding: AnimationOnboardingLottieBinding? = null

    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun onVisible() {
        viewBinding?.animationView?.playAnimation()
    }

    override fun onHidden() {
        viewBinding?.animationView?.cancelAnimation()
        viewBinding?.animationView?.progress = 0f
    }

    override fun getView(container: ViewGroup): View {
        viewBinding?.let { return it.root }

        val binding = AnimationOnboardingLottieBinding.inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        viewBinding?.animationView?.setAnimation(animationRes)

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(animationRes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomOnboardingIllustrationAdapter> {
        override fun createFromParcel(parcel: Parcel): CustomOnboardingIllustrationAdapter {
            return CustomOnboardingIllustrationAdapter(parcel)
        }

        override fun newArray(size: Int): Array<CustomOnboardingIllustrationAdapter?> {
            return arrayOfNulls(size)
        }
    }

}