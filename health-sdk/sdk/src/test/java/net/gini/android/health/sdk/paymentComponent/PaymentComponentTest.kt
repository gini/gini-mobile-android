package net.gini.android.health.sdk.paymentComponent

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.health.sdk.paymentprovider.getPaymentProviderApps
import net.gini.android.health.sdk.review.ReviewConfiguration
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.health.sdk.util.extensions.generateBitmapDrawableIcon
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PaymentComponentTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var context: Context? = null
    private var giniHealth: GiniHealth? = null
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }

    private val paymentProvider = PaymentProvider(
        id = "payment provider id",
        name = "payment provider name",
        packageName = "net.gini.android.bank.exampleapp1",
        appVersion = "appVersion",
        colors = PaymentProvider.Colors(
            backgroundColorRGBHex = "112233",
            textColoRGBHex = "ffffff"
        ),
        icon = byteArrayOf(),
        playStoreUrl = ""
        )

    private val paymentProvider1 = PaymentProvider(
        id = "payment provider id 1",
        name = "payment provider name",
        packageName = "net.gini.android.bank.exampleapp2",
        appVersion = "appVersion",
        colors = PaymentProvider.Colors(
            backgroundColorRGBHex = "112233",
            textColoRGBHex = "ffffff"
        ),
        icon = byteArrayOf(),
        playStoreUrl = ""
        )

    private val paymentProvider2 = PaymentProvider(
        id = "payment provider id 2",
        name = "payment provider name",
        packageName = "net.gini.android.bank.exampleapp3",
        appVersion = "appVersion",
        colors = PaymentProvider.Colors(
            backgroundColorRGBHex = "112233",
            textColoRGBHex = "ffffff"
        ),
        icon = byteArrayOf(),
        playStoreUrl = ""
    )

    private val noPlayStoreUrlPaymentProvider = PaymentProvider(
        id = "payment provider id 3",
        name = "payment provider name",
        packageName = "net.gini.android.bank.exampleapp3",
        appVersion = "appVersion",
        colors = PaymentProvider.Colors(
            backgroundColorRGBHex = "112233",
            textColoRGBHex = "ffffff"
        ),
        icon = byteArrayOf(),
    )

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI)
        context = getApplicationContext()
    }

    @After
    fun tearDown() {
        giniHealth = null
        context = null
        unmockkAll()
    }

    private fun createMockedContextAndSetDependencies(paymentProviderList: List<PaymentProvider>, paymentProviderAppsList: List<PaymentProviderApp>): Context {
        val privateContext: Context = mockk()
        val resources: Resources = spyk(context!!.resources)
        val packageManager: PackageManager = mockk(relaxed = true)

        mockkStatic(Context::generateBitmapDrawableIcon)
        mockkStatic(PackageManager::getPaymentProviderApps)
        every { privateContext.applicationContext } returns context
        every { privateContext.packageManager } returns packageManager
        every { privateContext.resources } returns resources
        every { privateContext.generateBitmapDrawableIcon(any(), any()) } returns null
        every { packageManager.getPaymentProviderApps(paymentProviderList, privateContext) } returns paymentProviderAppsList

        return privateContext
    }

    private fun buildPaymentProviderApp(paymentProvider: PaymentProvider, isInstalled: Boolean) = PaymentProviderApp(
        name = paymentProvider.name,
        icon = null,
        colors = PaymentProviderAppColors(
            backgroundColor = 0,
            textColor = 0
        ),
        paymentProvider = paymentProvider,
        installedPaymentProviderApp = if (isInstalled) mockk(relaxed = true) else null
    )

    @Test
    fun `emits error when it cannot load payment providers`() = runTest {
        // Given
        coEvery { documentManager.getPaymentProviders() } returns Resource.Error()

        // When
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            assertThat(awaitItem()).isInstanceOf(PaymentProviderAppsState.Error::class.java)
        }
    }

    @Test
    fun `emits error when loading payment providers is cancelled`() = runTest {
        // Given
        coEvery { documentManager.getPaymentProviders() } returns Resource.Cancelled()

        // When
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            assertThat(awaitItem()).isInstanceOf(PaymentProviderAppsState.Error::class.java)
        }
    }

    @Test
    fun `emits payment provider apps when loaded`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        // When
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `doesn't emit payment providers which are not installed and which don't have a Play Store url`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2,
            noPlayStoreUrlPaymentProvider
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentProviderAppList = listOf<PaymentProviderApp>(
            buildPaymentProviderApp(paymentProvider, true),
            buildPaymentProviderApp(paymentProvider1, true),
            buildPaymentProviderApp(paymentProvider2, true),
            buildPaymentProviderApp(noPlayStoreUrlPaymentProvider, false)
        )
        val mockedContext = createMockedContextAndSetDependencies(paymentProviderList, paymentProviderAppList)

        //When
        val paymentComponent = PaymentComponent(mockedContext, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits payment provider if it's not installed but has Play Store url`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentProviderAppList = listOf(buildPaymentProviderApp(paymentProvider, false))
        val mockedContext = createMockedContextAndSetDependencies(paymentProviderList, paymentProviderAppList)

        //When
        val paymentComponent = PaymentComponent(mockedContext, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(1)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits payment provider if it has Play Store URL and is installed`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2,
            noPlayStoreUrlPaymentProvider
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentProviderAppsList = listOf(
            buildPaymentProviderApp(paymentProvider, true),
            buildPaymentProviderApp(paymentProvider1, true),
            buildPaymentProviderApp(paymentProvider2, true),
            buildPaymentProviderApp(noPlayStoreUrlPaymentProvider, false)
        )
        val mockedContext = createMockedContextAndSetDependencies(paymentProviderList, paymentProviderAppsList)

        //When
        val paymentComponent = PaymentComponent(mockedContext, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `returns empty list when no payment providers installed`() = runTest {
        // Given
        val paymentProviderList = listOf(
            noPlayStoreUrlPaymentProvider
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        //When
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps).isEmpty()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `rechecks which payment provider apps are installed`() = runTest {
        //Given
        val paymentProviderList = mutableListOf(
            paymentProvider
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        //When
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps).isNotEmpty()

            paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
            val validateRecheckEmpty = awaitItem()
            assertThat(validateRecheckEmpty).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validateRecheckEmpty as PaymentProviderAppsState.Success).paymentProviderApps).isNotEmpty()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `sets selected payment provider`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2,
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentComponent = PaymentComponent(context!!, giniHealth!!)
        paymentComponent.loadPaymentProviderApps()

        paymentComponent.selectedPaymentProviderAppFlow.test {
            val noPaymentProviderSelectedValidation = awaitItem()

            assertThat(noPaymentProviderSelectedValidation).isInstanceOf(SelectedPaymentProviderAppState.NothingSelected::class.java)
            assertThat(paymentComponent.paymentProviderAppsFlow.value).isInstanceOf(PaymentProviderAppsState.Success::class.java)

            //When
            val paymentProviderToBeSelected = (paymentComponent.paymentProviderAppsFlow.value as PaymentProviderAppsState.Success).paymentProviderApps.last()
            paymentComponent.setSelectedPaymentProviderApp(paymentProviderToBeSelected)

            // Then
            val paymentProviderSelectedValidation = awaitItem()
            assertThat(paymentProviderSelectedValidation).isInstanceOf(SelectedPaymentProviderAppState.AppSelected::class.java)
            assertThat((paymentProviderSelectedValidation as SelectedPaymentProviderAppState.AppSelected).paymentProviderApp.paymentProvider.id).isEqualTo(paymentProviderList.last().id)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `throws exception when trying to create ReviewFragment if no payment provider app is set`() = runTest {
        // Given
        val reviewConfiguration: ReviewConfiguration = mockk(relaxed = true)
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)

        // When trying to instantiate fragment, then exception should be thrown
        paymentComponent.getPaymentReviewFragment("", reviewConfiguration)
    }

    @Test
    fun `instantiates review fragment if payment provider app is set`() = runTest {
        // Given
        val reviewConfiguration: ReviewConfiguration = mockk(relaxed = true)
        val paymentComponent: PaymentComponent = mockk(relaxed = true)
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)

        // When
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        // Then
        assertThat(paymentComponent.getPaymentReviewFragment("", reviewConfiguration)).isInstanceOf(ReviewFragment::class.java)
    }

}