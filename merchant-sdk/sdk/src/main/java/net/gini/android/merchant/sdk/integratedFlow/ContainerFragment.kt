package net.gini.android.merchant.sdk.integratedFlow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.merchant.sdk.databinding.GmsFragmentContainerBinding
import net.gini.android.merchant.sdk.moreinformation.MoreInformationFragment
import net.gini.android.merchant.sdk.paymentComponentBottomSheet.PaymentComponentBottomSheet
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme


class ContainerFragment private constructor(private val paymentComponent: PaymentComponent?) : Fragment(), BackListener {

//    constructor(): this()
    private var binding: GmsFragmentContainerBinding by autoCleared()
    private val viewModel by viewModels<ContainerViewModel> {
        ContainerViewModel.Factory(paymentComponent)
    }
    private lateinit var navController: NavController
    private var originalPaymentComponentListener: PaymentComponent.Listener? = null

    private val paymentComponentListener = object: PaymentComponent.Listener {
        override fun onMoreInformationClicked() {
            navController.navigate(R.id.moreInformationFragment)
        }

        override fun onBankPickerClicked() {
            TODO("Not yet implemented")
        }

        override fun onPayInvoiceClicked(documentId: String) {
            TODO("Not yet implemented")
        }
    }

    private val backPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = ContainerFragmentFactory(viewModel.paymentComponentFlow.value, this)
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressHandler)
        super.onCreate(savedInstanceState)
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
        navController = (childFragmentManager.fragments[0]).findNavController()
        parentFragmentManager.beginTransaction()
            .setPrimaryNavigationFragment(this)
            .commit()
        originalPaymentComponentListener = paymentComponent?.listener
        paymentComponent?.listener = paymentComponentListener
        return binding.root
    }

    internal class ContainerFragmentFactory(
        private val paymentComponent: PaymentComponent?,
        private val backListener: BackListener
    ): FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            when (className) {
                PaymentComponentBottomSheet::class.java.name -> {
                    return PaymentComponentBottomSheet.newInstance(paymentComponent).apply {
                        setBackListener(backListener)
                    }
                }
                MoreInformationFragment::class.java.name -> {
                    return MoreInformationFragment.newInstance(paymentComponent)
                }
                BankSelectionBottomSheet::class.java.name -> {
                    return BankSelectionBottomSheet.newInstance(paymentComponent!!)
                }
                else -> return super.instantiate(classLoader, className)
            }
        }
    }

//    class CustomNavHostFragment: NavHostFragment() {
//        override fun onCreateNavHostController(navHostController: NavHostController) {
////            super.onCreateNavHostController(navHostController)
//            Log.e("", "----- creating custom nav host fragment")
////            navHostController.setOnBackPressedDispatcher(OnBackPressedDispatcher {
////
////                Log.e("", "----- in on back pressed dispatcher")
////            })
//            super.onCreateNavHostController(navHostController)
//        }
//    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent) = ContainerFragment(paymentComponent)
    }

    override fun backCalled() {

    }
}

