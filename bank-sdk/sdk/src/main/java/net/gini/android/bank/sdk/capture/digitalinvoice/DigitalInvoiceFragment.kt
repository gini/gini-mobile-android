package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import kotlinx.coroutines.CoroutineScope
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.capture.util.parentFragmentManagerOrNull
import net.gini.android.bank.sdk.databinding.GbsFragmentDigitalInvoiceBinding
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.bank.sdk.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.ui.IntervalToolbarMenuItemIntervalClickListener
import net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsExtraProperties
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsValue
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType


/**
 * Created by Alpar Szotyori on 05.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

private const val ARGS_EXTRACTIONS = "GBS_ARGS_EXTRACTIONS"
private const val ARGS_COMPOUND_EXTRACTIONS = "GBS_ARGS_COMPOUND_EXTRACTIONS"
private const val ARGS_RETURN_REASONS = "GBS_ARGS_RETURN_REASONS"
private const val ARGS_INACCURATE_EXTRACTION = "GBS_ARGS_INACCURATE_EXTRACTION"

private const val TAG_RETURN_REASON_DIALOG = "TAG_RETURN_REASON_DIALOG"
private const val TAG_WHAT_IS_THIS_DIALOG = "TAG_WHAT_IS_THIS_DIALOG"

/**
 * Internal use only.
 */
open class DigitalInvoiceFragment : Fragment(), DigitalInvoiceScreenContract.View, LineItemsAdapterListener {

    private var binding by autoCleared<GbsFragmentDigitalInvoiceBinding>()
    private var lineItemsAdapter by autoCleared<LineItemsAdapter>()

    var listener: DigitalInvoiceFragmentListener? = null
        set(value) {
            field = value
            this.presenter?.listener = value
        }

    override val viewLifecycleScope: CoroutineScope
        get() = viewLifecycleOwner.lifecycleScope

    private var presenter: DigitalInvoiceScreenContract.Presenter? = null

    private var extractions: Map<String, GiniCaptureSpecificExtraction> = emptyMap()
    private var compoundExtractions: Map<String, GiniCaptureCompoundExtraction> = emptyMap()
    private var returnReasons: List<GiniCaptureReturnReason> = emptyList()
    private var isInaccurateExtraction: Boolean = false
    private var footerDetails: DigitalInvoiceScreenContract.FooterDetails? = null
    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private var onBackPressedCallback: OnBackPressedCallback? = null

    companion object {
        internal fun getExtractionsBundle(extractions: Map<String, GiniCaptureSpecificExtraction>): Bundle =
            Bundle().apply {
                extractions.forEach { putParcelable(it.key, it.value) }
            }

        internal fun getCompoundExtractionsBundle(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>): Bundle =
            Bundle().apply {
                compoundExtractions.forEach { putParcelable(it.key, it.value) }
            }

        internal fun getAmountsAreConsistentExtraction(extractions: Map<String, GiniCaptureSpecificExtraction>): Boolean {
            val isInaccurateExtraction = extractions["amountsAreConsistent"]?.let {
                it.value == "false"
            } ?: true
            return isInaccurateExtraction
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this.activity
        checkNotNull(activity) {
            "Missing activity for fragment."
        }
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        forcePortraitOrientationOnPhones(activity)
        readArguments()
        initListener()
        createPresenter(activity, savedInstanceState)

        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    private fun readArguments() {
        arguments?.run {
            getBundle(ARGS_EXTRACTIONS)?.run {
                extractions =
                    keySet().map { it to getParcelable<GiniCaptureSpecificExtraction>(it)!! }
                        .toMap()
            }
            getBundle(ARGS_COMPOUND_EXTRACTIONS)?.run {
                compoundExtractions =
                    keySet().map { it to getParcelable<GiniCaptureCompoundExtraction>(it)!! }
                        .toMap()
            }
            returnReasons =
                (BundleCompat.getParcelableArray(this, ARGS_RETURN_REASONS, GiniCaptureReturnReason::class.java)
                    ?.toList() as? List<GiniCaptureReturnReason>) ?: emptyList()

            isInaccurateExtraction = getBoolean(ARGS_INACCURATE_EXTRACTION, false)
        }
    }

    private fun createPresenter(activity: Activity, savedInstanceState: Bundle?) =
        DigitalInvoiceScreenPresenter(
            activity,
            this,
            extractions,
            compoundExtractions,
            returnReasons,
            isInaccurateExtraction,
            savedInstanceState,
        ).apply {
            listener = this@DigitalInvoiceFragment.listener
        }

    override fun onSaveInstanceState(outState: Bundle) {
        presenter?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GbsFragmentDigitalInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        setInputHandlers()
        initTopNavigationBar()
        initBottomBar()
        presenter?.onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        onBackPressedCallback?.isEnabled = false
        onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback {
            if (this@DigitalInvoiceFragment.isVisible) {
                trackCloseTappedEvent()
            }
            isEnabled = false
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        onBackPressedCallback?.isEnabled = false
    }

    private fun initTopNavigationBar() {
        if (GiniCapture.hasInstance()) {
            binding.gbsTopBarNavigation.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
            ) { injectedViewAdapter ->
                injectedViewAdapter.setTitle(getString(R.string.gbs_digital_invoice_onboarding_text_1))

                injectedViewAdapter.setNavButtonType(NavButtonType.CLOSE)

                if (!GiniCapture.getInstance().isBottomNavigationBarEnabled) {
                    injectedViewAdapter.setMenuResource(R.menu.gbs_menu_digital_invoice)
                    injectedViewAdapter.setOnMenuItemClickListener(IntervalToolbarMenuItemIntervalClickListener {
                        if (it.itemId == R.id.help) {
                            showHelp()
                        }
                        true
                    })
                }

                injectedViewAdapter.setOnNavButtonClickListener {
                    trackCloseTappedEvent()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }

    private fun showHelp() {
        trackHelpTappedEvent()
        findNavController().navigate(DigitalInvoiceFragmentDirections.toDigitalInvoiceHelpFragment())
    }

    private fun initBottomBar() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {

            binding.gbsBottomWrapper.visibility = View.INVISIBLE
            binding.gbsPay.isEnabled = false

            binding.gbsBottomBarNavigation.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(GiniBank.digitalInvoiceNavigationBarBottomAdapterInstance) { injectedViewAdapter ->
                    injectedViewAdapter.setOnHelpClickListener {
                        showHelp()
                    }

                    injectedViewAdapter.setOnProceedClickListener {
                        presenter?.pay()
                        trackProceedTapped()
                    }

                    footerDetails?.let {
                        val (integral, fractional) = it.totalGrossPriceIntegralAndFractionalParts
                        injectedViewAdapter.setTotalPrice(integral + fractional)
                        injectedViewAdapter.setProceedButtonEnabled(it.buttonEnabled)
                    }
                }
        }
    }

    private fun initListener() {
        if (activity is DigitalInvoiceFragmentListener) {
            listener = activity as DigitalInvoiceFragmentListener?
        } else checkNotNull(listener) {
            ("MultiPageReviewFragmentListener not set. "
                    + "You can set it with MultiPageReviewFragment#setListener() or "
                    + "by making the host activity implement the MultiPageReviewFragmentListener.")
        }
    }

    private fun initRecyclerView() {
        lineItemsAdapter = LineItemsAdapter(this, requireContext())
        activity?.let {
            binding.lineItems.apply {
                layoutManager = LinearLayoutManager(it)
                adapter = lineItemsAdapter
                setHasFixedSize(true)
            }
        }
    }

    private fun setInputHandlers() {
        binding.gbsPay.setOnClickListener {
            payButtonClicked()
        }
    }

    override fun payButtonClicked() {
        presenter?.pay()
        trackProceedTapped()
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun showLineItems(
        lineItems: List<SelectableLineItem>,
        isInaccurateExtraction: Boolean
    ) {
        lineItemsAdapter.apply {
            this.isInaccurateExtraction = isInaccurateExtraction
            this.lineItems = lineItems
        }
    }

    private lateinit var smoothScroller: SmoothScroller
    private val scrollListener = object : SmoothScroller.SmoothScrollerListener {
        override fun didStop() {
            scrollList(true)
        }

    }

    /**
     * header and footer are counted as aprox. 3 items
     * in order to have same time spent on scrolling different size views
     */
    override fun animateListScroll() {
        val itemCount =
            3 + (if (lineItemsAdapter.isInaccurateExtraction) 3 else 0) + lineItemsAdapter.lineItems.size
        smoothScroller = SmoothScroller(
            requireContext(),
            itemCount,
            scrollListener
        )
        scrollList(false)
    }

    override fun onEditLineItem(selectableLineItem: SelectableLineItem) {
        findNavController().navigate(DigitalInvoiceFragmentDirections.toDigitalInvoiceEditItemBottomSheetDialog(selectableLineItem))
    }

    override fun showOnboarding() {
        findNavController().navigate(DigitalInvoiceFragmentDirections.toDigitalInvoiceOnboardingFragment())
    }

    private fun scrollList(toTop: Boolean) {
        val delay: Long = if (toTop) 350 else 200
        binding.lineItems.postDelayed(Runnable {
            smoothScroller.targetPosition = if (toTop) 0 else lineItemsAdapter.itemCount
            (binding.lineItems.layoutManager as? LinearLayoutManager)?.startSmoothScroll(
                smoothScroller
            )
        }, delay)

    }

    internal class SmoothScroller(
        context: Context,
        private val itemsCount: Int,
        private val listener: SmoothScrollerListener
    ) : LinearSmoothScroller(context) {
        private val totalScrollTime = 2400f

        interface SmoothScrollerListener {
            fun didStop()
        }

        override fun onStop() {
            super.onStop()
            if (targetPosition > 0) {
                listener.didStop()
            }
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            return totalScrollTime / (itemsCount * displayMetrics.densityDpi)
        }

    }

    override fun updateFooterDetails(data: DigitalInvoiceScreenContract.FooterDetails) {
        val (integral, fractional) = data.totalGrossPriceIntegralAndFractionalParts
        binding.grossPriceTotalIntegralPart.text = integral
        binding.grossPriceTotalFractionalPart.text = fractional
        binding.gbsPay.isEnabled = data.buttonEnabled

        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            binding.gbsBottomBarNavigation.modifyAdapterIfOwned {
                (it as DigitalInvoiceNavigationBarBottomAdapter).apply {
                    setTotalPrice(integral + fractional)
                    setProceedButtonEnabled(data.buttonEnabled)
                }
            }
        }

        this.footerDetails = data
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun showAddons(addons: List<DigitalInvoiceAddon>) {
        lineItemsAdapter.addons = addons
    }


    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun showReturnReasonDialog(
        reasons: List<GiniCaptureReturnReason>,
        resultCallback: ReturnReasonDialogResultCallback
    ) {
        parentFragmentManagerOrNull()?.let { fragmentManager ->
            ReturnReasonDialog.createInstance(reasons).run {
                callback = resultCallback
                show(fragmentManager, TAG_RETURN_REASON_DIALOG)
            }
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun setPresenter(presenter: DigitalInvoiceScreenContract.Presenter) {
        this.presenter = presenter
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onStart() {
        super.onStart()
        presenter?.start()
        setBottomSheetResultListener()
    }

    private fun setBottomSheetResultListener() {
        parentFragmentManager
            .setFragmentResultListener(
                DigitalInvoiceBottomSheet.REQUEST_KEY,
                viewLifecycleOwner
            ) { _: String?, result: Bundle ->
                BundleCompat.getParcelable(result, DigitalInvoiceBottomSheet.RESULT_KEY, SelectableLineItem::class.java)
                    ?.let { selectableLineItem ->
                        presenter?.updateLineItem(selectableLineItem)
                    }
            }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onStop() {
        super.onStop()
        presenter?.stop()
    }


    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onLineItemClicked(lineItem: SelectableLineItem) {
        presenter?.editLineItem(lineItem)
        trackItemEditTappedTappedEvent()
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onLineItemSelected(lineItem: SelectableLineItem) {
        presenter?.selectLineItem(lineItem)
        trackItemSwitchTappedTappedEvent(lineItem.selected)
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onLineItemDeselected(lineItem: SelectableLineItem) {
        presenter?.deselectLineItem(lineItem)
        trackItemSwitchTappedTappedEvent(lineItem.selected)
    }

    // region Analytics

    private fun trackCloseTappedEvent() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.CLOSE_TAPPED,
            UserAnalyticsScreen.RETURN_ASSISTANT,
        )
    }

    private fun trackHelpTappedEvent() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.HELP_TAPPED,
            UserAnalyticsScreen.RETURN_ASSISTANT,
        )
    }

    private fun trackItemSwitchTappedTappedEvent(selected: Boolean) = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.ITEM_SWITCH_TAPPED,
            UserAnalyticsScreen.RETURN_ASSISTANT,
            mapOf(UserAnalyticsExtraProperties.SWITCH_ACTIVE to selected.mapToAnalyticsValue())
        )
    }

    private fun trackItemEditTappedTappedEvent() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.EDIT_TAPPED,
            UserAnalyticsScreen.RETURN_ASSISTANT,
        )
    }

    private fun trackProceedTapped() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.PROCEED_TAPPED, UserAnalyticsScreen.RETURN_ASSISTANT
        )
    }
}
    // endregion

