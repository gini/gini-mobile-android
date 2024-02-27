package net.gini.android.health.sdk.moreinformation

import android.database.DataSetObserver
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannedString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.text.buildSpannedString
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

class MoreInformationFragment private constructor(val paymentComponent: PaymentComponent?) :
    Fragment() {

    private var binding: GhsFragmentPaymentMoreInformationBinding by autoCleared()
    private val faqList: List<Pair<String, CharSequence>> by lazy {
        listOf(
            getString(R.string.ghs_faq_1) to getString(R.string.ghs_faq_answer_1),
            getString(R.string.ghs_faq_2) to getString(R.string.ghs_faq_answer_2),
            getString(R.string.ghs_faq_3) to getString(R.string.ghs_faq_answer_3),
            getString(R.string.ghs_faq_4) to buildGiniRelatedAnswer(),
            getString(R.string.ghs_faq_5) to getString(R.string.ghs_faq_answer_5),
            getString(R.string.ghs_faq_6) to getString(R.string.ghs_faq_answer_6)
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
            title = getString(R.string.ghs_more_information_underlined_part).replace(".", "")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ghsMoreInformationDetails.text = buildSpannedString {
            append(getString(R.string.ghs_more_information_details))
            append(" ")
            append(createGiniSpannableLink())
            append(".")
        }
        binding.ghsMoreInformationDetails.movementMethod = LinkMovementMethod.getInstance()
        binding.ghsPaymentProvidersIconsList.adapter = PaymentProvidersIconsAdapter(listOf())
        binding.ghsFaqList.apply {
            setAdapter(FaqExpandableListAdapter(faqList))
            setOnGroupClickListener { expandableListView, view, i, l ->
                setListViewHeight(listView = expandableListView, group = i)
                return@setOnGroupClickListener false
            }
        }
        //Set initial list view height so we can scroll full page
        binding.ghsFaqList.postDelayed({
            setListViewHeight(listView = binding.ghsFaqList, group = -1)
        }, 100)
        viewLifecycleOwner.lifecycleScope.launch {
            paymentComponent?.paymentProviderAppsFlow?.collect { paymentProviderAppsState ->
                when (paymentProviderAppsState) {
                    is PaymentProviderAppsState.Error -> {}
                    PaymentProviderAppsState.Loading -> {}
                    is PaymentProviderAppsState.Success -> updatePaymentProviderIconsAdapter(
                        paymentProviderAppsState.paymentProviderApps
                    )
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

    private fun createGiniSpannableLink(): SpannableString =
        SpannableString(getString(R.string.ghs_gini_link)).apply {
            setSpan(
                URLSpanNoUnderline(getString(R.string.ghs_gini_link_url)),
                0,
                this.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                TextAppearanceSpan(requireContext(), R.style.GiniHealth_Link_TextAppearance),
                0,
                this.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    private fun buildGiniRelatedAnswer(): SpannedString {
        val giniLink = createGiniSpannableLink()
        val span = buildSpannedString {
            append(getString(R.string.ghs_faq_answer_4))
            replace(indexOf("%s"), indexOf("%s") + 2, giniLink)
        }
        return span
    }

    private fun setListViewHeight(
        listView: ExpandableListView,
        group: Int
    ) {
        val listAdapter = listView.expandableListAdapter as ExpandableListAdapter
        var totalHeight = 0
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(
            listView.width,
            View.MeasureSpec.EXACTLY
        )
        for (i in 0 until listAdapter.groupCount) {
            val groupItem = listAdapter.getGroupView(i, false, null, listView)
            groupItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            totalHeight += groupItem.measuredHeight
            if (group == -1) continue
            if (listView.isGroupExpanded(i) && i != group || !listView.isGroupExpanded(i) && i == group) {
                for (j in 0 until listAdapter.getChildrenCount(i)) {
                    val listItem = listAdapter.getChildView(
                        i, j, false, null,
                        listView
                    )
                    listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
                    totalHeight += listItem.measuredHeight
                }
            }
        }
        val params = listView.layoutParams
        params.height = totalHeight + listView.dividerHeight * (listAdapter.groupCount - 1)
        listView.layoutParams = params
        listView.requestLayout()
    }

    internal class PaymentProvidersIconsAdapter(var dataSet: List<Drawable?>) :
        RecyclerView.Adapter<PaymentProvidersIconsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = GhsPaymentProviderIconHolderBinding.inflate(
                parent.getLayoutInflaterWithGiniHealthTheme(),
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = dataSet.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.ghsPaymentProviderIcon.setImageDrawable(dataSet[position])
        }

        class ViewHolder(val binding: GhsPaymentProviderIconHolderBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent?): MoreInformationFragment =
            MoreInformationFragment(paymentComponent = paymentComponent)
    }

    private class URLSpanNoUnderline(url: String?) : URLSpan(url) {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }
}