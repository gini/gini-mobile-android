package net.gini.android.bank.sdk.di

import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.koin.dsl.module

internal val analyticsModule = module {
    factory<UserAnalyticsEventTracker?> { getGiniCaptureKoin().getOrNull() }
}
