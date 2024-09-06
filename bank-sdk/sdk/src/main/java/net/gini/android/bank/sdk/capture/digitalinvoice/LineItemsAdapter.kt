package net.gini.android.bank.sdk.capture.digitalinvoice

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.ViewType.Addon
import net.gini.android.bank.sdk.capture.digitalinvoice.ViewType.LineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.ViewType.SkontoInfo
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceAddonBinding
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceLineItemBinding
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceSkontoBinding
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.capture.internal.ui.IntervalClickListener
import java.util.Collections.emptyList

/**
 * Created by Alpar Szotyori on 11.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
internal interface LineItemsAdapterListener {
    fun onLineItemClicked(lineItem: SelectableLineItem)
    fun onLineItemSelected(lineItem: SelectableLineItem)
    fun onLineItemDeselected(lineItem: SelectableLineItem)
    fun payButtonClicked()
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal interface SkontoListItemAdapterListener {
    fun onSkontoEditClicked(listItem: DigitalInvoiceSkontoListItem)
    fun onSkontoEnabled(listItem: DigitalInvoiceSkontoListItem)
    fun onSkontoDisabled(listItem: DigitalInvoiceSkontoListItem)
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal class LineItemsAdapter(
    private val listener: LineItemsAdapterListener,
    private val skontoListener: SkontoListItemAdapterListener,
    private val context: Context
) : RecyclerView.Adapter<ViewHolder<*>>() {

    private val amountFormatter: AmountFormatter by getGiniBankKoin().inject()

    var lineItems: List<SelectableLineItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var addons: List<DigitalInvoiceAddon> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var skontoDiscount: List<DigitalInvoiceSkontoListItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isInaccurateExtraction: Boolean = false

    private var footerDetails: DigitalInvoiceScreenContract.FooterDetails? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewTypeId: Int): ViewHolder<*> {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder.forViewTypeId(viewTypeId, layoutInflater, parent, amountFormatter)
    }


    override fun getItemCount(): Int =
        lineItems.size + addons.size + skontoDiscount.size

    override fun getItemViewType(position: Int): Int {
        val lineItemRange = lineItems.indices
        val addonRange = lineItemRange.last + 1..lineItemRange.last + addons.size
        val skontoRange = addonRange.last + 1..addonRange.last + skontoDiscount.size

        return when (position) {
            in lineItemRange -> LineItem.id
            in addonRange -> Addon.id
            in skontoRange -> SkontoInfo.id
            else -> error("Unknown view type at position $position")
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder<*>, position: Int) {
        when (viewHolder) {
            is ViewHolder.LineItemViewHolder -> {
                lineItems.getOrNull(position)?.let {
                    viewHolder.listener = listener
                    viewHolder.bind(it, lineItems, position)
                }
            }

            is ViewHolder.AddonViewHolder -> {
                val index = position - lineItems.size
                val enabled = footerDetails?.buttonEnabled ?: true
                addons.getOrNull(index)?.let {
                    viewHolder.bind(Pair(it, enabled), null)
                }

                // Adding padding for the last addon item, so the item looks full height without modifying the layout file
                val bottomPadding = if (position == (itemCount - 1)) context.resources.getDimension(
                    net.gini.android.capture.R.dimen.gc_large
                ).toInt() else 0
                viewHolder.itemView.setPadding(
                    context.resources.getDimension(net.gini.android.capture.R.dimen.gc_large)
                        .toInt(), 0, 0, bottomPadding
                )
            }

            is ViewHolder.SkontoViewHolder -> {
                skontoDiscount.getOrNull(viewHolder.bindingAdapterPosition - lineItems.size - skontoDiscount.size)
                    ?.let {
                        viewHolder.bind(it, null)
                        viewHolder.listener = skontoListener
                    }
            }
        }
    }

    override fun onViewRecycled(viewHolder: ViewHolder<*>) {
        viewHolder.unbind()
    }
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal sealed class ViewType {
    internal abstract val id: Int

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal object LineItem : ViewType() {
        override val id: Int = 1
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal object Addon : ViewType() {
        override val id: Int = 2
    }

    internal object SkontoInfo : ViewType() {
        override val id: Int = 3
    }

    internal companion object {

        @Suppress("MagicNumber")
        fun from(viewTypeId: Int): ViewType = when (viewTypeId) {
            1 -> LineItem
            2 -> Addon
            3 -> SkontoInfo
            else -> throw IllegalStateException("Unknow adapter view type id: $viewTypeId")
        }
    }
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal sealed class ViewHolder<in T>(itemView: View, val viewType: ViewType) :
    RecyclerView.ViewHolder(itemView) {

    internal abstract fun bind(data: T, allData: List<T>? = null, dataIndex: Int? = null)

    internal abstract fun unbind()

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal class LineItemViewHolder(private val binding: GbsItemDigitalInvoiceLineItemBinding) :
        ViewHolder<SelectableLineItem>(binding.root, LineItem) {
        internal var listener: LineItemsAdapterListener? = null

        override fun bind(
            data: SelectableLineItem,
            allData: List<SelectableLineItem>?,
            dataIndex: Int?
        ) {
            if (data.selected) {
                enable()
            } else {
                disable()
            }
            binding.gbsEnableSwitch.isChecked = data.selected

            binding.gbsEnableSwitch.isInvisible = data.addedByUser


            data.lineItem.let { li ->
                binding.gbsDescription.text = "${li.quantity}x ${li.description}"
                DigitalInvoice.lineItemTotalGrossPriceIntegralAndFractionalParts(li)
                    .let { (integral, fractional) ->
                        binding.gbsGrossPriceIntegralPart.text = integral
                        binding.gbsGrossPriceFractionalPart.text = fractional
                    }

                DigitalInvoice.lineItemUnitPriceIntegralAndFractionalParts(li)
                    .let { (integral, fractional) ->
                        binding.gbsPerUnit.text = binding.gbsPerUnit.resources.getString(
                            R.string.gbs_digital_invoice_line_item_quantity,
                            "$integral$fractional"
                        )
                    }
            }
            itemView.setOnClickListener(IntervalClickListener {
                allData?.getOrNull(dataIndex ?: -1)?.let {
                    listener?.onLineItemClicked(it)
                }
            })

            binding.gbsEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
                allData?.getOrNull(dataIndex ?: -1)?.let {
                    if (it.selected != isChecked) {
                        listener?.apply {
                            if (isChecked) {
                                onLineItemSelected(it)
                            } else {
                                onLineItemDeselected(it)
                            }
                        }
                    }
                }
            }
            if (dataIndex == (allData?.size?.minus(1))) {
                binding.gbsMaterialDivider.updateLayoutParams {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }

        override fun unbind() {
            listener = null
            itemView.setOnClickListener(null)
            binding.gbsEnableSwitch.setOnCheckedChangeListener(null)
        }

        private fun enable() {
            val alpha = 1.0f
            itemView.isEnabled = true
            binding.gbsEditButton.isEnabled = true
            binding.gbsEditButton.alpha = alpha
            binding.gbsDescription.alpha = alpha
            binding.gbsPerUnit.alpha = alpha
            binding.gbsGrossPriceFractionalPart.alpha = alpha
            binding.gbsGrossPriceIntegralPart.alpha = alpha
        }


        private fun disable() {
            val alpha = 0.5f
            itemView.isEnabled = false
            binding.gbsDescription.alpha = alpha
            binding.gbsEditButton.isEnabled = false
            binding.gbsEditButton.alpha = alpha
            binding.gbsPerUnit.alpha = alpha
            binding.gbsGrossPriceIntegralPart.alpha = alpha
            binding.gbsGrossPriceFractionalPart.alpha = alpha
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal class AddonViewHolder(binding: GbsItemDigitalInvoiceAddonBinding) :
        ViewHolder<Pair<DigitalInvoiceAddon, Boolean>>(binding.root, Addon) {
        private val addonName = binding.gbsAddonName
        private val priceIntegralPart: TextView = binding.gbsAddonPriceTotalIntegralPart
        private val fractionalPart: TextView = binding.gbsAddonPriceTotalFractionalPart

        override fun bind(
            data: Pair<DigitalInvoiceAddon, Boolean>,
            allData: List<Pair<DigitalInvoiceAddon, Boolean>>?,
            dataIndex: Int?
        ) {
            @SuppressLint("SetTextI18n")
            addonName.text = "${itemView.context.getString(data.first.nameStringRes)}"
            DigitalInvoice.addonPriceIntegralAndFractionalParts(data.first)
                .let { (integral, fractional) ->
                    priceIntegralPart.text = integral
                    fractionalPart.text = fractional
                }
        }

        override fun unbind() {
        }
    }

    internal class SkontoViewHolder(
        private val binding: GbsItemDigitalInvoiceSkontoBinding,
        private val amountFormatter: AmountFormatter,
    ) :
        ViewHolder<DigitalInvoiceSkontoListItem>(binding.root, SkontoInfo) {

        internal var listener: SkontoListItemAdapterListener? = null

        @SuppressLint("SetTextI18n")
        override fun bind(
            data: DigitalInvoiceSkontoListItem,
            allData: List<DigitalInvoiceSkontoListItem>?,
            dataIndex: Int?
        ) = with(binding) {
            // amount should be visible if the skonto is enabled
            if (data.enabled) {
                gbsSkontoAmount.visibility = View.VISIBLE
                gbsSkontoAmount.text = "-${amountFormatter.format(data.savedAmount)}"
            } else {
                gbsSkontoAmount.visibility = View.GONE
            }

            // message should be visible if it is an edgeCase or if the skonto is disabled
            if (data.isEdgeCase || !data.enabled) {
                gbsMessage.visibility = View.VISIBLE
                gbsMessage.text = data.message
            } else {
                gbsMessage.visibility = View.GONE
            }

            gbsEnableSwitch.isChecked = data.enabled
            gbsEditButton.setOnClickListener {
                listener?.onSkontoEditClicked(data)
            }
            gbsEnableSwitch.setOnClickListener {
                if (gbsEnableSwitch.isChecked) {
                    listener?.onSkontoEnabled(data)
                } else {
                    listener?.onSkontoDisabled(data)
                }
            }
        }

        override fun unbind() {
            listener = null
        }
    }

    companion object {
        fun forViewTypeId(
            viewTypeId: Int, layoutInflater: LayoutInflater, parent: ViewGroup,
            amountFormatter: AmountFormatter,
        ) =
            when (ViewType.from(viewTypeId)) {
                LineItem -> LineItemViewHolder(
                    GbsItemDigitalInvoiceLineItemBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )

                Addon -> AddonViewHolder(
                    GbsItemDigitalInvoiceAddonBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )

                SkontoInfo -> SkontoViewHolder(
                    GbsItemDigitalInvoiceSkontoBinding.inflate(
                        layoutInflater,
                        parent,
                        false,
                    ),
                    amountFormatter
                )
            }
    }
}
