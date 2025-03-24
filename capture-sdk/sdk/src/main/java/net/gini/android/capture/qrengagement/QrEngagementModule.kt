package net.gini.android.capture.qrengagement

import net.gini.android.capture.qrengagement.intent.NavigateBackIntent
import net.gini.android.capture.qrengagement.intent.NextPageIntent
import net.gini.android.capture.qrengagement.intent.PreviousPageIntent
import net.gini.android.capture.qrengagement.intent.SkipIntent
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val QrEngagementModule = module {
    factory { NavigateBackIntent() }
    factory { NextPageIntent() }
    factory { PreviousPageIntent() }
    factory { SkipIntent() }

    viewModel {
        QrEngagementViewModel(
            navigateBackIntent = get(),
            nextPageIntent = get(),
            previousPageIntent = get(),
            skipIntent = get()
        )
    }
}
