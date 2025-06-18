package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.analytics.getDifferences
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsScreenContract
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsScreenPresenter
import net.gini.android.bank.sdk.capture.digitalinvoice.details.MIN_QUANTITY
import net.gini.android.bank.sdk.capture.digitalinvoice.details.doAfterTextChanged
import net.gini.android.bank.sdk.capture.util.amountWatcher
import net.gini.android.bank.sdk.capture.util.hideKeyboard
import net.gini.android.bank.sdk.capture.util.showKeyboard
import net.gini.android.bank.sdk.databinding.GbsEditItemBottomSheetBinding
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.bank.sdk.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.bank.sdk.util.wrappedWithGiniCaptureTheme
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen

private const val ARGS_SELECTABLE_LINE_ITEM = "GBS_ARGS_SELECTABLE_LINE_ITEM"

/**
 * Internal use only.
 */
internal class DigitalInvoiceBottomSheet : BottomSheetDialogFragment(), LineItemDetailsScreenContract.View {

    private lateinit var binding: GbsEditItemBottomSheetBinding
    private var selectableLineItem: SelectableLineItem? = null
    private var originalSelectableLineItem: SelectableLineItem? = null
    private val keyboardThresholdRatio = 0.15
    private var quantity: Int = 1
    private val editorListener = TextView.OnEditorActionListener { v, actionId, event ->
        v.clearFocus()
        v.hideKeyboard()
        true
    }
    private var isKeyboardShowing = false
    private val isKeyboardShowingStateKey = "keyboard_showing_state_key"
    private val priceKey = "price_key"
    private val quantityKey = "quantity_key"
    private val descriptionKey = "description_key"
    private var focusedViewId = View.NO_ID
    private val focusedViewIdKey = "focused_view_id_key"

    private var selectedCurrency = "EUR"
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.EditReturnAssistant

    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var restoringFromOrientationChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectableLineItem = BundleCompat.getParcelable(it, ARGS_SELECTABLE_LINE_ITEM, SelectableLineItem::class.java)
            originalSelectableLineItem = selectableLineItem?.copy()
            activity?.let { activity ->
                createPresenter(activity)
            }
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            restoringFromOrientationChange = savedInstanceState != null
            activity?.let {
                binding = GbsEditItemBottomSheetBinding.inflate(getLayoutInflaterWithGiniCaptureTheme(it.layoutInflater), null, false)

                val builder = AlertDialog.Builder(context)
                builder.setView(binding.root)
                setUpBindings()

                val alertDialog = builder.create()

                activity?.window?.decorView?.let { view ->
                    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insetsCompat ->
                        val imeVisible = insetsCompat.isVisible(WindowInsetsCompat.Type.ime())
                        isKeyboardShowing = imeVisible
                        insetsCompat
                    }
                }

                alertDialog.setOnShowListener {
                    restoreKeyboardState(savedInstanceState)
                }

                return alertDialog
            }
        }

       return super.onCreateDialog(savedInstanceState)
    }

    override fun getTheme(): Int {
        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            return super.getTheme()
        }
        return R.style.GiniCaptureTheme_DigitalInvoice_Edit_BottomSheetDialog

    }

    private fun createPresenter(activity: Activity) = LineItemDetailsScreenPresenter(
        activity, this,
        selectableLineItem!!
    )

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(isKeyboardShowingStateKey, isKeyboardShowing)
        if (isKeyboardShowing) outState.putInt(focusedViewIdKey, focusedViewId)
        outState.putString(priceKey, binding.gbsUnitPriceEditTxt.text.toString())
        outState.putString(quantityKey, binding.gbsQuantityEditTxt.text.toString())
        outState.putString(descriptionKey, binding.gbsArticleNameEditTxt.text.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        binding = GbsEditItemBottomSheetBinding.inflate(inflater, container, false)
        handleBottomSheetConfigurations()
        return binding.root
    }

    private fun handleBottomSheetConfigurations() {
        dialog?.setOnShowListener {
            val bottomSheetInternal =
                (it as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.let {
                BottomSheetBehavior.from(bottomSheetInternal).apply {
                    peekHeight = 0
                    state = BottomSheetBehavior.STATE_EXPANDED
                    addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            if (newState in listOf(
                                    BottomSheetBehavior.STATE_HIDDEN,
                                    STATE_COLLAPSED
                                )
                            )
                                dismiss()

                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                    })
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setAccessibilityPaneTitle(view, getString(R.string.gbs_edit_article))

        restoringFromOrientationChange = savedInstanceState != null
        setUpBindings()

        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            dialog?.window?.disallowScreenshots()
        }

        restoreKeyboardState(savedInstanceState)
        trackScreenShownEvent()
        addGlobalViewTreeObserver()
        restoreTextIfChanged(savedInstanceState)
    }

    private fun restoreTextIfChanged(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            binding.gbsArticleNameEditTxt.setText(bundle.getString(descriptionKey) ?: "")
            val quantity = try {
                bundle.getString(quantityKey)?.toInt() ?: MIN_QUANTITY
            } catch (_: NumberFormatException) {
                MIN_QUANTITY
            }
            this.quantity = quantity
            binding.gbsQuantityEditTxt.setText("${this.quantity}")
            binding.gbsUnitPriceEditTxt.setText(bundle.getString(priceKey) ?: "")
        }
    }

    private fun restoreKeyboardState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(isKeyboardShowingStateKey)) {
                val viewId = savedInstanceState.getInt(focusedViewIdKey, View.NO_ID)
                val editText = when (viewId) {
                    binding.gbsArticleNameEditTxt.id -> binding.gbsArticleNameEditTxt
                    binding.gbsUnitPriceEditTxt.id -> binding.gbsUnitPriceEditTxt
                    else -> null
                }
                editText?.requestFocus()
                editText?.post { editText.showKeyboard() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            globalLayoutListener?.let {
                binding.root.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            }
        }
    }


    private fun addGlobalViewTreeObserver() {
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight = binding.root.rootView?.height ?: 0
            val keypadHeight = screenHeight - r.bottom
            val imeVisible = keypadHeight > screenHeight * keyboardThresholdRatio
            isKeyboardShowing = imeVisible
        }
        binding.root.viewTreeObserver?.addOnGlobalLayoutListener(globalLayoutListener)
    }


    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onStart() {
        super.onStart()
        presenter?.start()
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

    override fun onResume() {
        super.onResume()
        if (resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet)) {
            ViewCompat.setAccessibilityPaneTitle(binding.root, getString(R.string.gbs_edit_article))
        }
    }

    private fun bindUI(lineItem: LineItem) {
        binding.gbsDropDownSelectionValue.text = lineItem.currency?.currencyCode

        binding.gbsArticleNameEditTxt.doAfterTextChanged {
            if (binding.gbsNameErrorTextView.visibility != View.INVISIBLE) {
                binding.gbsNameErrorTextView.visibility = View.INVISIBLE
            }
        }

        binding.gbsArticleNameEditTxt.setOnEditorActionListener(editorListener)

        binding.gbsUnitPriceEditTxt.addTextChangedListener(amountWatcher)

        binding.gbsUnitPriceEditTxt.doAfterTextChanged {
            if (binding.gbsPriceErrorTextView.visibility != View.INVISIBLE) {
                binding.gbsPriceErrorTextView.visibility = View.INVISIBLE
            }
        }

        binding.gbsUnitPriceEditTxt.setOnEditorActionListener(editorListener)

        binding.gbsQuantityEditTxt.doAfterTextChanged {
            presenter?.setQuantity(
                try {
                    it.toInt()
                } catch (_: NumberFormatException) {
                    MIN_QUANTITY
                }
            )
        }

        if (MULTIPLE_CURRENCIES_ENABLED) {
            binding.gbsDropDownArrow.visibility = View.VISIBLE
        } else {
            binding.gbsDropDownArrow.visibility = View.GONE
            getBottomSheetItemTitleColor()?.let {
                binding.gbsDropDownSelectionValue.setTextColor(it)
            }

            // Setting large margin to currency label if arrow is hidden to align with + button on UI
            val param = (binding.gbsDropDownSelectionValue.layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(0,0, resources.getDimension(net.gini.android.capture.R.dimen.gc_large).toInt(),0)
            }
            binding.gbsDropDownSelectionValue.layoutParams = param
        }
    }

    private fun setupInputHandlers() {
        binding.gbsCloseBottomSheet.setOnClickListener {
            this.dismiss()
            trackCloseTappedEvent()
        }

        binding.gbsAddQuantity.setOnClickListener {
            if (quantity == QUANTITY_LIMIT) return@setOnClickListener

            quantity += 1
            binding.gbsQuantityEditTxt.setText("$quantity")
        }

        binding.gbsRemoveQuantity.setOnClickListener {
            if (quantity <= 1)
                return@setOnClickListener

            quantity -= 1
            binding.gbsQuantityEditTxt.setText("$quantity")
        }

        binding.gbsCurrenciesDropDown.setOnItemClickListener { _, _, _, _ ->
            binding.gbsDropDownSelectionValue.text = binding.gbsCurrenciesDropDown.text
            selectedCurrency = binding.gbsDropDownSelectionValue.text.toString()
        }

        binding.gbsDropDownSelectionValue.setOnClickListener {
            if (!MULTIPLE_CURRENCIES_ENABLED) return@setOnClickListener

            setUpDropDown()
            if (binding.gbsCurrenciesDropDown.isPopupShowing)
                binding.gbsCurrenciesDropDown.dismissDropDown()
            else binding.gbsCurrenciesDropDown.showDropDown()
        }

        binding.gbsDropDownArrow.setOnClickListener {
            if (!MULTIPLE_CURRENCIES_ENABLED) return@setOnClickListener

            setUpDropDown()
            if (binding.gbsCurrenciesDropDown.isPopupShowing)
                binding.gbsCurrenciesDropDown.dismissDropDown()
            else binding.gbsCurrenciesDropDown.showDropDown()
        }

        binding.gbsSave.setOnClickListener {
            if (selectableLineItem == null)
                return@setOnClickListener

            if (!validateLineItemValues()) {
                return@setOnClickListener
            }

            presenter?.save()
            this.dismiss()
        }
    }

    private fun validateLineItemValues(): Boolean {
        var fieldsAreValid = true

        val editedName = binding.gbsArticleNameEditTxt.text.toString()

        if (presenter?.validateLineItemName(editedName) == false) {
            binding.gbsNameErrorTextView.visibility = View.VISIBLE
            fieldsAreValid = false
        }

        val editedPrice = binding.gbsUnitPriceEditTxt.text.toString()

        if (presenter?.validateLineItemGrossPrice(editedPrice) == false) {
            binding.gbsPriceErrorTextView.visibility = View.VISIBLE
            fieldsAreValid = false
        }

        presenter?.setDescription(editedName)
        presenter?.setGrossPrice(editedPrice)

        return fieldsAreValid
    }

    private fun manageFocuses() {
        binding.gbsArticleNameEditTxt.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                focusedViewId = view.id
                binding.gbsNameTxt.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        net.gini.android.capture.R.color.gc_accent_01
                    )
                )
                binding.gbsArticleNameDivider.visibility = View.VISIBLE
            } else {
                binding.gbsArticleNameDivider.visibility = View.INVISIBLE
                getBottomSheetItemTitleColor()?.let {
                    binding.gbsNameTxt.setTextColor(it)
                }
            }
        }

        binding.gbsUnitPriceEditTxt.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                focusedViewId = view.id
                binding.gbsUnitPriceTxt.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        net.gini.android.capture.R.color.gc_accent_01
                    )
                )
                binding.gbsUnitPriceDivider.visibility = View.VISIBLE
            } else {
                binding.gbsUnitPriceDivider.visibility = View.INVISIBLE
                getBottomSheetItemTitleColor()?.let {
                    binding.gbsUnitPriceTxt.setTextColor(it)
                }
            }
        }
    }

    private fun setUpDropDown() {
        val currenciesList = AmountCurrency.values().map { it.name }
        binding.gbsCurrenciesDropDown.setAdapter(CurrencyAdapter(requireContext().wrappedWithGiniCaptureTheme(),
            R.layout.gbs_item_currency_dropdown,
            if (MULTIPLE_CURRENCIES_ENABLED) currenciesList else listOf(selectedCurrency), selectedCurrency))
    }

    private fun setUpBindings() {
        selectableLineItem?.let {
            bindUI(it.lineItem)
            setupInputHandlers()
            manageFocuses()
        }
    }

    private var presenter: LineItemDetailsScreenContract.Presenter? = null

    override fun showDescription(description: String) {
        if (restoringFromOrientationChange) return
        binding.gbsArticleNameEditTxt.setText(description)
    }

    override fun showQuantity(quantity: Int) {
        if (restoringFromOrientationChange) return
        this.quantity = quantity
        binding.gbsQuantityEditTxt.setText("${this.quantity}")
    }

    override fun showGrossPrice(displayedGrossPrice: String, currency: String) {
        if (restoringFromOrientationChange) return
        binding.gbsUnitPriceEditTxt.setText(displayedGrossPrice)
    }

    override fun showCheckbox(selected: Boolean, quantity: Int, visible: Boolean) {
    }

    override fun showTotalGrossPrice(integralPart: String, fractionalPart: String) {
    }

    override fun enableSaveButton() {
    }

    override fun disableSaveButton() {
    }

    override fun enableInput() {
    }

    override fun disableInput() {
    }

    override fun showReturnReasonDialog(
        reasons: List<GiniCaptureReturnReason>,
        resultCallback: ReturnReasonDialogResultCallback
    ) {

    }

    override fun onSave(selectableLineItem: SelectableLineItem) {
        trackSaveTappedEvent(selectableLineItem)
        setFragmentResult(REQUEST_KEY, Bundle().apply {
            putParcelable(RESULT_KEY, selectableLineItem)
        })
    }

    override fun setPresenter(presenter: LineItemDetailsScreenContract.Presenter) {
        this.presenter = presenter
    }

    @ColorInt
    private fun getBottomSheetItemTitleColor() : Int? {
        return context?.wrappedWithGiniCaptureTheme()?.let {
            val typedArray = it.obtainStyledAttributes(R.styleable.GBSCurrencyStyle)
            typedArray.getColor(R.styleable.GBSCurrencyStyle_gbsBottomSheetItemTitle, net.gini.android.capture.R.color.gc_light_01)
        }

    }

    companion object {
        const val QUANTITY_LIMIT = 1000
        const val MULTIPLE_CURRENCIES_ENABLED = false
        const val REQUEST_KEY = "GBS_DIGITAL_INVOICE_BOTTOM_SHEET_REQUEST_KEY"
        const val RESULT_KEY = "GBS_DIGITAL_INVOICE_BOTTOM_SHEET_RESULT_BUNDLE_KEY"

        fun newInstance(selectableLineItem: SelectableLineItem) =
            DigitalInvoiceBottomSheet().apply {
                val args = Bundle()
                args.putParcelable(ARGS_SELECTABLE_LINE_ITEM, selectableLineItem)
                this.arguments = args
                return this
            }
    }

    private class CurrencyAdapter( private val mContext: Context,
                                      private val viewResourceId: Int,
                                      private val items: List<String>,
                                      private var selectedCurrency: String) : ArrayAdapter<String?>(mContext, viewResourceId, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(viewResourceId, parent, false)
            }
            convertView!!.findViewById<TextView>(R.id.gbs_currency_textview).text = items[position]

            val typedArray = mContext.obtainStyledAttributes(R.styleable.GBSCurrencyStyle)

            if (items[position] == selectedCurrency) {
                convertView.setBackgroundColor(typedArray.getColor(R.styleable.GBSCurrencyStyle_gbsCurrencyPickerItemSelectedColor, net.gini.android.capture.R.color.gc_light_01))
            } else {
                convertView.setBackgroundColor(typedArray.getColor(R.styleable.GBSCurrencyStyle_gbsCurrencyPickerItemBackgroundColor, net.gini.android.capture.R.color.gc_light_01))
            }

            return convertView
        }
    }

    // region Analytics

    private fun trackScreenShownEvent() {
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
    }

    private fun trackCloseTappedEvent() {
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.CLOSE_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
    }

    private fun trackSaveTappedEvent(selectableLineItem: SelectableLineItem) {
        val originalLineItem = originalSelectableLineItem?.lineItem
        val finalLineItem = selectableLineItem.lineItem
        val differenceList =
            originalLineItem?.getDifferences(finalLineItem) ?: emptySet()

        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SAVE_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.ItemsChanged(differenceList)
            )
        )
    }

    // endregion

}
