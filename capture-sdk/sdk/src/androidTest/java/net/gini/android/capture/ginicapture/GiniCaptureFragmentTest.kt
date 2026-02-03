package net.gini.android.capture.ginicapture

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragment
import net.gini.android.capture.GiniCaptureHelperForInstrumentationTests
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.internal.network.ConfigurationNetworkResult
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.tracking.useranalytics.BufferedUserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter
import net.gini.android.capture.view.DefaultNavigationBarTopAdapter
import net.gini.android.capture.view.InjectedViewAdapterInstance
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import net.gini.android.capture.di.getGiniCaptureKoin
import java.util.UUID

/**
 * Integration test to verify the correct behavior of Analytics.
 * Classes involved
 * - [GiniCaptureFragment]
 * - [GiniCapture]
 * - [GiniCapture.Internal]
 * - [NetworkRequestsManager]
 * - [BufferedUserAnalyticsEventTracker]
 * */

@RunWith(AndroidJUnit4::class)
class GiniCaptureFragmentTest {
    private lateinit var networkRequestsManager: NetworkRequestsManager
    private lateinit var giniCapture: GiniCapture
    private lateinit var giniInternal: GiniCapture.Internal
    private lateinit var memoryStore: ImageMultiPageDocumentMemoryStore
    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
    }

    /**
     * We are using multiple dependencies in different classes, so many mocks are needed
     * to fully test the functionality of AnalyticsTracker, by running the GiniCaptureFragment
     * in Isolation.
     * Mock was needed for [NetworkRequestsManager], [GiniCapture], [GiniCapture.Internal],
     * [ImageMultiPageDocumentMemoryStore], [GiniBankConfigurationProvider]
     * Also, in [GiniCaptureFragment], we are using koin to update
     * the [GiniBankConfigurationProvider], for that we need to load and unload the module,
     * other wise an exception will be thrown from koin for not loaded module.
     * In the end, we need to set the mocked [GiniCapture] instance, and we have a helper class
     * [GiniCaptureHelperForInstrumentationTests] for that.
     *
     * */

    @Before
    fun setUp() {
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))

        networkRequestsManager = mock()
        giniCapture = mock()
        giniInternal = mock()
        memoryStore = mock()

        whenever(giniInternal.networkRequestsManager).thenReturn(networkRequestsManager)
        whenever(giniCapture.internal()).thenReturn(giniInternal)
        whenever(giniCapture.entryPoint).thenReturn(EntryPoint.BUTTON)
        whenever(giniInternal.imageMultiPageDocumentMemoryStore).thenReturn(memoryStore)
        whenever(giniInternal.navigationBarTopAdapterInstance).thenReturn(
            InjectedViewAdapterInstance(DefaultNavigationBarTopAdapter())
        )
        whenever(giniCapture.documentImportEnabledFileTypes).thenReturn(
            DocumentImportEnabledFileTypes.NONE
        )
        whenever(giniCapture.internal().loadingIndicatorAdapterInstance).thenReturn(
            InjectedViewAdapterInstance(DefaultLoadingIndicatorAdapter())
        )

        GiniCaptureHelperForInstrumentationTests.setGiniCaptureInstance(giniCapture)
    }


    /**
     * Unload the koin modules which were loaded in the [setUp].
     * */

    @After
    fun tearDown() = CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))


    @Test
    fun analyticsTracker_shouldBeEmpty_whenUserJourneyDisabled() {

        whenever(networkRequestsManager.getConfigurations(any())).thenReturn(
            CompletableFuture.completedFuture(getMockedConfiguration(userJourneyEnabled = false))
        )

        launchGiniCaptureFragment().use { scenario ->

            scenario.moveToState(Lifecycle.State.STARTED)

            scenario.onFragment { _ ->
                assertThat(getAnalyticsTracker().getTrackers()).isEmpty()
            }
        }
    }

    @Test
    fun savePhotosLocally_shouldBeDisabled_whenConfigurationSetToFalse() {
        whenever(networkRequestsManager.getConfigurations(any())).thenReturn(
            CompletableFuture.completedFuture(
                getMockedConfiguration(
                    userJourneyEnabled = false,
                    savePhotosLocallyEnabled = false
                )
            )
        )

        launchGiniCaptureFragment().use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            val giniBankConfigurationProvider = getGiniCaptureKoin().get<GiniBankConfigurationProvider>()
            val configuration = giniBankConfigurationProvider.provide()

            assertThat(configuration.isSavePhotosLocallyEnabled).isFalse()
        }
    }

    @Test
    fun savePhotosLocally_shouldBeEnabled_whenConfigurationSetToTrue() {
        whenever(networkRequestsManager.getConfigurations(any())).thenReturn(
            CompletableFuture.completedFuture(
                getMockedConfiguration(
                    userJourneyEnabled = false,
                    savePhotosLocallyEnabled = true
                )
            )
        )

        launchGiniCaptureFragment().use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            val giniBankConfigurationProvider = getGiniCaptureKoin().get<GiniBankConfigurationProvider>()
            val configuration = giniBankConfigurationProvider.provide()

            assertThat(configuration.isSavePhotosLocallyEnabled).isTrue()
        }
    }


    @Test
    fun analyticsTracker_shouldNotBeEmpty_whenUserJourneyEnabled() {

        whenever(networkRequestsManager.getConfigurations(any())).thenReturn(
            CompletableFuture.completedFuture(getMockedConfiguration(userJourneyEnabled = true))
        )

        launchGiniCaptureFragment().use { scenario ->

            scenario.moveToState(Lifecycle.State.STARTED)

            scenario.onFragment { _ ->
                assertThat(getAnalyticsTracker().getTrackers()).isNotEmpty()
            }
        }
    }


    private fun getMockedConfiguration(
        userJourneyEnabled: Boolean,
        savePhotosLocallyEnabled: Boolean = false
    ): ConfigurationNetworkResult {
        val testConfig = Configuration(
            id = UUID.randomUUID(),
            clientID = TEST_CLIENT_ID,
            isUserJourneyAnalyticsEnabled = userJourneyEnabled,
            isSkontoEnabled = false,
            isReturnAssistantEnabled = false,
            isTransactionDocsEnabled = false,
            isQrCodeEducationEnabled = false,
            isInstantPaymentEnabled = false,
            isEInvoiceEnabled = false,
            amplitudeApiKey = TEST_API_KEY,
            isSavePhotosLocallyEnabled = savePhotosLocallyEnabled,
            isPaymentDueHintEnabled = false,
            isAlreadyPaidHintEnabled = false,
            isCreditNoteHintEnabled = false
        )

        return ConfigurationNetworkResult(testConfig, UUID.randomUUID())
    }

    private fun getAnalyticsTracker(): BufferedUserAnalyticsEventTracker {
        return UserAnalytics.getAnalyticsEventTracker() as BufferedUserAnalyticsEventTracker
    }

    /**
     * Helper method to launch the [GiniCaptureFragment] in a container,
     * needed in all the tests.
     *
     * */

    private fun launchGiniCaptureFragment(): FragmentScenario<GiniCaptureFragment> {
        return FragmentScenario.launchInContainer(
            fragmentClass = GiniCaptureFragment::class.java,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return GiniCaptureFragment.createInstance().apply {
                        setListener(mock())
                        setBankSDKBridge(mock())
                    }
                }
            }
        )
    }

    companion object {
        private const val TEST_CLIENT_ID = "test-client-id"
        private const val TEST_API_KEY = "test-api-key"
    }
}
