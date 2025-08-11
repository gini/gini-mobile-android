package net.gini.android.internal.payment.moreinformation

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.gini.android.health.api.response.IngredientBrandType
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.databinding.GpsFragmentPaymentMoreInformationBinding
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.UrlSpanNoUnderline
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.getLocaleStringResource
import net.gini.android.internal.payment.utils.extensions.isViewModelInitialized

/**
 * The [MoreInformationFragment] displays information and an FAQ section about the payment feature. It requires a
 * [PaymentComponent] instance to show the icons of the available payment provider apps.
 */
class MoreInformationFragment private constructor(
    private val viewmodelFactory: ViewModelProvider.Factory? = null
) : Fragment() {
    constructor() : this(  null)

    private var binding: GpsFragmentPaymentMoreInformationBinding by autoCleared()
    private val viewModel: MoreInformationViewModel by viewModels {
      viewmodelFactory ?: object :ViewModelProvider.Factory {}
    }

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, viewModel.paymentComponent?.paymentModule)
    }

    @VisibleForTesting
    internal val faqList: List<Pair<String, CharSequence>> by lazy {
        listOf(
            getLocaleStringResource(R.string.gps_faq_1) to getLocaleStringResource(R.string.gps_faq_answer_1),
            getLocaleStringResource(R.string.gps_faq_2) to getLocaleStringResource(R.string.gps_faq_answer_2),
            getLocaleStringResource(R.string.gps_faq_3) to getLocaleStringResource(R.string.gps_faq_answer_3),
            getLocaleStringResource(R.string.gps_faq_4) to buildGiniRelatedAnswer(),
            getLocaleStringResource(R.string.gps_faq_5) to getLocaleStringResource(R.string.gps_faq_answer_5),
            getLocaleStringResource(R.string.gps_faq_6) to getLocaleStringResource(R.string.gps_faq_answer_6)
        )
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            GiniInternalPaymentModule.getSDKLanguageInternal(requireContext())?.languageLocale()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewmodelFactory == null && !isViewModelInitialized(MoreInformationViewModel::class)) {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GpsFragmentPaymentMoreInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gpsMoreInformationDetails.text = buildSpannedString {
            append(getLocaleStringResource(R.string.gps_more_information_details))
            append(" ")
            append(createSpanForLink(R.string.gps_gini_link, R.string.gps_gini_link_url))
            append(".")
        }
        binding.gpsMoreInformationDetails.movementMethod = LinkMovementMethod.getInstance()
        binding.gpsPaymentProvidersIconsList.adapter = PaymentProvidersIconsAdapter(listOf(), viewModel.getLocale())

        val faqAdapter = FaqRecyclerAdapter(viewModel.getLocale(), faqList)
        binding.gpsFaqRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.gpsFaqRecycler.adapter = faqAdapter

        binding.gpsPoweredByGini.root.visibility =
            if (viewModel.paymentComponent?.paymentModule?.getIngredientBrandVisibility() == IngredientBrandType.FULL_VISIBLE)
                View.VISIBLE else View.INVISIBLE

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
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    binding.root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                }
            }
        }
    }

    private fun updatePaymentProviderIconsAdapter(paymentProviderApps: List<PaymentProviderApp>) {
        (binding.gpsPaymentProvidersIconsList.adapter as PaymentProvidersIconsAdapter).apply {
            dataSet = paymentProviderApps
            notifyDataSetChanged()
        }
    }
    private fun createSpanForLink(@StringRes placeholder: Int, @StringRes urlResource: Int) =
        SpannableString(getLocaleStringResource(placeholder)).apply {
            setSpan(
                UrlSpanNoUnderline(requireContext(), getLocaleStringResource(urlResource)),
                0,
                this.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                TextAppearanceSpan(requireContext(), R.style.GiniPaymentTheme_Typography_Link),
                0,
                this.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }


    private fun buildGiniRelatedAnswer(): SpannedString {
        val giniLink = createSpanForLink(R.string.gps_gini_website, R.string.gps_gini_link_url)
        val privacyPolicyString =
            createSpanForLink(R.string.gps_privacy_policy, R.string.gps_privacy_policy_link_url)
        val baseText = getLocaleStringResource(R.string.gps_faq_answer_4)

        return buildSpannedString {
            append(baseText)
            val sIndex = indexOf("%s")
            if (sIndex >= 0) {
                replace(sIndex, sIndex + 2, giniLink)
            }
            val pIndex = indexOf("%p")
            if (pIndex >= 0) {
                replace(pIndex, pIndex + 2, privacyPolicyString)
            }
        }
    }

    companion object {
        /**
         * Create a new instance of the [MoreInformationFragment].
         *
         * @param paymentComponent the [PaymentComponent] instance which contains the list of payment provider apps
         */
        fun newInstance(paymentComponent: PaymentComponent?): MoreInformationFragment {
            val viewModelFactory = MoreInformationViewModel.Factory(
                paymentComponent
            )
            return MoreInformationFragment(viewmodelFactory = viewModelFactory)

        }

    }
}