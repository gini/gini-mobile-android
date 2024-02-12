package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

import android.app.Activity
import net.gini.android.capture.GiniCaptureBasePresenter
import net.gini.android.capture.GiniCaptureBaseView

/**
 * Created by Sergiu Ciuperca on 12.04.2021.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
interface DigitalOnboardingScreenContract {
    interface View  : GiniCaptureBaseView<Presenter>
    {
        fun close()
    }

    abstract class Presenter(activity: Activity, view: View) :
        GiniCaptureBasePresenter<View>(activity, view) {
            abstract fun dismisOnboarding(doNotShowAnymore: Boolean)
    }
}