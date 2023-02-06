package net.gini.android.bank.sdk.capture.digitalinvoice

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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

    private var footerDetails: DigitalInvoiceScreenContract.FooterDetails? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val footerSkipButtonClickListener = View.OnClickListener {
        listener.skipButtonClicked()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewTypeId: Int): ViewHolder<*> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewHolder =
            ViewHolder.forViewTypeId(viewTypeId, layoutInflater, parent)

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

    internal companion object {
        fun from(viewTypeId: Int): ViewType = when (viewTypeId) {
            1 -> Header
            2 -> LineItem
            3 -> Addon
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
            binding.gbsEnableSwitch.isChecked = data.selected

            binding.gbsEnableSwitch.isInvisible = data.addedByUser


            data.lineItem.let { li ->
                binding.gbsDescription.text = "${li.quantity}x ${li.description}"
                DigitalInvoice.lineItemTotalGrossPriceIntegralAndFractionalParts(li)
                    .let { (integral, fractional) ->
                        binding.gbsGrossPriceIntegralPart.text = integral
                        binding.gbsGrossPriceFractionalPart.text = fractional
                    }

                DigitalInvoice.lineItemUnitPriceIntegralAndFractionalParts(li).let {(integral, fractional) ->
                    binding.gbsPerUnit.text = binding.gbsPerUnit.resources.getString(
                        R.string.gbs_digital_invoice_line_item_quantity,
                        "$integral$fractional"
                    )
                }
            }
            itemView.setOnClickListener {
                allData?.getOrNull(dataIndex ?: -1)?.let {
                    listener?.onLineItemClicked(it)
                }
            }

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
            allData: List< Pair<DigitalInvoiceAddon, Boolean>>?,
            dataIndex: Int?
        ) {
            @SuppressLint("SetTextI18n")
            addonName.text = "${itemView.context.getString(data.first.nameStringRes)}:"
            DigitalInvoice.addonPriceIntegralAndFractionalParts(data.first)
                .let { (integral, fractional) ->
                    priceIntegralPart.text = integral
                    fractionalPart.text = fractional
                }
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
            }
    }
}
