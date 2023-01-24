package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.databinding.GbsFragmentDigitalInvoiceOnboardingBinding
import net.gini.android.capture.GiniCapture

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
class DigitalInvoiceOnboardingFragment : Fragment(), DigitalOnboardingScreenContract.View,
    DigitalOnboardingFragmentInterface {

    companion object {
        @JvmStatic
        fun createInstance() = DigitalInvoiceOnboardingFragment()
    }

    private var binding by autoCleared<GbsFragmentDigitalInvoiceOnboardingBinding>()

    private var presenter: DigitalOnboardingScreenContract.Presenter? = null
    override var listener: DigitalInvoiceOnboardingFragmentListener?
        get() = this.presenter?.listener
        set(value) {
            this.presenter?.listener = value
        }

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
        initListener()
        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.fade)
        exitTransition = enterTransition

    }

    private fun createPresenter(activity: Activity) =
        DigitalOnboardingScreenPresenter(
            activity,
            this,
        )

    private fun initListener() {
        if (activity is DigitalInvoiceOnboardingFragmentListener) {
            listener = activity as DigitalInvoiceOnboardingFragmentListener?
        } else checkNotNull(listener) {
            ("DigitalInvoiceOnboardingFragmentListener not set. "
                    + "You can set it with DigitalInvoiceOnboardingFragmentListener#setListener() or "
                    + "by making the host activity implement the DigitalInvoiceOnboardingFragmentListener.")
        }
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


    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInputHandlers()
        setupBottomNavigationBar()
    }


    private fun setupBottomNavigationBar() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {

            binding.doneButton.visibility = View.INVISIBLE
            binding.doneButton.isEnabled = false

            binding.gbsInjectedNavigationBarContainerBottom?.injectedViewAdapter = GiniBank.digitalInvoiceOnboardingBarBottomAdapter
            GiniBank.digitalInvoiceOnboardingBarBottomAdapter.setGetStartedButtonClickListener {
                presenter?.dismisOnboarding(false)
            }
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onDestroyView() {
        listener = null
        super.onDestroyView()
    }

    private fun setInputHandlers() {
        binding.doneButton.setOnClickListener {
            presenter?.dismisOnboarding(false)
        }
    }
}