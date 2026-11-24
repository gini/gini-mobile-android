package net.gini.android.capture.analysis

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LastAnalyzedDocumentProviderTest {

    private val userAnalyticsEventTracker = mockk<UserAnalyticsEventTracker>(relaxed = true)
    private val provider = LastAnalyzedDocumentProvider(UnconfinedTestDispatcher(), userAnalyticsEventTracker)
    private val document = RemoteAnalyzedDocument(giniApiDocumentId = "document-id", filename = "invoice.pdf")

    @Test
    fun `provides no document initially`() {
        assertThat(provider.provide()).isNull()
        assertThat(provider.data.value).isNull()
    }

    @Test
    fun `provides the document it was updated with`() {
        provider.update(document)

        assertThat(provider.provide()).isEqualTo(document)
        assertThat(provider.data.value).isEqualTo(document)
    }

    @Test
    fun `sets the analyzed document id as analytics super property on update`() {
        provider.update(document)

        verify {
            userAnalyticsEventTracker.setEventSuperProperty(
                UserAnalyticsEventSuperProperty.AnalyzedDocumentId("document-id")
            )
        }
    }

    @Test
    fun `provides no document after it was cleared`() {
        provider.update(document)

        provider.clear()

        assertThat(provider.provide()).isNull()
        assertThat(provider.data.value).isNull()
    }
}
