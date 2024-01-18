package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.databinding.GbsFragmentDigitalInvoiceOnboardingBinding
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import net.gini.android.capture.view.InjectedViewAdapterHolder

/**
 * Created by Alpar Szotyori on 14.10.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * You should show the `DigitalInvoiceOnboardingFragment` when the
 * [DigitalInvoiceFragmentListener.showOnboarding()] is called.
 *
 * Include the `DigitalInvoiceOnboardingFragment` into your layout by using the [DigitalInvoiceOnboardingFragment.createInstance()] factory method to create
 * an instance and display it using the [androidx.fragment.app.FragmentManager].
 *
 * A [DigitalInvoiceOnboardingFragmentListener] instance must be available before the `DigitalInvoiceOnboardingFragment` is attached to an activity. Failing
 * to do so will throw an exception. The listener instance can be provided either implicitly by making the hosting Activity implement the
 * [DigitalInvoiceOnboardingFragmentListener] interface or explicitly by setting the listener using [DigitalInvoiceOnboardingFragment.listener].
 *
 * Your Activity is automatically set as the listener in [DigitalInvoiceOnboardingFragment.onCreate()].
 *
 * ### Customizing the Digital Invoice Onboarding Screen
 *
 * TODO: PPL-14: Customization guide for return assistant - Android
 */
class DigitalInvoiceOnboardingFragment : Fragment(), DigitalOnboardingScreenContract.View {

    private var binding by autoCleared<GbsFragmentDigitalInvoiceOnboardingBinding>()

    private var presenter: DigitalOnboardingScreenContract.Presenter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GbsFragmentDigitalInvoiceOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createPresenter(requireActivity())
        enterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.fade)
        exitTransition = enterTransition

    }

    private fun createPresenter(activity: Activity) =
        DigitalOnboardingScreenPresenter(
            activity,
            this,
        )

    override fun close() {
        findNavController().popBackStack()
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun setPresenter(presenter: DigitalOnboardingScreenContract.Presenter) {
        this.presenter = presenter
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onStart() {
        super.onStart()
        binding.digitalInvoiceImageContainer.modifyAdapterIfOwned { (it as OnboardingIllustrationAdapter).onVisible() }
        presenter?.start()
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onStop() {
        super.onStop()
        binding.digitalInvoiceImageContainer.modifyAdapterIfOwned { (it as OnboardingIllustrationAdapter).onHidden() }
        presenter?.stop()
    }


    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInputHandlers()
        setupImageIllustrationAdapter()
        setupOnboardingBottomNavigationBar()
    }


    private fun setupImageIllustrationAdapter() {
        if (GiniCapture.hasInstance()) {
            binding.digitalInvoiceImageContainer.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(GiniBank.digitalInvoiceOnboardingIllustrationAdapterInstance) { it.onVisible() }
        }
    }

    private fun setupOnboardingBottomNavigationBar() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {

            binding.doneButton.visibility = View.INVISIBLE
            binding.doneButton.isEnabled = false

            binding.gbsInjectedNavigationBarContainerBottom.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(
                    GiniBank.digitalInvoiceOnboardingNavigationBarBottomAdapterInstance
                ) { injectedViewAdapter ->
                    injectedViewAdapter.setGetStartedButtonClickListener(
                        IntervalClickListener {
                            presenter?.dismisOnboarding(false)
                        }
                    )
                }
        }
    }

    private fun setInputHandlers() {
        binding.doneButton.setOnClickListener {
            presenter?.dismisOnboarding(false)
        }
    }
}
