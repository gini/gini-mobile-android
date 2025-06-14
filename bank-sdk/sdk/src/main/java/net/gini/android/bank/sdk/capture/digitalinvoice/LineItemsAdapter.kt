package net.gini.android.bank.sdk.capture.digitalinvoice

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
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
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
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
    private val context: Context,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<ViewHolder<*>>() {

    private val amountFormatter: AmountFormatter by getGiniBankKoin().inject()
    private val analyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }

    var lineItems: List<SelectableLineItem> = emptyList()
        set(value) {
            field = value
        }

    var addons: List<DigitalInvoiceAddon> = emptyList()
        set(value) {
            field = value
        }

    var skontoDiscount: List<DigitalInvoiceSkontoListItem> = emptyList()
        set(value) {
                recyclerView.post {
                    recyclerView.post {
                        val oldSize = field.size
                        field = value
                        val position = lineItems.size + addons.size
                        when {
                            oldSize == 0 && value.isNotEmpty() -> {
                                notifyItemInserted(position)
                            }
                            oldSize == 1 && value.isEmpty() -> {
                                notifyItemRemoved(position)
                            }
                            oldSize == 1 && value.size == 1 -> {
                                notifyItemChanged(position)
                            }
                            else -> {
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
        }

    var isInaccurateExtraction: Boolean = false

    private var footerDetails: DigitalInvoiceScreenContract.FooterDetails? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun updateLineItems(newItems: List<SelectableLineItem>) {
        val diffCallback = LineItemsDiffCallback(lineItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        lineItems = newItems
        diffResult.dispatchUpdatesTo(this)
    }


    class LineItemsDiffCallback(
        private val oldList: List<SelectableLineItem>,
        private val newList: List<SelectableLineItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition].lineItem
            val new = newList[newItemPosition].lineItem
            return old.id == new.id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewTypeId: Int): ViewHolder<*> {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder.forViewTypeId(
            viewTypeId,
            layoutInflater,
            parent,
            amountFormatter,
            analyticsEventTracker
        )
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
    internal class LineItemViewHolder(
        private val binding: GbsItemDigitalInvoiceLineItemBinding,
    ) :
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
            binding.gbsEditButton.setOnClickListener(
                IntervalClickListener {
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
            itemView.isEnabled = true
            binding.gbsEditButton.isEnabled = true

            binding.gbsEditButton.setTextColor(
                resolveColor(
                    net.gini.android.capture.R.color.gc_accent_01,
                    binding
                )
            )
            binding.gbsPerUnit.setTextColor(
                resolveColor(
                    net.gini.android.capture.R.color.gc_dark_05,
                    binding
                )
            )

            resolveAttrColor(
                R.attr.colorOnBackground,
                net.gini.android.capture.R.color.gc_light_05,
                binding
            ).let { color ->
                listOf(
                    binding.gbsDescription,
                    binding.gbsGrossPriceFractionalPart,
                    binding.gbsGrossPriceIntegralPart
                ).forEach { it.setTextColor(color) }
            }
        }

        private fun disable() {
            itemView.isEnabled = false
            binding.gbsEditButton.isEnabled = false

            val color = if (ContextHelper.isDarkTheme(binding.gbsEditButton.context)) {
                resolveColor(net.gini.android.capture.R.color.gc_light_05, binding)
            } else {
                resolveColor(net.gini.android.capture.R.color.gc_dark_05, binding)
            }

            listOf(
                binding.gbsEditButton,
                binding.gbsPerUnit,
                binding.gbsDescription,
                binding.gbsGrossPriceFractionalPart,
                binding.gbsGrossPriceIntegralPart
            ).forEach { it.setTextColor(color) }
        }

        /**
         * Separating the util functions to fetch colors
         * */

        private fun resolveAttrColor(
            @AttrRes attrRes: Int,
            fallbackColorRes: Int,
            binding: GbsItemDigitalInvoiceLineItemBinding
        ): Int {
            val context = binding.root.context
            val typedValue = TypedValue()
            return if (context.theme.resolveAttribute(attrRes, typedValue, true)) {
                typedValue.data
            } else {
                ContextCompat.getColor(context, fallbackColorRes)
            }
        }

        private fun resolveColor(
            @ColorRes colorRes: Int,
            binding: GbsItemDigitalInvoiceLineItemBinding
        ): Int {
            return ContextCompat.getColor(binding.root.context, colorRes)
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
        private val analyticsEventTracker: UserAnalyticsEventTracker?,
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
            setupSkontoAmount(data)
            // message should be visible if it is an edgeCase or if the skonto is disabled
            setupMessage(data)
            // Disable edit button when Skonto switch is disabled
            setupEditButton(data)
            setupEnableSwitch(data)
        }
        private fun GbsItemDigitalInvoiceSkontoBinding.setupEnableSwitch(data: DigitalInvoiceSkontoListItem) {
            gbsEnableSwitch.isChecked = data.enabled
            gbsEnableSwitch.setOnClickListener {
                analyticsEventTracker?.trackEvent(
                    UserAnalyticsEvent.SKONTO_SWITCH_TAPPED,
                    setOf(
                        UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.ReturnAssistant),
                        UserAnalyticsEventProperty.SwitchActive(gbsEnableSwitch.isChecked)
                    )
                )
                if (gbsEnableSwitch.isChecked) {
                    listener?.onSkontoEnabled(data)
                } else {
                    listener?.onSkontoDisabled(data)
                }
            }
        }
        @SuppressLint("SetTextI18n")
        private fun GbsItemDigitalInvoiceSkontoBinding.setupSkontoAmount(data: DigitalInvoiceSkontoListItem) {
            if (data.enabled) {
                gbsSkontoAmount.visibility = View.VISIBLE
                gbsSkontoAmount.text = "-${amountFormatter.format(data.savedAmount)}"
                gbsSkontoAmount.setTextColor(
                    ContextCompat.getColor(
                        gbsSkontoAmount.context,
                        if (ContextHelper.isDarkTheme(gbsSkontoAmount.context)) {
                            net.gini.android.capture.R.color.gc_success_02
                        } else {
                            net.gini.android.capture.R.color.gc_success_01
                        }
                    )
                )
            } else {
                gbsSkontoAmount.visibility = View.GONE
            }
        }

        private fun GbsItemDigitalInvoiceSkontoBinding.setupMessage(data: DigitalInvoiceSkontoListItem) {
            if (data.isEdgeCase || !data.enabled) {
                gbsMessage.visibility = View.VISIBLE
                gbsMessage.text = data.message
            } else {
                gbsMessage.visibility = View.GONE
            }
        }
        private fun GbsItemDigitalInvoiceSkontoBinding.setupEditButton(data: DigitalInvoiceSkontoListItem) {
            if (data.enabled) {
                gbsEditButton.isClickable = true
                gbsEditButton.focusable = View.FOCUSABLE
                gbsEditButton.setTextColor(
                    gbsEditButton.context.getColor(net.gini.android.capture.R.color.gc_accent_01)
                )
                gbsEditButton.setOnClickListener {
                    analyticsEventTracker?.trackEvent(
                        UserAnalyticsEvent.EDIT_TAPPED,
                        setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.ReturnAssistant))
                    )
                    listener?.onSkontoEditClicked(data)
                }
            } else {
                gbsEditButton.setTextColor(
                    gbsEditButton.context.getColor(net.gini.android.capture.R.color.gc_dark_05)
                )
                gbsEditButton.setOnClickListener(null)
                gbsEditButton.focusable = View.NOT_FOCUSABLE
                gbsEditButton.isClickable = false
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
            analyticsEventTracker: UserAnalyticsEventTracker?,
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
                    amountFormatter,
                    analyticsEventTracker
                )
            }
    }
}
