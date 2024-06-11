package net.gini.android.merchant.sdk.integratedFlow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.add
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme

class ContainerFragment private constructor(paymentComponent: PaymentComponent?, documentId: String) : Fragment(), BackListener {

    constructor(): this(null, "")
    private var binding: GmsFragmentContainerBinding by autoCleared()
    private val viewModel by viewModels<ContainerViewModel> {
        ContainerViewModel.Factory(paymentComponent, documentId)
    }

    private val paymentComponentListener = object: PaymentComponent.Listener {
        override fun onMoreInformationClicked() {
            viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.MoreInformationFragment)
            childFragmentManager.add(
                containerId = binding.gmsFragmentContainerView.id,
                fragment = MoreInformationFragment.newInstance(viewModel.paymentComponent, this@ContainerFragment),
                addToBackStack = true
            )
        }

        override fun onBankPickerClicked() {
            viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.BankSelectionBottomSheet)
            BankSelectionBottomSheet.newInstance(viewModel.paymentComponent!!, backListener = this@ContainerFragment).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
        }

        override fun onPayInvoiceClicked(documentId: String) {
            viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.ReviewFragment)
            viewModel.viewModelScope.launch {
                val reviewFragment = viewModel.paymentComponent?.getPaymentReviewFragment(documentId, ReviewConfiguration())
                reviewFragment?.let {
                    childFragmentManager.add(
                        containerId = binding.gmsFragmentContainerView.id,
                        fragment = it,
                        addToBackStack = true
                    )
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
                    requireActivity().supportFragmentManager.popBackStack()
                }
                ContainerViewModel.DisplayedScreen.PaymentComponentBottomSheet -> PaymentComponentBottomSheet.newInstance(viewModel.paymentComponent, documentId = viewModel.documentId,this).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
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
        if (viewModel.getLastBackstackEntry() is ContainerViewModel.DisplayedScreen.Nothing) {
            showPaymentComponentBottomSheet()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackFlow()
            }
        })
    }

    private fun showPaymentComponentBottomSheet() {
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(
            viewModel.paymentComponent,
            documentId = viewModel.documentId,
            backListener = this@ContainerFragment
        )
        paymentComponentBottomSheet.show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
        viewModel.addToBackStack(ContainerViewModel.DisplayedScreen.PaymentComponentBottomSheet)
    }

    override fun backCalled() {
        handleBackFlow()
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent, documentId: String) = ContainerFragment(paymentComponent, documentId)
    }
}

