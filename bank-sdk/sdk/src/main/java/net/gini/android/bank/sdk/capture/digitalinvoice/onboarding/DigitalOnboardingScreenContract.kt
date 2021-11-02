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

    }

    abstract class Presenter(activity: Activity, view: View) :
        GiniCaptureBasePresenter<View>(activity, view), DigitalOnboardingFragmentInterface {
            abstract fun dismisOnboarding(doNotShowAnymore: Boolean)
    }
}