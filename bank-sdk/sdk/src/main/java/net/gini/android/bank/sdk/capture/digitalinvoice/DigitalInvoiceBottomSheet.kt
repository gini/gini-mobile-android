package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.ActionBar
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.details.*
import net.gini.android.bank.sdk.databinding.GbsEditItemBottomSheetBinding
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.network.model.GiniCaptureReturnReason

private const val EXTRA_IN_SELECTABLE_LINE_ITEM = "EXTRA_IN_SELECTABLE_LINE_ITEM"


class DigitalInvoiceBottomSheet : BottomSheetDialogFragment(), LineItemDetailsScreenContract.View,
    LineItemDetailsFragmentInterface {


    private lateinit var binding: GbsEditItemBottomSheetBinding
    private var selectableLineItem: SelectableLineItem? = null
    private var quantity: Int = 1

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

    override fun getTheme(): Int = R.style.GiniCaptureTheme_DigitalInvoice_Edit_BottomSheetDialog

    private fun createPresenter(activity: Activity) = LineItemDetailsScreenPresenter(
        activity, this,
        selectableLineItem!!
    )

    private fun initListener() {
        if (activity is LineItemDetailsFragmentListener) {
            listener = activity as LineItemDetailsFragmentListener?
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
    ): View {
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

        selectableLineItem?.let {
            bindUI(it.lineItem)
            setupInputHandlers()
            manageFocuses()
        }
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
        val spinnerAdapter = ArrayAdapter<AmountCurrency>(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            AmountCurrency.values()
        )
        binding.gbsCurrenciesDropDown.setAdapter(spinnerAdapter)
        binding.gbsDropDownSelectionValue.text = lineItem.currency?.currencyCode

        binding.gbsArticleNameEditTxt.doAfterTextChanged {
            presenter?.setDescription(it)
        }

        binding.gbsUnitPriceEditTxt.doAfterTextChanged {
            presenter?.setGrossPrice(it)
        }

        binding.gbsQuantityEditTxt.doAfterTextChanged {
            presenter?.setQuantity(
                try {
                    it.toInt()
                } catch (_: NumberFormatException) {
                    MIN_QUANTITY
                }
            )
        }
    }

    private fun setupInputHandlers() {
        binding.gbsCloseBottomSheet.setOnClickListener {
            this.dismiss()
        }

        binding.gbsAddQuantity.setOnClickListener {
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
        }


        binding.gbsDropDownSelectionValue.setOnClickListener {
            if (binding.gbsCurrenciesDropDown.isPopupShowing)
                binding.gbsCurrenciesDropDown.dismissDropDown()
            else binding.gbsCurrenciesDropDown.showDropDown()
        }

        binding.gbsDropDownArrow.setOnClickListener {
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
                        R.color.Accent_01
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
                        R.color.Accent_01
                    )
                )
                binding.gbsUnitPriceDivider.visibility = View.VISIBLE
                binding.gbsDropDownSelectionValue.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.Accent_01
                    )
                )
            } else {
                binding.gbsUnitPriceDivider.visibility = View.INVISIBLE
                binding.gbsUnitPriceTxt.setTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Body2)
                binding.gbsDropDownSelectionValue.setTextAppearance(R.style.Root_GiniCaptureTheme_Typography_Subtitle1)
            }
        }
    }


    companion object {
        fun newInstance(selectableLineItem: SelectableLineItem) =
            DigitalInvoiceBottomSheet().apply {
                val args = Bundle()
                args.putParcelable(EXTRA_IN_SELECTABLE_LINE_ITEM, selectableLineItem)
                this.arguments = args
                return this
            }
    }

    override var listener: LineItemDetailsFragmentListener?
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
}
