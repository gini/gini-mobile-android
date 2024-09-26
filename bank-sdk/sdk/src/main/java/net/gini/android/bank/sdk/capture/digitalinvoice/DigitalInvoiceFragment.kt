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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.DigitalInvoiceSkontoFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoResultArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.skonto.factory.text.SkontoDiscountLabelTextFactory
import net.gini.android.bank.sdk.capture.skonto.factory.text.SkontoSavedAmountTextFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.capture.util.parentFragmentManagerOrNull
import net.gini.android.bank.sdk.databinding.GbsFragmentDigitalInvoiceBinding
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocShouldBeAutoAttachedUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocsFeatureEnabledUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogCancelAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.ui.dialog.attachdoc.AttachDocumentToTransactionDialog
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.bank.sdk.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.ui.IntervalToolbarMenuItemIntervalClickListener
import net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType


/**
 * Created by Alpar Szotyori on 05.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */


private const val TAG_RETURN_REASON_DIALOG = "TAG_RETURN_REASON_DIALOG"

/**
 * Internal use only.
 */
internal open class DigitalInvoiceFragment : Fragment(), DigitalInvoiceScreenContract.View,
    LineItemsAdapterListener {

    private val args: DigitalInvoiceFragmentArgs by navArgs<DigitalInvoiceFragmentArgs>()

    private var binding by autoCleared<GbsFragmentDigitalInvoiceBinding>()
    private var lineItemsAdapter by autoCleared<LineItemsAdapter>()
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.ReturnAssistant

    var listener: DigitalInvoiceFragmentListener? = null
        set(value) {
            field = value
            this.presenter?.listener = value
        }

    lateinit var cancelListener: CancelListener

    override val viewLifecycleScope: CoroutineScope
        get() = viewLifecycleOwner.lifecycleScope

    private var presenter: DigitalInvoiceScreenContract.Presenter? = null

    private var footerDetails: DigitalInvoiceScreenContract.FooterDetails? = null
    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private var onBackPressedCallback: OnBackPressedCallback? = null

    private val skontoSavedAmountTextFactory: SkontoSavedAmountTextFactory by getGiniBankKoin().inject()
    private val skontoDiscountLabelTextFactory: SkontoDiscountLabelTextFactory by getGiniBankKoin().inject()

    private val transactionDocShouldBeAutoAttachedUseCase: GetTransactionDocShouldBeAutoAttachedUseCase
            by getGiniBankKoin().inject()
    private val transactionDocDialogCancelAttachUseCase: TransactionDocDialogCancelAttachUseCase
            by getGiniBankKoin().inject()
    private val transactionDocDialogConfirmAttachUseCase: TransactionDocDialogConfirmAttachUseCase
            by getGiniBankKoin().inject()
    private val getTransactionDocsFeatureEnabledUseCase: GetTransactionDocsFeatureEnabledUseCase
            by getGiniBankKoin().inject()

    private val skontoAdapterListener = object : SkontoListItemAdapterListener {

        override fun onSkontoEditClicked(listItem: DigitalInvoiceSkontoListItem) {
            presenter?.editSkontoDataListItem(listItem)
        }

        override fun onSkontoEnabled(listItem: DigitalInvoiceSkontoListItem) {
            presenter?.enableSkonto()
        }

        override fun onSkontoDisabled(listItem: DigitalInvoiceSkontoListItem) {
            presenter?.disableSkonto()
        }
    }

    override fun showSkontoEditScreen(
        data: SkontoData,
        isSkontoSectionActive: Boolean,
    ) {
        findNavController().navigate(
            DigitalInvoiceFragmentDirections.toDigitalInvoiceSkontoFragment(
                DigitalInvoiceSkontoArgs(
                    data = data,
                    invoiceHighlights = args.skontoInvoiceHighlights.toList(),
                    isSkontoSectionActive = isSkontoSectionActive
                )
            )
        )
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
        initListener()
        createPresenter(activity, savedInstanceState)

        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    private fun changeMarginAccordingToFontOversize() {
        if (!ContextHelper.isTablet(requireContext()) && resources.configuration.fontScale > 1.0F) {
            (binding.gbsArticleTxt.layoutParams as ViewGroup.MarginLayoutParams).run {
                this.topMargin =
                    resources.getDimension(net.gini.android.capture.R.dimen.gc_small).toInt()
            }
            (binding.totalLabel.layoutParams as ViewGroup.MarginLayoutParams).run {
                topMargin =
                    resources.getDimension(net.gini.android.capture.R.dimen.gc_small).toInt()
            }
            (binding.gbsPay.layoutParams as ViewGroup.MarginLayoutParams).run {
                bottomMargin =
                    resources.getDimension(net.gini.android.capture.R.dimen.gc_small).toInt()
                topMargin =
                    resources.getDimension(net.gini.android.capture.R.dimen.gc_small).toInt()
            }
        }
    }

    private fun createPresenter(activity: Activity, savedInstanceState: Bundle?) =
        DigitalInvoiceScreenPresenter(
            activity,
            this,
            args.extractionsResult.specificExtractions,
            args.extractionsResult.compoundExtractions,
            args.extractionsResult.returnReasons,
            args.skontoData,
            getAmountsAreConsistentExtraction(args.extractionsResult.specificExtractions),
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
        changeMarginAccordingToFontOversize()
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
            cancelListener.onCancelFlow()
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
                    injectedViewAdapter.setOnMenuItemClickListener(
                        IntervalToolbarMenuItemIntervalClickListener {
                            if (it.itemId == R.id.help) {
                                showHelp()
                            }
                            true
                        })
                }

                injectedViewAdapter.setOnNavButtonClickListener {
                    trackCloseTappedEvent()
                    cancelListener.onCancelFlow()
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
                        payButtonClicked()
                    }

                    footerDetails?.let {
                        val (integral, fractional) = it.totalGrossPriceIntegralAndFractionalParts
                        injectedViewAdapter.setTotalPrice(integral + fractional)
                        injectedViewAdapter.setProceedButtonEnabled(it.buttonEnabled)
                        injectedViewAdapter.onSkontoPercentageBadgeVisibilityUpdate(
                            it.skontoDiscountPercentage != null
                        )
                        injectedViewAdapter.onSkontoSavingsAmountVisibilityUpdated(
                            it.skontoSavedAmount != null
                        )
                        it.skontoDiscountPercentage?.let { percentage ->
                            injectedViewAdapter.onSkontoPercentageBadgeUpdated(
                                skontoDiscountLabelTextFactory.create(percentage)
                            )
                        }
                        it.skontoSavedAmount?.let { amount ->
                            injectedViewAdapter.onSkontoSavingsAmountUpdated(
                                skontoSavedAmountTextFactory.create(amount)
                            )
                        }
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
        lineItemsAdapter = LineItemsAdapter(this, skontoAdapterListener, requireContext())
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
        tryShowAttachDocToTransactionDialog {
            presenter?.pay()
            trackProceedTapped()
            trackSdkClosedEvent()
        }
    }

    private fun tryShowAttachDocToTransactionDialog(continueFlow: () -> Unit) {
        val autoAttachDoc = runBlocking { transactionDocShouldBeAutoAttachedUseCase() }
        if (!getTransactionDocsFeatureEnabledUseCase()) {
            continueFlow()
            return
        }
        binding.gbComposeView.setContent {
            GiniTheme {
                if (!autoAttachDoc) {
                    AttachDocumentToTransactionDialog(onDismiss = {
                        lifecycleScope.launch { transactionDocDialogCancelAttachUseCase() }
                        continueFlow()
                    }, onConfirm = {
                        lifecycleScope.launch { transactionDocDialogConfirmAttachUseCase(it) }
                        continueFlow()
                    })
                } else {
                    continueFlow()
                }
            }
        }
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
        findNavController().navigate(
            DigitalInvoiceFragmentDirections.toDigitalInvoiceEditItemBottomSheetDialog(
                selectableLineItem
            )
        )
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

        val isSkontoSavedAmountVisible = data.skontoSavedAmount != null
        val isSkontoDiscountVisible = data.skontoDiscountPercentage != null

        binding.skontoSavedAmount.isVisible = isSkontoSavedAmountVisible
        if (data.skontoSavedAmount != null) {
            binding.skontoSavedAmount.text =
                skontoSavedAmountTextFactory.create(data.skontoSavedAmount)
        }

        binding.skontoDiscountLabel.isVisible = isSkontoDiscountVisible
        if (data.skontoDiscountPercentage != null) {
            binding.skontoDiscountLabel.text =
                skontoDiscountLabelTextFactory.create(data.skontoDiscountPercentage)
        }


        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            binding.gbsBottomBarNavigation.modifyAdapterIfOwned {
                (it as DigitalInvoiceNavigationBarBottomAdapter).apply {
                    setTotalPrice(integral + fractional)
                    setProceedButtonEnabled(data.buttonEnabled)
                    onSkontoSavingsAmountVisibilityUpdated(isSkontoSavedAmountVisible)
                    onSkontoPercentageBadgeVisibilityUpdate(isSkontoDiscountVisible)
                    data.skontoDiscountPercentage?.let { percentage ->
                        onSkontoPercentageBadgeUpdated(
                            skontoDiscountLabelTextFactory.create(
                                percentage
                            )
                        )
                    }
                    data.skontoSavedAmount?.let { amount ->
                        onSkontoSavingsAmountUpdated(skontoSavedAmountTextFactory.create(amount))
                    }
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

    override fun showSkonto(data: DigitalInvoiceSkontoListItem) {
        lineItemsAdapter.skontoDiscount = listOf(data)
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
        setSkontoResultListener()
    }

    private fun setBottomSheetResultListener() {
        parentFragmentManager
            .setFragmentResultListener(
                DigitalInvoiceBottomSheet.REQUEST_KEY,
                viewLifecycleOwner
            ) { _: String?, result: Bundle ->
                BundleCompat.getParcelable(
                    result,
                    DigitalInvoiceBottomSheet.RESULT_KEY,
                    SelectableLineItem::class.java
                )
                    ?.let { selectableLineItem ->
                        presenter?.updateLineItem(selectableLineItem)
                    }
            }
    }

    private fun setSkontoResultListener() {
        parentFragmentManager
            .setFragmentResultListener(
                DigitalInvoiceSkontoFragment.REQUEST_KEY,
                viewLifecycleOwner
            ) { _: String?, result: Bundle ->
                BundleCompat.getParcelable(
                    result,
                    DigitalInvoiceSkontoFragment.RESULT_KEY,
                    DigitalInvoiceSkontoResultArgs::class.java
                )?.let { skontoResult ->
                    presenter?.updateSkontoData(skontoResult.skontoData)
                    if (skontoResult.isSkontoEnabled) {
                        presenter?.enableSkonto()
                    } else {
                        presenter?.disableSkonto()
                    }
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
            setOf(UserAnalyticsEventProperty.Screen(screenName)),
        )
    }

    private fun trackHelpTappedEvent() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.HELP_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName)),
        )
    }

    private fun trackItemSwitchTappedTappedEvent(selected: Boolean) = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.ITEM_SWITCH_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.SwitchActive(selected)
            )
        )
    }

    private fun trackItemEditTappedTappedEvent() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.EDIT_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName)),
        )
    }

    private fun trackProceedTapped() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.PROCEED_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
    }

    private fun trackSdkClosedEvent() = runCatching {
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.SDK_CLOSED, setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.Status(UserAnalyticsEventProperty.Status.StatusType.Successful)
            )
        )
    }

    private fun getAmountsAreConsistentExtraction(extractions: Map<String, GiniCaptureSpecificExtraction>): Boolean {
        val isInaccurateExtraction = extractions["amountsAreConsistent"]?.let {
            it.value == "false"
        } ?: true
        return isInaccurateExtraction
    }
}
// endregion

