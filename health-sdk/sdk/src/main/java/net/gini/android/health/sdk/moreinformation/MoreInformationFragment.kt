package net.gini.android.health.sdk.moreinformation

import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsFragmentPaymentMoreInformationBinding
import net.gini.android.health.sdk.databinding.GhsPaymentProviderIconHolderBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthTheme


/**
 * Created by dani on 26/02/2024.
 */

class MoreInformationFragment private constructor(val paymentComponent: PaymentComponent?) : Fragment() {

    private var binding: GhsFragmentPaymentMoreInformationBinding by autoCleared()
    private val faqList: List<Pair<String, String>> by lazy {
        //mock strings for testing purposes - to be updated when we have wording
        listOf (
            getString(R.string.ghs_more_information) to getString(R.string.ghs_amount_hint),
            getString(R.string.ghs_error_document) to getString(R.string.ghs_snackbar_retry),
            getString(R.string.ghs_bank_placeholder) to getString(R.string.ghs_amount_hint),
            getString(R.string.ghs_error_input_iban_empty) to getString(R.string.ghs_select_bank),
            getString(R.string.ghs_error_input_recipient_empty) to getString(R.string.ghs_amount_hint),
        )
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniHealthTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GhsFragmentPaymentMoreInformationBinding.inflate(inflater, container, false)
        (requireActivity()).apply {
            title = getString(R.string.ghs_more_information)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ghsPaymentProvidersIconsList.adapter = PaymentProvidersIconsAdapter(listOf())
        binding.ghsFaqList.apply {
            setAdapter(FaqExpandableListAdapter(faqList))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            paymentComponent?.paymentProviderAppsFlow?.collect { paymentProviderAppsState ->
                when (paymentProviderAppsState) {
                    is PaymentProviderAppsState.Error -> {}
                    PaymentProviderAppsState.Loading -> {}
                    is PaymentProviderAppsState.Success -> updatePaymentProviderIconsAdapter(paymentProviderAppsState.paymentProviderApps)
                }
            }
        }
    }

    private fun updatePaymentProviderIconsAdapter(paymentProviderApps: List<PaymentProviderApp>) {
        (binding.ghsPaymentProvidersIconsList.adapter as PaymentProvidersIconsAdapter).apply {
            dataSet = paymentProviderApps.map { it.icon }
            notifyDataSetChanged()
        }
    }

    internal class PaymentProvidersIconsAdapter(var dataSet: List<Drawable?>): RecyclerView.Adapter<PaymentProvidersIconsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = GhsPaymentProviderIconHolderBinding.inflate(parent.getLayoutInflaterWithGiniHealthTheme(), parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = dataSet.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.ghsPaymentProviderIcon.setImageDrawable(dataSet[position])
        }

        class ViewHolder(val binding: GhsPaymentProviderIconHolderBinding): RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent?): MoreInformationFragment = MoreInformationFragment(paymentComponent = paymentComponent)
    }
}