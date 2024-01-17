package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

import android.app.Activity
import net.gini.android.bank.sdk.capture.util.SimpleBusEventStore
import net.gini.android.bank.sdk.capture.util.OncePerInstallEvent
import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.bank.sdk.capture.util.BusEvent

internal class DigitalOnboardingScreenPresenter(
    activity: Activity,
    view: DigitalOnboardingScreenContract.View,
    private val oncePerInstallEventStore: OncePerInstallEventStore = OncePerInstallEventStore(
        activity
    ),
    private val simpleBusEventStore: SimpleBusEventStore = SimpleBusEventStore(activity)
) : DigitalOnboardingScreenContract.Presenter(activity, view) {

    init {
        view.setPresenter(this)
    }

    override fun dismisOnboarding(doNotShowAnymore: Boolean) {
        if (doNotShowAnymore) {
            oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SHOW_DIGITAL_INVOICE_ONBOARDING)
        }
        simpleBusEventStore.saveEvent(BusEvent.DISMISS_ONBOARDING_FRAGMENT)
        view.close()
    }

    override fun start() {
    }

    override fun stop() {
    }
}