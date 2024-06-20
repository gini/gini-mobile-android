package net.gini.android.capture.tracking.useranalytics

import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty


interface UserAnalyticsEventTracker {

    fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty)

    fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>)

    fun setUserProperty(userProperty: UserAnalyticsUserProperty)

    fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>)

    fun trackEvent(eventName: UserAnalyticsEvent)

    fun trackEvent(eventName: UserAnalyticsEvent, properties: Set<UserAnalyticsEventProperty>)
}








