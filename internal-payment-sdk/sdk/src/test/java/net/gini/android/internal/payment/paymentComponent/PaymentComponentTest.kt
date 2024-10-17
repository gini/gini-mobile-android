package net.gini.android.internal.payment.paymentComponent

import android.content.Context
import android.content.SharedPreferences
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.paymentProvider.PaymentProviderAppColors
import net.gini.android.internal.payment.paymentProvider.getInstalledPaymentProviderApps
import net.gini.android.internal.payment.paymentProvider.getPaymentProviderApps
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.utils.extensions.generateBitmapDrawableIcon
import net.gini.android.merchant.sdk.test.ViewModelTestCoroutineRule
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
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private lateinit var giniPaymentModule: GiniInternalPaymentModule
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val testCoroutineScope =
        TestScope(testCoroutineDispatcher + Job())

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
        playStoreUrl = "",
        gpcSupportedPlatforms = listOf("android"),
        openWithSupportedPlatforms = listOf("android")
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
        playStoreUrl = "",
        gpcSupportedPlatforms = listOf("android"),
        openWithSupportedPlatforms = listOf("android")
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
        playStoreUrl = "",
        gpcSupportedPlatforms = listOf("android"),
        openWithSupportedPlatforms = listOf("android")
    )

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        context = getApplicationContext()
        giniPaymentModule = GiniInternalPaymentModule(context!!, giniHealthAPI)
    }

    @After
    fun tearDown() {
        testCoroutineScope.runTest {
            PaymentComponentPreferences(context!!).apply {
                clearData()
            }
        }
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
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
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
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
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
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
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
    fun `installed payment providers are prioritized to top of list`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentProviderAppList = listOf<PaymentProviderApp>(
            buildPaymentProviderApp(paymentProvider, false),
            buildPaymentProviderApp(paymentProvider1, false),
            buildPaymentProviderApp(paymentProvider2, false),
        )
        val mockedContext = createMockedContextAndSetDependencies(paymentProviderList, paymentProviderAppList)
        mockkStatic(PackageManager::getInstalledPaymentProviderApps)
        every { mockedContext.packageManager.getInstalledPaymentProviderApps() } returns emptyList()

        val preferences: SharedPreferences = mockk()
        every { mockedContext.getSharedPreferences("GiniPaymentPreferences", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString("SDK_LANGUAGE_PREFS_KEY", null) } returns null

        //When
        val paymentComponent = PaymentComponent(mockedContext, giniPaymentModule)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)
            assertThat(validation.paymentProviderApps.indexOfFirst { it.paymentProvider.id == paymentProvider2.id }).isEqualTo(2)
            assertThat(validation.paymentProviderApps.filter { it.isInstalled() }).isEmpty()

            val paymentProviderAppListWithInstalled = listOf<PaymentProviderApp>(
                buildPaymentProviderApp(paymentProvider, false),
                buildPaymentProviderApp(paymentProvider1, false),
                buildPaymentProviderApp(paymentProvider2, true),
            )
            every { mockedContext.packageManager.getPaymentProviderApps(paymentProviderList, mockedContext) } returns paymentProviderAppListWithInstalled

            paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
            val installedIsFirstItemValidation = awaitItem()

            assertThat(installedIsFirstItemValidation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((installedIsFirstItemValidation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)
            assertThat(installedIsFirstItemValidation.paymentProviderApps.indexOfFirst { it.paymentProvider.id == paymentProvider2.id }).isEqualTo(0)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `sorting of installed GPC apps is stable`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentProviderAppList = listOf<PaymentProviderApp>(
            buildPaymentProviderApp(paymentProvider, false),
            buildPaymentProviderApp(paymentProvider1, false),
            buildPaymentProviderApp(paymentProvider2, false),
        )
        val mockedContext = createMockedContextAndSetDependencies(paymentProviderList, paymentProviderAppList)

        val preferences: SharedPreferences = mockk()
        every { mockedContext.getSharedPreferences("GiniPaymentPreferences", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString("SDK_LANGUAGE_PREFS_KEY", null) } returns null

        //When
        val paymentComponent = PaymentComponent(mockedContext, giniPaymentModule)
        paymentComponent.loadPaymentProviderApps()

        // Then
        paymentComponent.paymentProviderAppsFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((validation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)
            assertThat(validation.paymentProviderApps.indexOfFirst { it.paymentProvider.id == paymentProvider1.id }).isEqualTo(1)
            assertThat(validation.paymentProviderApps.indexOfFirst { it.paymentProvider.id == paymentProvider2.id }).isEqualTo(2)
            assertThat(validation.paymentProviderApps.filter { it.isInstalled() }).isEmpty()

            val paymentProviderAppListWithInstalled = listOf<PaymentProviderApp>(
                buildPaymentProviderApp(paymentProvider, false),
                buildPaymentProviderApp(paymentProvider1, true),
                buildPaymentProviderApp(paymentProvider2, true),
            )
            every { mockedContext.packageManager.getPaymentProviderApps(paymentProviderList, mockedContext) } returns paymentProviderAppListWithInstalled

            paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
            val installedIsFirstItemValidation = awaitItem()

            assertThat(installedIsFirstItemValidation).isInstanceOf(PaymentProviderAppsState.Success::class.java)
            assertThat((installedIsFirstItemValidation as PaymentProviderAppsState.Success).paymentProviderApps.size).isEqualTo(3)
            assertThat(installedIsFirstItemValidation.paymentProviderApps.indexOfFirst { it.paymentProvider.id == paymentProvider1.id }).isEqualTo(0)
            assertThat(installedIsFirstItemValidation.paymentProviderApps.indexOfFirst { it.paymentProvider.id == paymentProvider2.id }).isEqualTo(1)

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
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
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

        val paymentComponentPreferences = PaymentComponentPreferences(context!!)
        paymentComponentPreferences.deleteSelectedPaymentProviderId()

        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
        paymentComponent.loadPaymentProviderApps()

        paymentComponent.selectedPaymentProviderAppFlow.test {
            val noPaymentProviderSelectedValidation = awaitItem()

            assertThat(noPaymentProviderSelectedValidation).isInstanceOf(
                SelectedPaymentProviderAppState.NothingSelected::class.java)
            assertThat(paymentComponent.paymentProviderAppsFlow.value).isInstanceOf(PaymentProviderAppsState.Success::class.java)

            //When
            val paymentProviderToBeSelected = (paymentComponent.paymentProviderAppsFlow.value as PaymentProviderAppsState.Success).paymentProviderApps.last()
            paymentComponent.setSelectedPaymentProviderApp(paymentProviderToBeSelected)

            // Then
            val paymentProviderSelectedValidation = awaitItem()
            assertThat(paymentProviderSelectedValidation).isInstanceOf(
                SelectedPaymentProviderAppState.AppSelected::class.java)
            assertThat((paymentProviderSelectedValidation as SelectedPaymentProviderAppState.AppSelected).paymentProviderApp.paymentProvider.id).isEqualTo(paymentProviderList.last().id)

            cancelAndConsumeRemainingEvents()
        }

        paymentComponentPreferences.deleteSelectedPaymentProviderId()
    }

    @Test
    fun `restores previously selected payment provider`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2,
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentComponentPreferences = PaymentComponentPreferences(context!!)
        paymentComponentPreferences.deleteSelectedPaymentProviderId()

        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
        paymentComponent.loadPaymentProviderApps()

        paymentComponent.selectedPaymentProviderAppFlow.test {
            val noPaymentProviderSelectedValidation = awaitItem()

            assertThat(noPaymentProviderSelectedValidation).isInstanceOf(
                SelectedPaymentProviderAppState.NothingSelected::class.java)
            assertThat(paymentComponent.paymentProviderAppsFlow.value).isInstanceOf(PaymentProviderAppsState.Success::class.java)

            val paymentProviderToBeSelected = (paymentComponent.paymentProviderAppsFlow.value as PaymentProviderAppsState.Success).paymentProviderApps.last()
            paymentComponent.setSelectedPaymentProviderApp(paymentProviderToBeSelected)

            cancelAndConsumeRemainingEvents()
        }

        // When
        val newPaymentComponent = PaymentComponent(context!!, giniPaymentModule)
        newPaymentComponent.loadPaymentProviderApps()

        paymentComponent.selectedPaymentProviderAppFlow.test {
            // Then
            val paymentProviderSelectedValidation = awaitItem()
            assertThat(paymentProviderSelectedValidation).isInstanceOf(
                SelectedPaymentProviderAppState.AppSelected::class.java)
            assertThat((paymentProviderSelectedValidation as SelectedPaymentProviderAppState.AppSelected).paymentProviderApp.paymentProvider.id).isEqualTo(paymentProviderList.last().id)

            cancelAndConsumeRemainingEvents()
        }

        paymentComponentPreferences.deleteSelectedPaymentProviderId()
    }

    @Test
    fun `sort function brings installed GCP apps to the front of the line`() = runTest {
        // Given
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
        val paymentProviderAppList = listOf<PaymentProviderApp>(
            buildPaymentProviderApp(paymentProvider, false),
            buildPaymentProviderApp(paymentProvider1, false),
            buildPaymentProviderApp(paymentProvider2, true),
        )

        assertThat(paymentProviderAppList.indexOfFirst { it.isInstalled()}).isEqualTo(2)

        // When
        val sortedPaymentProviderAppList = paymentComponent.sortPaymentProviderApps(paymentProviderAppList)

        // Then
        assertThat(sortedPaymentProviderAppList.indexOfFirst { it.isInstalled() }).isEqualTo(0)
    }

    @Test
    fun `updates selected payment provider app`() = runTest {
        // Given
        val paymentProviderList = listOf(
            paymentProvider,
            paymentProvider1,
            paymentProvider2
        )

        coEvery { documentManager.getPaymentProviders() } returns Resource.Success(paymentProviderList)

        val paymentComponentPreferences = PaymentComponentPreferences(context!!)
        paymentComponentPreferences.deleteSelectedPaymentProviderId()

        val paymentProviderAppList = listOf<PaymentProviderApp>(
            buildPaymentProviderApp(paymentProvider, false),
            buildPaymentProviderApp(paymentProvider1, false),
            buildPaymentProviderApp(paymentProvider2, false),
        )
        val mockedContext = createMockedContextAndSetDependencies(paymentProviderList, paymentProviderAppList)

        val preferences: SharedPreferences = mockk()
        every { mockedContext.getSharedPreferences("GiniPaymentPreferences", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString("SDK_LANGUAGE_PREFS_KEY", null) } returns null

        val paymentComponent = PaymentComponent(mockedContext, giniPaymentModule)
        paymentComponent.loadPaymentProviderApps()
        paymentComponent.setSelectedPaymentProviderApp(paymentProviderAppList[0])

        paymentComponent.selectedPaymentProviderAppFlow.test {
            var selectedPaymentProviderAppState = awaitItem()

            assertThat(selectedPaymentProviderAppState).isInstanceOf(SelectedPaymentProviderAppState.AppSelected::class.java)
            assertThat((selectedPaymentProviderAppState as SelectedPaymentProviderAppState.AppSelected).paymentProviderApp.isInstalled()).isFalse()

            val paymentProviderAppListWithInstalled = listOf(
                buildPaymentProviderApp(paymentProvider, true),
                buildPaymentProviderApp(paymentProvider1, false),
                buildPaymentProviderApp(paymentProvider2, false),
            )
            every { mockedContext.packageManager.getPaymentProviderApps(paymentProviderList, mockedContext) } returns paymentProviderAppListWithInstalled

            // When
            paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()

            // Then
            selectedPaymentProviderAppState = awaitItem()
            assertThat((selectedPaymentProviderAppState as SelectedPaymentProviderAppState.AppSelected).paymentProviderApp.isInstalled()).isTrue()

            cancelAndConsumeRemainingEvents()
        }

        paymentComponentPreferences.deleteSelectedPaymentProviderId()
    }

    @Test
    fun `rechecks returning user`() = runTest {
        // Given
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
        paymentComponent.shouldCheckReturningUser = true

        paymentComponent.checkReturningUser()

        paymentComponent.returningUserFlow.test {
            assertThat(awaitItem()).isEqualTo(false)

            // When
            paymentComponent.paymentComponentPreferences.saveReturningUser()
            paymentComponent.checkReturningUser()

            // Then
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `calls to save returning user`() = runTest {
        // Given
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
        paymentComponent.shouldCheckReturningUser = true

        paymentComponent.checkReturningUser()

        paymentComponent.returningUserFlow.test {
            assertThat(awaitItem()).isEqualTo(false)

            // When
            paymentComponent.onPayInvoiceClicked("123")
            paymentComponent.checkReturningUser()

            // Then
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `forwards onPay call to listener`() = runTest {
        // Given
        val paymentComponent = PaymentComponent(context!!, giniPaymentModule)
        paymentComponent.listener = mockk(relaxed = true)

        // When
        paymentComponent.onPayInvoiceClicked("123")

        // Then
        verify { paymentComponent.listener?.onPayInvoiceClicked("123") }
    }
}
