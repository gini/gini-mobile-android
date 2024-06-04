package net.gini.android.merchant.sdk.integratedFlow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.merchant.sdk.databinding.GmsFragmentContainerBinding
import net.gini.android.merchant.sdk.moreinformation.MoreInformationFragment
import net.gini.android.merchant.sdk.paymentComponentBottomSheet.PaymentComponentBottomSheet
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.review.ReviewConfiguration
import net.gini.android.merchant.sdk.review.ReviewFragment
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme


class ContainerFragment private constructor(paymentComponent: PaymentComponent?, private val documentId: String) : Fragment(), BackListener {

    constructor(): this(null, "")
    private var binding: GmsFragmentContainerBinding by autoCleared()
    private val viewModel by viewModels<ContainerViewModel> {
        ContainerViewModel.Factory(paymentComponent)
    }
    private var originalPaymentComponentListener: PaymentComponent.Listener? = null

    private val paymentComponentListener = object: PaymentComponent.Listener {
        override fun onMoreInformationClicked() {
            viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.MoreInformationFragment)
            childFragmentManager.beginTransaction()
                .add(binding.gmsFragmentContainerView.id, MoreInformationFragment.newInstance(paymentComponent!!, this@ContainerFragment))
                .addToBackStack(MoreInformationFragment::class.java.name)
                .commit()
        }

        override fun onBankPickerClicked() {
            viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.BankSelectionBottomSheet)
            BankSelectionBottomSheet.newInstance(paymentComponent!!, backListener = this@ContainerFragment).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
        }

        override fun onPayInvoiceClicked(documentId: String) {
            viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.ReviewFragment)
            viewModel.viewModelScope.launch {
                val reviewFragment = paymentComponent?.getPaymentReviewFragment(documentId, ReviewConfiguration())
                reviewFragment?.let {
                    it.setBackListener(this@ContainerFragment)
                    childFragmentManager.beginTransaction()
                        .add(binding.gmsFragmentContainerView.id, it)
                        .addToBackStack(ReviewFragment::class.java.name)
                        .commit()
                }
            }
        }
    }

    private fun handleBackFlow() {
        childFragmentManager.popBackStackImmediate()
        viewModel.popBackStack()

        if (childFragmentManager.backStackEntryCount == 0) {
            when (viewModel.getLastBackstackEntry()) {
                ContainerViewModel.DisplayedScreen.BankSelectionBottomSheet -> {
                    viewModel.paymentComponent?.let {
                        BankSelectionBottomSheet.newInstance(it, this).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
                    }
                }
                ContainerViewModel.DisplayedScreen.Nothing -> {
                    viewModel.paymentComponent?.listener = originalPaymentComponentListener
                    requireActivity().supportFragmentManager.popBackStack()
                }
                ContainerViewModel.DisplayedScreen.PaymentComponentBottomSheet -> PaymentComponentBottomSheet.newInstance(viewModel.paymentComponent, documentId = documentId,this).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
                else -> {

                }
            }
        }
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
        binding = GmsFragmentContainerBinding.inflate(inflater, container, false)
        originalPaymentComponentListener = viewModel.paymentComponent?.listener
        viewModel.paymentComponent?.listener = paymentComponentListener
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.paymentComponent?.selectedPaymentProviderAppFlow?.collect {
                    if (it is SelectedPaymentProviderAppState.AppSelected) {
                        if (viewModel.paymentProviderAppChanged(it.paymentProviderApp)) {
                            handleBackFlow()
                        }
                    }
                }
            }
        }
        PaymentComponentBottomSheet.newInstance(viewModel.paymentComponent, documentId = documentId, backListener = this@ContainerFragment).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
        viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.PaymentComponentBottomSheet)
        return binding.root
    }

    override fun backCalled() {
        handleBackFlow()
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent, documentId: String) = ContainerFragment(paymentComponent, documentId)
    }
}

