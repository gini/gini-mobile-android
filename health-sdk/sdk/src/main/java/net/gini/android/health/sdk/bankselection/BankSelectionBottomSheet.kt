package net.gini.android.health.sdk.bankselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBottomSheetBankSelectionBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.autoCleared
import org.slf4j.LoggerFactory

class BankSelectionBottomSheet private constructor(private val paymentComponent: PaymentComponent?) :
    BottomSheetDialogFragment() {

    constructor() : this(null)

    private var binding: GhsBottomSheetBankSelectionBinding by autoCleared()
    private val viewModel: BankSelectionViewModel by viewModels { BankSelectionViewModel.Factory(paymentComponent) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = GhsBottomSheetBankSelectionBinding.inflate(inflater, container, false)

        binding.ghsPaymentProviderAppsList.layoutManager = LinearLayoutManager(requireContext())
        binding.ghsPaymentProviderAppsList.adapter = PaymentProviderAppsAdapter(emptyList(), object : PaymentProviderAppsAdapter.OnItemClickListener {
            override fun onItemClick(paymentProviderApp: PaymentProviderApp) {
                LOG.debug("Selected payment provider app: {}", paymentProviderApp.name)
                viewModel.paymentComponent?.setSelectedPaymentProviderApp(paymentProviderApp)
                this@BankSelectionBottomSheet.dismiss()
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    if (viewModel.paymentComponent == null) {
                        LOG.warn("Cannot show payment provider apps: PaymentComponent must be set before showing the BankSelectionBottomSheet")
                        return@launch
                    }
                    LOG.debug("Collecting payment provider apps state from PaymentComponent")
                    viewModel.paymentComponent?.paymentProviderAppsFlow?.collect { paymentProviderAppsState ->
                        LOG.debug("Received payment provider apps state: {}", paymentProviderAppsState)
                        when (paymentProviderAppsState) {
                            is PaymentProviderAppsState.Error -> {
                                LOG.error("Error loading payment provider apps", paymentProviderAppsState.throwable)
                                // TODO
                            }

                            PaymentProviderAppsState.Loading -> {
                                LOG.debug("Loading payment provider apps")
                                // TODO
                            }

                            is PaymentProviderAppsState.Success -> {
                                if (paymentProviderAppsState.paymentProviderApps.isNotEmpty()) {
                                    LOG.debug(
                                        "Received {} payment provider apps",
                                        paymentProviderAppsState.paymentProviderApps.size
                                    )
                                    (binding.ghsPaymentProviderAppsList.adapter as PaymentProviderAppsAdapter).apply {
                                        dataSet = paymentProviderAppsState.paymentProviderApps
                                        notifyDataSetChanged()
                                    }
                                } else {
                                    LOG.debug("No payment provider apps received")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BankSelectionBottomSheet::class.java)

        fun newInstance(paymentComponent: PaymentComponent): BankSelectionBottomSheet {
            return BankSelectionBottomSheet(paymentComponent)
        }
    }
}

class PaymentProviderAppsAdapter(
    var dataSet: List<PaymentProviderApp>,
    val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<PaymentProviderAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View, onClickListener: OnClickListener) : RecyclerView.ViewHolder(view) {
        val name: TextView

        init {
            name = view.findViewById(R.id.ghs_name)
            view.setOnClickListener { onClickListener.onClick(adapterPosition) }
        }

        interface OnClickListener {
            fun onClick(adapterPosition: Int)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ghs_item_payment_provider_app, parent, false)
        val viewHolder = ViewHolder(view, object: ViewHolder.OnClickListener {
            override fun onClick(adapterPosition: Int) {
                onItemClickListener.onItemClick(dataSet[adapterPosition])
            }
        })
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = dataSet[position].name
    }

    override fun getItemCount() = dataSet.size

    interface OnItemClickListener {
        fun onItemClick(paymentProviderApp: PaymentProviderApp)
    }
}