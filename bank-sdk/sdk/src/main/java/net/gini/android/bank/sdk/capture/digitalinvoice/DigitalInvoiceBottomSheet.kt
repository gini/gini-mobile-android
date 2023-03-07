package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.details.*
import net.gini.android.bank.sdk.capture.util.amountWatcher
import net.gini.android.bank.sdk.capture.util.hideKeyboard
import net.gini.android.bank.sdk.databinding.GbsEditItemBottomSheetBinding
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.network.model.GiniCaptureReturnReason

private const val EXTRA_IN_SELECTABLE_LINE_ITEM = "EXTRA_IN_SELECTABLE_LINE_ITEM"

/**
 * Internal use only.
 */
internal class DigitalInvoiceBottomSheet : BottomSheetDialogFragment(), LineItemDetailsScreenContract.View,
    LineItemDetailsInterface {

    private lateinit var binding: GbsEditItemBottomSheetBinding
    private var selectableLineItem: SelectableLineItem? = null
    private var quantity: Int = 1
    private val editorListener = TextView.OnEditorActionListener { v, actionId, event ->
        v.clearFocus()
        v.hideKeyboard()
        true
    }

    private var selectedCurrency = "EUR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectableLineItem = it.getParcelable(EXTRA_IN_SELECTABLE_LINE_ITEM)
            activity?.let { activity ->
                createPresenter(activity)
                initListener()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            activity?.let {
                binding = GbsEditItemBottomSheetBinding.inflate(it.layoutInflater, null, false)

                val builder = AlertDialog.Builder(context)
                builder.setView(binding.root)
                setUpBindings()

                return builder.create()
            }
        }

       return super.onCreateDialog(savedInstanceState)
    }

    override fun getTheme(): Int {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            return super.getTheme()
        }
        return R.style.GiniCaptureTheme_DigitalInvoice_Edit_BottomSheetDialog

    }

    private fun createPresenter(activity: Activity) = LineItemDetailsScreenPresenter(
        activity, this,
        selectableLineItem!!
    )

    private fun initListener() {
        if (activity is LineItemDetailsListener) {
            listener = activity as LineItemDetailsListener?
        } else checkNotNull(listener) {
            ("LineItemDetailsFragmentListener not set. "
                    + "You can set it with LineItemDetailsFragmentListener#setListener() or "
                    + "by making the host activity implement the LineItemDetailsFragmentListener.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        binding = GbsEditItemBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener {
            val bottomSheetInternal = (it as? BottomSheetDialog)?.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheetInternal?.let {
                BottomSheetBehavior.from(bottomSheetInternal).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpBindings()
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

    private fun bindUI(lineItem: LineItem) {
        binding.gbsDropDownSelectionValue.text = lineItem.currency?.currencyCode

        binding.gbsArticleNameEditTxt.doAfterTextChanged {
            presenter?.setDescription(it)
        }

        binding.gbsArticleNameEditTxt.setOnEditorActionListener(editorListener)

        binding.gbsUnitPriceEditTxt.addTextChangedListener(amountWatcher)

        binding.gbsUnitPriceEditTxt.doAfterTextChanged {
            presenter?.setGrossPrice(it)
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

        if (GiniBank.multipleCurrenciesEnabled) {
            binding.gbsDropDownArrow.visibility = View.VISIBLE
        } else {
            binding.gbsDropDownArrow.visibility = View.GONE
            context?.let {
                val typedArray = it.obtainStyledAttributes(R.styleable.GBSCurrencyStyle)
                binding.gbsDropDownSelectionValue.setTextColor(typedArray.getColor(R.styleable.GBSCurrencyStyle_gbsBottomSheetItemTitle, R.color.gc_light_01))
            }

            // Setting large margin to currency label if arrow is hidden to align with + button on UI
            val param = (binding.gbsDropDownSelectionValue.layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(0,0, resources.getDimension(R.dimen.gc_large).toInt(),0)
            }
            binding.gbsDropDownSelectionValue.layoutParams = param
        }
    }

    private fun setupInputHandlers() {
        binding.gbsCloseBottomSheet.setOnClickListener {
            this.dismiss()
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
            if (!GiniBank.multipleCurrenciesEnabled) return@setOnClickListener

            setUpDropDown()
            if (binding.gbsCurrenciesDropDown.isPopupShowing)
                binding.gbsCurrenciesDropDown.dismissDropDown()
            else binding.gbsCurrenciesDropDown.showDropDown()
        }

        binding.gbsDropDownArrow.setOnClickListener {
            if (!GiniBank.multipleCurrenciesEnabled) return@setOnClickListener

            setUpDropDown()
            if (binding.gbsCurrenciesDropDown.isPopupShowing)
                binding.gbsCurrenciesDropDown.dismissDropDown()
            else binding.gbsCurrenciesDropDown.showDropDown()
        }

        binding.gbsSave.setOnClickListener {
            if (selectableLineItem == null)
                return@setOnClickListener

            presenter?.save()
            this.dismiss()
        }
    }

    private fun manageFocuses() {
        binding.gbsArticleNameEditTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.gbsNameTxt.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gc_accent_01
                    )
                )
                binding.gbsArticleNameDivider.visibility = View.VISIBLE
            } else {
                binding.gbsArticleNameDivider.visibility = View.INVISIBLE
                binding.gbsNameTxt.setTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Body2)
            }
        }

        binding.gbsUnitPriceEditTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.gbsUnitPriceTxt.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gc_accent_01
                    )
                )
                binding.gbsUnitPriceDivider.visibility = View.VISIBLE
            } else {
                binding.gbsUnitPriceDivider.visibility = View.INVISIBLE
                binding.gbsUnitPriceTxt.setTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Body2)
                binding.gbsDropDownSelectionValue.setTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Subtitle1)
            }
        }
    }

    private fun setUpDropDown() {
        val currenciesList = AmountCurrency.values().map { it.name }
        binding.gbsCurrenciesDropDown.setAdapter(CurrencyAdapter(requireActivity(),
            R.layout.gbs_item_currency_dropdown,
            if (GiniBank.multipleCurrenciesEnabled) currenciesList else listOf(currenciesList[0]), selectedCurrency))
    }

    private fun setUpBindings() {
        selectableLineItem?.let {
            bindUI(it.lineItem)
            setupInputHandlers()
            manageFocuses()
        }
    }

    override var listener: LineItemDetailsListener?
        get() = this.presenter?.listener
        set(value) {
            this.presenter?.listener = value
        }

    private var presenter: LineItemDetailsScreenContract.Presenter? = null

    override fun showDescription(description: String) {
        binding.gbsArticleNameEditTxt.setText(description)
    }

    override fun showQuantity(quantity: Int) {
        this.quantity = quantity
        binding.gbsQuantityEditTxt.setText("${this.quantity}")
    }

    override fun showGrossPrice(displayedGrossPrice: String, currency: String) {
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

    override fun setPresenter(presenter: LineItemDetailsScreenContract.Presenter) {
        this.presenter = presenter
    }

    companion object {
        const val QUANTITY_LIMIT = 1000

        fun newInstance(selectableLineItem: SelectableLineItem) =
            DigitalInvoiceBottomSheet().apply {
                val args = Bundle()
                args.putParcelable(EXTRA_IN_SELECTABLE_LINE_ITEM, selectableLineItem)
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

            val typedArray = mContext.theme.obtainStyledAttributes(R.styleable.GBSCurrencyStyle)

            if (items[position] == selectedCurrency) {
                convertView.setBackgroundColor(typedArray.getColor(R.styleable.GBSCurrencyStyle_gbsCurrencyPickerItemSelectedColor, R.color.gc_light_01))
            } else {
                convertView.setBackgroundColor(typedArray.getColor(R.styleable.GBSCurrencyStyle_gbsCurrencyPickerItemBackgroundColor, R.color.gc_light_01))
            }

            return convertView
        }
    }
}
