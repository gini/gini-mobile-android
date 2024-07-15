package net.gini.android.health.sdk.exampleapp.configuration

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import net.gini.android.health.sdk.exampleapp.MainViewModel
import net.gini.android.health.sdk.exampleapp.databinding.FragmentConfigurationBinding
import net.gini.android.health.sdk.exampleapp.review.ReviewViewModel
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentConfiguration

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
        ghsHidePoweredBy.isChecked = !(viewModel.getPaymentComponentConfiguration()?.isPaymentComponentBranded ?: true)

        ghsHidePoweredBy.setOnCheckedChangeListener { _, newValue ->
            viewModel.setPaymentComponentConfiguration(PaymentComponentConfiguration(isPaymentComponentBranded = !newValue))
        }
    }

    companion object {
        fun newInstance() = ConfigurationFragment()
    }
}
