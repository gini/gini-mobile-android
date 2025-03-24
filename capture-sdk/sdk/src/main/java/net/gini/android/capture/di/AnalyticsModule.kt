package net.gini.android.capture.di

import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.koin.dsl.module

internal val analyticsModule = module {
    factory<UserAnalyticsEventTracker?> { UserAnalytics.getAnalyticsEventTracker() }
}
