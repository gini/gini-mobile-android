package net.gini.android.bank.screen.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.screen.databinding.CustomOnboardingNavigationBarBottomBinding
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomButton

class CustomOnboardingNavigationBarBottomAdapter : OnboardingNavigationBarBottomAdapter {

    var viewBinding: CustomOnboardingNavigationBarBottomBinding? = null

    override fun setOnSkipButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.buttonSkip?.setOnClickListener(listener)
    }

    override fun setOnNextButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.buttonNext?.setOnClickListener(listener)
    }

    override fun setOnGetStartedButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.buttonGetStarted?.setOnClickListener(listener)
    }

    override fun showButtons(vararg buttons: OnboardingNavigationBarBottomButton) {
        viewBinding?.buttonSkip?.visibility = View.INVISIBLE
        viewBinding?.buttonNext?.visibility = View.INVISIBLE
        viewBinding?.buttonGetStarted?.visibility = View.INVISIBLE

        for (button in buttons) {
            when (button) {
                OnboardingNavigationBarBottomButton.SKIP -> viewBinding?.buttonSkip?.visibility = View.VISIBLE
                OnboardingNavigationBarBottomButton.NEXT -> viewBinding?.buttonNext?.visibility = View.VISIBLE
                OnboardingNavigationBarBottomButton.GET_STARTED -> viewBinding?.buttonGetStarted?.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(container: ViewGroup): View {

        val binding = CustomOnboardingNavigationBarBottomBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        handleSkipButtonMultipleLines()

        return binding.root
    }

    //Wait for view to be inflated
    //Check how many lines
    private fun handleSkipButtonMultipleLines() {
        viewBinding?.buttonSkip?.post {
            val buttonSkip = viewBinding?.buttonSkip
            when (buttonSkip?.lineCount) {
                2 -> buttonSkip.text = buttonSkip.context.getString(R.string.gc_skip_two_lines)
            }
        }
    }

    override fun onDestroy() {
        viewBinding = null
    }

}