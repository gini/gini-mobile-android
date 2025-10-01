package net.gini.android.capture.tracking.useranalytics

import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty


interface UserAnalyticsEventTracker {

    fun setEventSuperProperty(property: UserAnalyticsEventSuperProperty): Boolean

    fun setEventSuperProperty(property: Set<UserAnalyticsEventSuperProperty>): Boolean

    fun setUserProperty(userProperty: UserAnalyticsUserProperty): Boolean

    fun setUserProperty(userProperties: Set<UserAnalyticsUserProperty>): Boolean

    fun trackEvent(eventName: UserAnalyticsEvent): Boolean

    fun trackEvent(
        eventName: UserAnalyticsEvent,
        properties: Set<UserAnalyticsEventProperty>
    ): Boolean

    fun flushEvents(): Boolean
}








