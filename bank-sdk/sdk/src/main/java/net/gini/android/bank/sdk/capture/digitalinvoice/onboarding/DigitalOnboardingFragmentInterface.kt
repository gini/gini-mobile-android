package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

/**
 * Created by Sergiu Ciuperca on 12.04.2021.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

/**
 * Public API of the [DigitalInvoiceOnboardingFragment].
 */
interface DigitalOnboardingFragmentInterface {

    /**
     * Set a listener for onboarding  events.
     */
    var listener: DigitalInvoiceOnboardingFragmentListener?
}