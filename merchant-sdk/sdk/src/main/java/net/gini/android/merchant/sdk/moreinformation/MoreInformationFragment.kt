package net.gini.android.merchant.sdk.moreinformation

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannedString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GmsFragmentPaymentMoreInformationBinding
import net.gini.android.merchant.sdk.databinding.GmsPaymentProviderIconHolderBinding
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme

/**
 * The [MoreInformationFragment] displays information and an FAQ section about the payment feature. It requires a
 * [PaymentComponent] instance to show the icons of the available payment provider apps.
 */
internal class MoreInformationFragment private constructor(private val paymentComponent: PaymentComponent?, private val backListener: BackListener? = null) :
    Fragment() {
    constructor() : this(paymentComponent = null)

    private var binding: GmsFragmentPaymentMoreInformationBinding by autoCleared()
    private val viewModel: MoreInformationViewModel by viewModels {
        MoreInformationViewModel.Factory(
            paymentComponent
        )
    }

    @VisibleForTesting
    internal val faqList: List<Pair<String, CharSequence>> by lazy {
        listOf(
            getString(R.string.gms_faq_1) to getString(R.string.gms_faq_answer_1),
            getString(R.string.gms_faq_2) to getString(R.string.gms_faq_answer_2),
            getString(R.string.gms_faq_3) to getString(R.string.gms_faq_answer_3),
            getString(R.string.gms_faq_4) to buildGiniRelatedAnswer(),
            getString(R.string.gms_faq_5) to getString(R.string.gms_faq_answer_5),
            getString(R.string.gms_faq_6) to getString(R.string.gms_faq_answer_6)
        )
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniMerchantTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GmsFragmentPaymentMoreInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gmsMoreInformationDetails.text = buildSpannedString {
            append(getString(R.string.gms_more_information_details))
            append(" ")
            append(createSpanForLink(R.string.gms_gini_link, R.string.gms_gini_link_url))
            append(".")
        }
        binding.gmsMoreInformationDetails.movementMethod = LinkMovementMethod.getInstance()
        binding.gmsPaymentProvidersIconsList.adapter = PaymentProvidersIconsAdapter(listOf())
        binding.gmsFaqList.apply {
            setAdapter(FaqExpandableListAdapter(faqList))
            setOnGroupClickListener { expandableListView, _, group, _ ->
                setListViewHeight(listView = expandableListView, group = group, isReload = false)
                return@setOnGroupClickListener false
            }
        }
        //Set initial list view height so we can scroll full page
        binding.gmsFaqList.postDelayed({
            setListViewHeight(listView = binding.gmsFaqList, group = getExpandedGroupPosition(), isReload = true)
        }, 100)

        viewModel.start()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentProviderAppsListFlow.collect { paymentProviderAppsListState ->
                    when (paymentProviderAppsListState) {
                        is MoreInformationViewModel.PaymentProviderAppsListState.Error -> {}
                        MoreInformationViewModel.PaymentProviderAppsListState.Loading -> {}
                        is MoreInformationViewModel.PaymentProviderAppsListState.Success -> updatePaymentProviderIconsAdapter(
                            paymentProviderAppsListState.paymentProviderAppsList
                        )
                    }
                }
            }
        }

        backListener?.let {
            requireActivity().onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = false
                    remove()
                    backListener.backCalled()
                }
            })
        }
    }

    private fun getExpandedGroupPosition(): Int {
        for (i in faqList.indices) {
            if (binding.gmsFaqList.isGroupExpanded(i)) return i
        }
        return -1
    }

    private fun updatePaymentProviderIconsAdapter(paymentProviderApps: List<PaymentProviderApp>) {
        (binding.gmsPaymentProvidersIconsList.adapter as PaymentProvidersIconsAdapter).apply {
            dataSet = paymentProviderApps
            notifyDataSetChanged()
        }
    }

    private fun createSpanForLink(@StringRes placeholder: Int, @StringRes urlResource: Int) =
        SpannableString(getString(placeholder)).apply {
            setSpan(
                URLSpanNoUnderline(getString(urlResource)),
                0,
                this.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                TextAppearanceSpan(requireContext(), R.style.GiniMerchantTheme_Typography_Link),
                0,
                this.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    private fun buildGiniRelatedAnswer(): SpannedString {
        val giniLink = createSpanForLink(R.string.gms_gini_website, R.string.gms_gini_link_url)
        val privacyPolicyString = createSpanForLink(R.string.gms_privacy_policy, R.string.gms_privacy_policy_link_url)
        val span = buildSpannedString {
            append(getString(R.string.gms_faq_answer_4))
            replace(indexOf("%s"), indexOf("%s") + 2, giniLink)
            replace(indexOf("%p"), indexOf("%p") + 2, privacyPolicyString)
        }
        return span
    }

    private fun setListViewHeight(
        listView: ExpandableListView,
        group: Int,
        isReload: Boolean
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
            if ((listView.isGroupExpanded(i) && (i != group || isReload)) || !listView.isGroupExpanded(i) && i == group) {
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

    internal class PaymentProvidersIconsAdapter(var dataSet: List<PaymentProviderApp?>) :
        RecyclerView.Adapter<PaymentProvidersIconsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = GmsPaymentProviderIconHolderBinding.inflate(
                parent.getLayoutInflaterWithGiniMerchantTheme(),
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = dataSet.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val context = holder.binding.root.context
            holder.binding.gmsPaymentProviderIcon.setImageDrawable(dataSet[position]?.icon)
            holder.binding.gmsPaymentProviderIcon.contentDescription = dataSet[position]?.paymentProvider?.name + " ${context.getString(
                R.string.gms_payment_provider_logo_content_description)}"
        }

        class ViewHolder(val binding: GmsPaymentProviderIconHolderBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        /**
         * Create a new instance of the [MoreInformationFragment].
         *
         * @param paymentComponent the [PaymentComponent] instance which contains the list of payment provider apps
         * @param backListener a listener for back events
         */
        fun newInstance(paymentComponent: PaymentComponent?, backListener: BackListener? = null): MoreInformationFragment =
            MoreInformationFragment(paymentComponent = paymentComponent, backListener = backListener)
    }

    private class URLSpanNoUnderline(url: String?) : URLSpan(url) {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }
}