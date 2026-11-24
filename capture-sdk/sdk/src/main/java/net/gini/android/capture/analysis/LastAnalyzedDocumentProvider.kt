package net.gini.android.capture.analysis

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty

class LastAnalyzedDocumentProvider(
    backgroundDispatcher: CoroutineDispatcher,
    private val userAnalyticsEventTracker: UserAnalyticsEventTracker,
) {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    private val _data = MutableStateFlow<RemoteAnalyzedDocument?>(null)
    val data: StateFlow<RemoteAnalyzedDocument?> = _data.asStateFlow()

    fun provide(): RemoteAnalyzedDocument? = _data.value

    fun update(document: RemoteAnalyzedDocument) {
        userAnalyticsEventTracker
            .setEventSuperProperty(UserAnalyticsEventSuperProperty.AnalyzedDocumentId(document.giniApiDocumentId))
        coroutineScope.launch { _data.emit(document) }
    }

    fun clear() {
        coroutineScope.launch { _data.emit(null) }
    }
}
