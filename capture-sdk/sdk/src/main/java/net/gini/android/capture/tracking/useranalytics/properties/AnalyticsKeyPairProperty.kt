package net.gini.android.capture.tracking.useranalytics.properties

abstract class AnalyticsKeyPairProperty(private val key: String, private val value: Any) {
    fun getPair() = key to value
}