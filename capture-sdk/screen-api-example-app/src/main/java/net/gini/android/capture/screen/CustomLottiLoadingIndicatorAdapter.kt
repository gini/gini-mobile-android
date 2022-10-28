package net.gini.android.capture.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import net.gini.android.capture.screen.databinding.AnimationOnboardingLottieBinding
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter

class CustomLottiLoadingIndicatorAdapter(@RawRes val animationRes: Int) : CustomLoadingIndicatorAdapter {

    private var viewBinding: AnimationOnboardingLottieBinding? = null

    override fun onVisible() {
        viewBinding?.animationView?.playAnimation()
    }

    override fun onHidden() {
        viewBinding?.animationView?.cancelAnimation()
        viewBinding?.animationView?.progress = 0f
    }

    override fun onCreateView(container: ViewGroup): View {

        val binding = AnimationOnboardingLottieBinding.inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        viewBinding?.animationView?.setAnimation(animationRes)

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }
}
