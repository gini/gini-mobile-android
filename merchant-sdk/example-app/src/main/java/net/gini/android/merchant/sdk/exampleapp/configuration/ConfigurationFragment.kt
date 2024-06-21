package net.gini.android.merchant.sdk.exampleapp.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.gini.android.merchant.sdk.exampleapp.MainViewModel
import net.gini.android.merchant.sdk.exampleapp.databinding.FragmentConfigurationBinding
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration


class ConfigurationFragment: Fragment() {

    private lateinit var binding: FragmentConfigurationBinding
    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfigurationBinding.inflate(layoutInflater)
        with(binding) {
            setupSwitchListeners()
        }
        return binding.root
    }

    private fun FragmentConfigurationBinding.setupSwitchListeners() {
        gmsAmountEditable.isEnabled = false
        gmsShowReviewFragment.setOnCheckedChangeListener { buttonView, isChecked ->
            gmsAmountEditable.isFocusable = isChecked
            gmsAmountEditable.isEnabled = isChecked
            if (!isChecked) {
                gmsAmountEditable.isChecked = false
            }
            saveConfiguration()
        }
        gmsAmountEditable.setOnCheckedChangeListener { _, _ ->
            saveConfiguration()
        }
    }

    private fun saveConfiguration() {
        viewModel.saveConfiguration(
            PaymentFlowConfiguration(
                shouldShowReviewFragment = binding.gmsShowReviewFragment.isChecked,
                isAmountFieldEditable = binding.gmsAmountEditable.isChecked,
                shouldHandleErrorsInternally = true
            )
        )
    }

    companion object {
        fun newInstance() = ConfigurationFragment()
    }
}