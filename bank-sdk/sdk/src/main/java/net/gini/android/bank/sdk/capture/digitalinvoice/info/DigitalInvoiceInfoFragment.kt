package net.gini.android.bank.sdk.capture.digitalinvoice.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.databinding.GbsFragmentDigitalInvoiceInfoBinding

/**
 * Created by Sergiu Ciuperca.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
class DigitalInvoiceInfoFragment : Fragment() {

    companion object {
        @JvmStatic
        fun createInstance() = DigitalInvoiceInfoFragment()
    }

    private var binding by autoCleared<GbsFragmentDigitalInvoiceInfoBinding>()
    var listener: DigitalInvoiceInfoFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GbsFragmentDigitalInvoiceInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.fade)
        exitTransition = enterTransition
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInputHandlers()
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
        binding.closeButton.setOnClickListener {
            listener?.onCloseInfo()
        }
    }
}