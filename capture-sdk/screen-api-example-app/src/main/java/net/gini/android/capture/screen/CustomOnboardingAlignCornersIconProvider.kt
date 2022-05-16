package net.gini.android.capture.screen

import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.onboarding.view.OnboardingIconProvider
import net.gini.android.capture.screen.databinding.AnimationOnboardingLottieBinding

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class CustomOnboardingAlignCornersIconProvider() : OnboardingIconProvider {

    private var viewBinding: AnimationOnboardingLottieBinding? = null

    constructor(parcel: Parcel) : this()

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

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomOnboardingAlignCornersIconProvider> {
        override fun createFromParcel(parcel: Parcel): CustomOnboardingAlignCornersIconProvider {
            return CustomOnboardingAlignCornersIconProvider(parcel)
        }

        override fun newArray(size: Int): Array<CustomOnboardingAlignCornersIconProvider?> {
            return arrayOfNulls(size)
        }
    }

}