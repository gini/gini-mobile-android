package net.gini.android.capture.onboarding.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcOnboardingNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Created by Alpár Szotyori on 13.05.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Adapter for injecting a custom bottom navigation bar on the onboarding screen.
 */
interface OnboardingNavigationBarBottomAdapter : InjectedViewAdapter {
    /**
     * Set the click listener for the skip button.
     *
     * @param listener the click listener for the button
     */
    fun setOnSkipButtonClickListener(listener: View.OnClickListener?)

    /**
     * Set the click listener for the next button.
     *
     * @param listener the click listener for the button
     */
    fun setOnNextButtonClickListener(listener: View.OnClickListener?)

    /**
     * Set the click listener for the "get started" button.
     *
     * @param listener the click listener for the button
     */
    fun setOnGetStartedButtonClickListener(listener: View.OnClickListener?)

    /**
     * Called when the displayed buttons have to change. Show only the buttons that are in the list.
     *
     * @param buttons list of the buttons that have to be shown
     */
    fun showButtons(vararg buttons: OnboardingNavigationBarBottomButton)
}

/**
 * Buttons that can be shown on the onboarding screen's bottom navigation bar.
 */
enum class OnboardingNavigationBarBottomButton {
    SKIP,
    NEXT,
    GET_STARTED
}

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

    override fun onCreateView(container: ViewGroup): View {

        val binding = GcOnboardingNavigationBarBottomBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        handleSkipButtonMultipleLines()

        return binding.root
    }

    //Wait for view to be inflated
    //Check how many lines
    private fun handleSkipButtonMultipleLines() {
        viewBinding?.gcSkip?.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                v.removeOnLayoutChangeListener(this)
                val buttonSkip = viewBinding?.gcSkip
                if (buttonSkip != null && buttonSkip.lineCount >= 2) {
                    buttonSkip.text = buttonSkip.context.getString(R.string.gc_skip_two_lines)
                }
            }
        })
    }

    override fun onDestroy() {
        viewBinding = null
    }

}