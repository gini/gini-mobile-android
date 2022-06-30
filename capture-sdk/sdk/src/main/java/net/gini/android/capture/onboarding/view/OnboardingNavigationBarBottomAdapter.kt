package net.gini.android.capture.onboarding.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcOnboardingNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

interface OnboardingNavigationBarBottomAdapter : InjectedViewAdapter {
    fun setOnSkipButtonClickListener(listener: View.OnClickListener?)
    fun setOnNextButtonClickListener(listener: View.OnClickListener?)
    fun setOnGetStartedButtonClickListener(listener: View.OnClickListener?)
    fun showButtons(vararg buttons: OnboardingNavigationBarBottomButton)
}

enum class OnboardingNavigationBarBottomButton {
    SKIP,
    NEXT,
    GET_STARTED
}

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
internal class DefaultOnboardingNavigationBarBottomAdapter : OnboardingNavigationBarBottomAdapter {

    var viewBinding: GcOnboardingNavigationBarBottomBinding? = null

    override fun setOnSkipButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcSkip?.setOnClickListener(listener)
    }

    override fun setOnNextButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcNext?.setOnClickListener(listener)
    }

    override fun setOnGetStartedButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcGetStarted?.setOnClickListener(listener)
    }

    override fun showButtons(vararg buttons: OnboardingNavigationBarBottomButton) {
        viewBinding?.gcSkip?.visibility = View.INVISIBLE
        viewBinding?.gcNext?.visibility = View.INVISIBLE
        viewBinding?.gcGetStarted?.visibility = View.INVISIBLE

        for (button in buttons) {
            when (button) {
                OnboardingNavigationBarBottomButton.SKIP -> viewBinding?.gcSkip?.visibility = View.VISIBLE
                OnboardingNavigationBarBottomButton.NEXT -> viewBinding?.gcNext?.visibility = View.VISIBLE
                OnboardingNavigationBarBottomButton.GET_STARTED -> viewBinding?.gcGetStarted?.visibility = View.VISIBLE
            }
        }
    }

    override fun getView(container: ViewGroup): View {
        viewBinding?.let { return it.root }

        val binding = GcOnboardingNavigationBarBottomBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return binding.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}