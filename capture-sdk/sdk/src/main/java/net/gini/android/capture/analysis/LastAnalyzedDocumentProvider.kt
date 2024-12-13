package net.gini.android.capture.analysis

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty

class LastAnalyzedDocumentProvider(
    backgroundDispatcher: CoroutineDispatcher,
    private val userAnalyticsEventTracker: UserAnalyticsEventTracker,
) {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    val data: MutableStateFlow<RemoteAnalyzedDocument?> = MutableStateFlow(null)

    fun provide(): RemoteAnalyzedDocument? = data.value

    fun update(document: RemoteAnalyzedDocument) {
        userAnalyticsEventTracker
            .setEventSuperProperty(UserAnalyticsEventSuperProperty.AnalyzedDocumentId(document.giniApiDocumentId))
        coroutineScope.launch { data.emit(document) }
    }

    fun clear() {
        coroutineScope.launch { data.emit(null) }
    }
}
