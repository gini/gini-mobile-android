package net.gini.android.health.sdk.exampleapp.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.gini.android.health.sdk.exampleapp.MainViewModel
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.FragmentConfigurationBinding
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.internal.payment.utils.GiniLocalization

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
            setupSliderListener()
        }

        val languages = arrayOf(GiniLocalization.GERMAN, GiniLocalization.ENGLISH)
        context?.let { context ->
            val languageAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, languages)
            binding.languageSpinner.apply {
                adapter = languageAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        languageAdapter.getItem(p2)?.let { language ->
                            viewModel.setGiniHealthLanguage(language, context)
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }
                }
                setSelection(languageAdapter.getPosition(viewModel.getGiniHealthLanguage(context)))
            }
        }

        return binding.root
    }

    private fun FragmentConfigurationBinding.setupSwitchListeners() {
        ghsShowReviewDialog.isChecked = viewModel.getPaymentFlowConfiguration()?.shouldShowReviewBottomDialog ?: false
        ghsShowReviewDialog.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePaymentFlowConfiguration {
                copy(shouldShowReviewBottomDialog = isChecked)
            }
        }
    }

    private fun FragmentConfigurationBinding.setupSliderListener() {
        sliderPopupDuration.value =
            (viewModel.getPaymentFlowConfiguration()?.popupDurationPaymentReview ?: PaymentFlowConfiguration.DEFAULT_POPUP_DURATION).toFloat()
        tvPopupDurationValue.text = getString(
            R.string.popup_duration_value,
            viewModel.getPaymentFlowConfiguration()?.popupDurationPaymentReview ?: PaymentFlowConfiguration.DEFAULT_POPUP_DURATION
        )

        sliderPopupDuration.addOnChangeListener { _, value, _ ->
            val newValue = value.toInt()
            viewModel.updatePaymentFlowConfiguration {
                copy(popupDurationPaymentReview = newValue)
            }
            tvPopupDurationValue.text = getString(R.string.popup_duration_value, newValue)
        }
    }

    companion object {
        fun newInstance() = ConfigurationFragment()
    }
}
