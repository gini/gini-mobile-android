package net.gini.android.health.sdk.exampleapp.configuration

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import net.gini.android.health.sdk.exampleapp.MainViewModel
import net.gini.android.health.sdk.exampleapp.databinding.FragmentConfigurationBinding
import net.gini.android.health.sdk.exampleapp.review.ReviewViewModel
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentConfiguration
import net.gini.android.health.sdk.util.GiniLocalization
import org.koin.android.ext.android.bind

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

        val languages = arrayOf(GiniLocalization.GERMAN, GiniLocalization.ENGLISH)
        context?.let {
            val languageAdapter = ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, languages)
            binding.languageSpinner.apply {
                adapter = languageAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        languageAdapter.getItem(p2)?.let {
                            viewModel.setGiniHealthLanguage(it)
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }
                }
                setSelection(languageAdapter.getPosition(viewModel.getGiniHealthLanguage()))
            }
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
