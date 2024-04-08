package net.gini.android.health.sdk.bankselection

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBottomSheetBankSelectionBinding
import net.gini.android.health.sdk.databinding.GhsItemPaymentProviderAppBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthTheme
import net.gini.android.health.sdk.util.setIntervalClickListener
import net.gini.android.health.sdk.util.setIntervalClickListener
import net.gini.android.health.sdk.util.wrappedWithGiniHealthTheme
import org.slf4j.LoggerFactory

/**
 * The [BankSelectionBottomSheet] displays a list of available banks for the user to choose from. If a banking app is not
 * installed it will also display its Play Store link.
 */
class BankSelectionBottomSheet private constructor(private val paymentComponent: PaymentComponent?) :
    BottomSheetDialogFragment() {

    constructor() : this(null)

    private var binding: GhsBottomSheetBankSelectionBinding by autoCleared()
    private val viewModel: BankSelectionViewModel by viewModels { BankSelectionViewModel.Factory(paymentComponent) }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniHealthTheme(inflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val wrappedContext = requireContext().wrappedWithGiniHealthTheme()
        val dialog = BottomSheetDialog(wrappedContext, theme)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(wrappedContext, R.color.ghs_bottom_sheet_scrim))
        colorDrawable.alpha = 102 // 40% alpha
        dialog.window?.setBackgroundDrawable(colorDrawable)

        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = STATE_EXPANDED

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = GhsBottomSheetBankSelectionBinding.inflate(inflater, container, false)

        binding.ghsPaymentProviderAppsList.layoutManager = LinearLayoutManager(requireContext())
        binding.ghsPaymentProviderAppsList.adapter =
            PaymentProviderAppsAdapter(emptyList(), object : PaymentProviderAppsAdapter.OnItemClickListener {
                override fun onItemClick(paymentProviderApp: PaymentProviderApp) {
                    LOG.debug("Selected payment provider app: {}", paymentProviderApp.name)

                    if (paymentProviderApp.isInstalled()) {
                        LOG.debug("Changing selected payment provider app in PaymentComponent")
                        viewModel.setSelectedPaymentProviderApp(paymentProviderApp)
                        this@BankSelectionBottomSheet.dismiss()
                    }
                    //TODO remove commented code when we change behavior
                    else if (paymentProviderApp.hasPlayStoreUrl()) {
                        paymentProviderApp.paymentProvider.playStoreUrl?.let {
                            LOG.debug("Opening payment provider app in Play Store")
                            openPlayStoreUrl(it)
                        }
                    } else {
                        LOG.error("No installed payment provider app and no Play Store URL")
                    }
                }
            })

        binding.ghsCloseButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    private fun openPlayStoreUrl(playStoreUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentProviderAppsListFlow.collect { paymentProviderAppsListState ->
                    when (paymentProviderAppsListState) {
                        is PaymentProviderAppsListState.Error -> {
                            dismiss()
                        }

                        PaymentProviderAppsListState.Loading -> {}

                        is PaymentProviderAppsListState.Success -> {
                            (binding.ghsPaymentProviderAppsList.adapter as PaymentProviderAppsAdapter).apply {
                                dataSet = paymentProviderAppsListState.paymentProviderAppsList
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.recheckWhichPaymentProviderAppsAreInstalled()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BankSelectionBottomSheet::class.java)

        /**
         * Create a new instance of the [BankSelectionBottomSheet].
         *
         * @param paymentComponent the [PaymentComponent] which contains the list of payment provider apps and handles the
         * payment provider app selection
         */
        fun newInstance(paymentComponent: PaymentComponent): BankSelectionBottomSheet {
            return BankSelectionBottomSheet(paymentComponent)
        }
    }
}

internal class PaymentProviderAppsAdapter(
    var dataSet: List<PaymentProviderAppListItem>,
    val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<PaymentProviderAppsAdapter.ViewHolder>() {

    class ViewHolder(binding: GhsItemPaymentProviderAppBinding, onClickListener: OnClickListener) : RecyclerView.ViewHolder(binding.root) {
        val button: Button
        val iconView: ShapeableImageView

        init {
            iconView = binding.ghsSelectorLayout.ghsPaymentProviderAppIconHolder.ghsPaymentProviderIcon
            button = binding.ghsSelectorLayout.ghsSelectBankButton
            button.setIntervalClickListener { onClickListener.onClick(adapterPosition) }
        }

        interface OnClickListener {
            fun onClick(adapterPosition: Int)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = GhsItemPaymentProviderAppBinding.inflate(parent.getLayoutInflaterWithGiniHealthTheme(), parent, false)
        val viewHolder = ViewHolder(view, object : ViewHolder.OnClickListener {
            override fun onClick(adapterPosition: Int) {
                onItemClickListener.onItemClick(dataSet[adapterPosition].paymentProviderApp)
            }
        })
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paymentProviderAppListItem = dataSet[position]
        holder.itemView.context.wrappedWithGiniHealthTheme().let { context ->
            holder.button.text = paymentProviderAppListItem.paymentProviderApp.name
            holder.iconView.setImageDrawable(paymentProviderAppListItem.paymentProviderApp.icon)
            holder.itemView.isSelected = paymentProviderAppListItem.isSelected
            holder.button.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                if (paymentProviderAppListItem.isSelected) ContextCompat.getDrawable(context, R.drawable.ghs_checkmark) else null,
                null
            )
        }
    }

    override fun getItemCount() = dataSet.size

    interface OnItemClickListener {
        fun onItemClick(paymentProviderApp: PaymentProviderApp)
    }
}