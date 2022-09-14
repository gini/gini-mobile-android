package net.gini.android.bank.sdk.capture.digitalinvoice

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.bank.sdk.R
import java.util.Collections.emptyList
import net.gini.android.bank.sdk.capture.digitalinvoice.ViewType.*
import net.gini.android.bank.sdk.capture.digitalinvoice.ViewType.LineItem
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceAddonBinding
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceFooterBinding
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceHeaderBinding
import net.gini.android.bank.sdk.databinding.GbsItemDigitalInvoiceLineItemBinding

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
    fun removeLineItem(lineItem: SelectableLineItem)
    fun onWhatIsThisButtonClicked()
    fun payButtonClicked()
    fun skipButtonClicked()
    fun addNewArticle()
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal class LineItemsAdapter(private val listener: LineItemsAdapterListener) :
    RecyclerView.Adapter<ViewHolder<*>>() {


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
    var isInaccurateExtraction: Boolean = false

    var footerDetails: DigitalInvoiceScreenContract.FooterDetails? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val footerButtonClickListener = View.OnClickListener {
        listener.payButtonClicked()
    }
    private val footerSkipButtonClickListener = View.OnClickListener {
        listener.skipButtonClicked()
    }

    private val footerAddButtonClickListener = View.OnClickListener {
        listener.addNewArticle()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewTypeId: Int): ViewHolder<*> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder =
            ViewHolder.forViewTypeId(viewTypeId, layoutInflater, parent)
        if (viewHolder is ViewHolder.FooterViewHolder) {
            viewHolder.binding.payButton.setOnClickListener(footerButtonClickListener)
            viewHolder.binding.skipButton.setOnClickListener(footerSkipButtonClickListener)
            viewHolder.binding.addButton.setOnClickListener(footerAddButtonClickListener)
        }

        if (viewHolder is ViewHolder.HeaderViewHolder) {
            viewHolder.binding.headerButton2.setOnClickListener(footerSkipButtonClickListener)
        }

        return viewHolder
    }


    override fun getItemCount(): Int =
        lineItems.size + addons.size + if (isInaccurateExtraction) 2 else 1

    private fun footerPosition() =
        lineItems.size + addons.size + if (isInaccurateExtraction) 1 else 0

    private fun addonsRange() =
        (lineItems.size + if (isInaccurateExtraction) 1 else 0)..(lineItems.size + addons.size + if (isInaccurateExtraction) 1 else 0)

    override fun getItemViewType(position: Int): Int {
       return when (position) {
            0 -> if (isInaccurateExtraction) Header.id else LineItem.id
            footerPosition() -> Footer.id
            in addonsRange() -> Addon.id
            else -> LineItem.id
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder<*>, position: Int) {
        when (viewHolder) {
            is ViewHolder.HeaderViewHolder -> {
                viewHolder.listener = listener
                viewHolder.bind(footerDetails?.buttonEnabled ?: false)
            }
            is ViewHolder.LineItemViewHolder -> {
                val index = if (isInaccurateExtraction) position - 1 else position
                lineItems.getOrNull(index)?.let {
                    viewHolder.listener = listener
                    viewHolder.bind(it, lineItems, index)
                }
            }
            is ViewHolder.AddonViewHolder -> {
                val index = if (isInaccurateExtraction) position - 1 - lineItems.size else position - lineItems.size
                val enabled = footerDetails?.buttonEnabled ?: true
                addons.getOrNull(index)?.let {
                    viewHolder.bind(Pair(it, enabled), null)
                }
            }
            is ViewHolder.FooterViewHolder -> {
                footerDetails?.also {
                    viewHolder.bind(it)
                }
            }
        }
    }

    override fun onViewRecycled(viewHolder: ViewHolder<*>) {
        viewHolder.unbind()
    }
}

@JvmSynthetic
internal fun addonForPosition(
    position: Int,
    addons: List<DigitalInvoiceAddon>,
    lineItems: List<SelectableLineItem>
): DigitalInvoiceAddon? =
    addons.getOrNull(position - lineItems.size - 1)

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
    internal object Header : ViewType() {
        override val id: Int = 1
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal object LineItem : ViewType() {
        override val id: Int = 2
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal object Addon : ViewType() {
        override val id: Int = 3
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal object Footer : ViewType() {
        override val id: Int = 4
    }

    internal companion object {
        fun from(viewTypeId: Int): ViewType = when (viewTypeId) {
            1 -> Header
            2 -> LineItem
            3 -> Addon
            4 -> Footer
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
    internal class HeaderViewHolder(
        val binding: GbsItemDigitalInvoiceHeaderBinding
    ) :
        ViewHolder<Boolean>(binding.root, Header) {
        internal var listener: LineItemsAdapterListener? = null
        private val collapsedHeight =
            binding.root.resources.getDimensionPixelSize(R.dimen.gbs_digital_invoice_header_collapsed_height)
        private val collapsedWidth =
            binding.root.resources.getDimensionPixelSize(R.dimen.gbs_digital_invoice_header_collapsed_width)
        private val collapsedMarginRight =
            binding.root.resources.getDimensionPixelSize(R.dimen.gbs_digital_invoice_header_title_collapsed_margin)

        private val collapsedCardRadius =
            binding.root.resources.getDimensionPixelSize(R.dimen.gbs_digital_invoice_header_corners_collapsed)
                .toFloat()

        private val expandedCardRadius =
            binding.root.resources.getDimensionPixelSize(R.dimen.gbs_digital_invoice_header_corners_expanded)
                .toFloat()

        private var expandedHeight: Int = -1
        private var expandedWidth: Int = -1

        private var animatorSet: AnimatorSet? = null

        private val toggleClickListener = View.OnClickListener {
            animateView()
        }

        override fun bind(data: Boolean, allData: List<Boolean>?, dataIndex: Int?) {
            binding.headerButton2.isEnabled = data
            binding.collapseButton.setOnClickListener(toggleClickListener)
            binding.headerButton1.setOnClickListener(toggleClickListener)
            binding.headerTitle.setOnClickListener(toggleClickListener)
        }

        override fun unbind() {
        }

        private fun animateView() {
            animatorSet?.cancel()
            if (expandedHeight == -1) {
                expandedHeight = binding.headerBackgroundView.height
                expandedWidth = binding.headerBackgroundView.width
            }

            val isExpandingAnimation = binding.headerBackgroundView.height <= collapsedHeight
            val wDiff = (expandedWidth - collapsedWidth).toFloat()
            val hDiff = (expandedHeight - collapsedHeight).toFloat()
            val cornerDiff = collapsedCardRadius - expandedCardRadius

            val animator = ValueAnimator.ofFloat(0f, 1f)
                .setDuration(300)

            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    if (!isExpandingAnimation) {
                        binding.headerText1.isVisible = false
                        binding.headerText2.isVisible = false
                        binding.headerImage.isVisible = false
                        binding.containerButtons.isVisible = false
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (isExpandingAnimation) {
                        binding.headerText1.isVisible = true
                        binding.headerText2.isVisible = true
                        binding.headerImage.isVisible = true
                        binding.containerButtons.isVisible = true
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            })

            animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                override fun onAnimationUpdate(animation: ValueAnimator) {
                    val updateVal = (animation?.animatedValue as? Float)?.let {
                        if (!isExpandingAnimation) 1 - it else it
                    } ?: return

                    val backgroundLp = binding.headerBackgroundView.layoutParams
                    val containerLp = binding.headerContent.layoutParams

                    backgroundLp.width = collapsedWidth + (updateVal * wDiff).toInt()
                    containerLp.width = collapsedWidth + (updateVal * wDiff).toInt()
                    backgroundLp.height = collapsedHeight + (updateVal * hDiff).toInt()
                    containerLp.height = collapsedHeight + (updateVal * hDiff).toInt()
                    binding.headerTitle.updatePadding(right = ((1f - updateVal) * collapsedMarginRight).toInt())

                    binding.collapseButton.alpha = 0.7f + updateVal * 0.3f
                    binding.collapseButton.rotation = 180f - updateVal * 180f
                    binding.collapseButton.alpha = 0.7f + updateVal * 0.3f
                    binding.collapseButton.scaleX = 0.8f + updateVal * 0.2f
                    binding.collapseButton.scaleY = 0.8f + updateVal * 0.2f

                    binding.headerTitle.alpha = 0.7f + updateVal * 0.3f
                    binding.headerTitle.scaleX = 1.25f - updateVal * 0.25f
                    binding.headerTitle.scaleY = 1.25f - updateVal * 0.25f

                    binding.headerBackgroundView.radius =
                        collapsedCardRadius - updateVal * cornerDiff

                    binding.headerBackgroundView.layoutParams = backgroundLp
                    binding.headerContent.layoutParams = containerLp
                }
            })

            animatorSet = AnimatorSet()
            animatorSet?.interpolator = AccelerateDecelerateInterpolator()
            animatorSet?.play(animator)
            animatorSet?.start()

        }
    }

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
            val articleIndex = (dataIndex ?: 0) + 1
            binding.itemIndexLabel.text = binding.itemIndexLabel.resources.getString(
                R.string.gbs_digital_invoice_line_item_index,
                articleIndex,
                allData?.size ?: 0
            )
            binding.enableSwitch.isChecked = data.selected

            binding.removeButton.isVisible = data.addedByUser
            binding.enableSwitch.isInvisible = data.addedByUser


            data.lineItem.let { li ->
                binding.description.text = li.description
                binding.quantity.text = binding.quantity.resources.getString(
                    R.string.gbs_digital_invoice_line_item_quantity,
                    li.quantity
                )
                DigitalInvoice.lineItemTotalGrossPriceIntegralAndFractionalParts(li)
                    .let { (integral, fractional) ->
                        binding.grossPriceIntegralPart.text = integral
                        @SuppressLint("SetTextI18n")
                        binding.grossPriceFractionalPart.text = fractional
                    }
            }
            itemView.setOnClickListener {
                allData?.getOrNull(dataIndex ?: -1)?.let {
                    listener?.onLineItemClicked(it)
                }
            }

            binding.removeButton.setOnClickListener {
                allData?.getOrNull(dataIndex ?: -1)?.let {
                    listener?.removeLineItem(it)
                }
            }

            binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
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
        }

        override fun unbind() {
            listener = null
            itemView.setOnClickListener(null)
            binding.enableSwitch.setOnCheckedChangeListener(null)
        }

        fun enable() {
            itemView.isEnabled = true
            binding.strokeBackgroundView.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.gbs_digital_invoice_line_item_stroke_background
            )
            binding.description.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.gbs_digital_invoice_line_item_description_text
                )
            )
            binding.editButton.isEnabled = true

            binding.quantity.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.gbs_digital_invoice_line_item_quantity_text
                )
            )
            binding.grossPriceIntegralPart.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.gbs_digital_invoice_line_item_gross_price_text
                )
            )
            binding.grossPriceFractionalPart.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.gbs_digital_invoice_line_item_gross_price_text
                )
            )
        }


        fun disable() {
            itemView.isEnabled = false
            val disabledColor = ContextCompat.getColor(
                itemView.context,
                R.color.gbs_digital_invoice_line_item_disabled
            )
            binding.strokeBackgroundView.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.gbs_digital_invoice_line_item_stroke_background_disabled
            )
            binding.description.setTextColor(disabledColor)
            binding.editButton.isEnabled = false
            binding.quantity.setTextColor(disabledColor)
            binding.grossPriceIntegralPart.setTextColor(disabledColor)
            binding.grossPriceFractionalPart.setTextColor(disabledColor)
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
        private val priceFractionalPart: TextView = binding.gbsAddonPriceTotalFractionalPart

        override fun bind(
            data: Pair<DigitalInvoiceAddon, Boolean>,
            allData: List< Pair<DigitalInvoiceAddon, Boolean>>?,
            dataIndex: Int?
        ) {
            @SuppressLint("SetTextI18n")
            addonName.text = "${itemView.context.getString(data.first.nameStringRes)}:"
            DigitalInvoice.addonPriceIntegralAndFractionalParts(data.first)
                .let { (integral, fractional) ->
                    priceIntegralPart.text = integral
                    @SuppressLint("SetTextI18n")
                    priceFractionalPart.text = fractional
                }

            when (data.second) {
                true -> {
                    addonName.setTextColor(ContextCompat.getColor(
                        itemView.context,
                        R.color.gbs_digital_invoice_addon_name_text
                    ))

                    val enabledColor = ContextCompat.getColor(
                        itemView.context,
                        R.color.gbs_digital_invoice_addon_price_text
                    )
                    priceIntegralPart.setTextColor(enabledColor)
                    priceFractionalPart.setTextColor(enabledColor)
                }

                else -> {
                    val disabledColor = ContextCompat.getColor(
                        itemView.context,
                        R.color.gbs_digital_invoice_line_item_disabled
                    )
                    addonName.setTextColor(disabledColor)
                    priceIntegralPart.setTextColor(disabledColor)
                    priceFractionalPart.setTextColor(disabledColor)
                }
            }
        }

        override fun unbind() {
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    internal class FooterViewHolder(val binding: GbsItemDigitalInvoiceFooterBinding) :
        ViewHolder<DigitalInvoiceScreenContract.FooterDetails>(binding.root, Footer) {

        override fun bind(
            data: DigitalInvoiceScreenContract.FooterDetails,
            allData: List<DigitalInvoiceScreenContract.FooterDetails>?,
            dataIndex: Int?
        ) {
            binding.skipButton.isEnabled = data.buttonEnabled
            binding.skipButton.isVisible = data.inaccurateExtraction
            binding.payButton.isEnabled = data.buttonEnabled
            binding.payButton.text = if (data.buttonEnabled) {
                if (data.count > 0) {
                    binding.payButton.resources.getString(
                        R.string.gbs_digital_invoice_pay,
                        data.count,
                        data.total
                    )
                } else {
                    binding.payButton.resources.getString(R.string.gbs_digital_invoice_pay_other_charges)
                }
            } else {
                binding.payButton.resources.getString(R.string.gbs_digital_invoice_pay_disabled)
            }
            val (integral, fractional) = data.totalGrossPriceIntegralAndFractionalParts
            binding.grossPriceTotalIntegralPart.text = integral
            binding.grossPriceTotalFractionalPart.text = fractional
        }

        override fun unbind() {
        }
    }

    companion object {
        fun forViewTypeId(
            viewTypeId: Int, layoutInflater: LayoutInflater, parent: ViewGroup
        ) =
            when (ViewType.from(viewTypeId)) {
                Header -> HeaderViewHolder(
                    GbsItemDigitalInvoiceHeaderBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )
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
                Footer -> FooterViewHolder(
                    GbsItemDigitalInvoiceFooterBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )
            }
    }
}
