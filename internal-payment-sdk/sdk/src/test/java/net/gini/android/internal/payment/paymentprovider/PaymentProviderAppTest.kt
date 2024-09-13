package net.gini.android.internal.payment.paymentprovider

import android.content.ComponentName
import android.content.Intent
import android.content.pm.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.gini.android.health.api.models.PaymentProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpár Szotyori on 09.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

private val installedPaymentProviderAppsFixture = listOf(
    InstalledPaymentProviderApp(
        packageName = "net.gini.android.bank.exampleapp1",
        version = "1.2.3",
        launchIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            component = ComponentName("net.gini.android.bank.exampleapp1.screens.main", "MainScreenActivity")
        }
    ),
    InstalledPaymentProviderApp(
        packageName = "net.gini.android.bank.exampleapp2",
        version = "3.4.5",
        launchIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            component = ComponentName("net.gini.android.bank.exampleapp2.screens.main", "MainScreenActivity")
        }
    )
)

val resolveInfosFixture = installedPaymentProviderAppsFixture.map { installedPaymentProviderApp ->
    ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = installedPaymentProviderApp.packageName
            }
            packageName = installedPaymentProviderApp.launchIntent.component!!.packageName
            name = installedPaymentProviderApp.launchIntent.component!!.className
        }
    }
}

val packageInfosFixture = installedPaymentProviderAppsFixture.map { installedPaymentProviderApp ->
    installedPaymentProviderApp.packageName to PackageInfo().apply {
        versionName = installedPaymentProviderApp.version
    }
}

val paymentProvidersFixture = installedPaymentProviderAppsFixture.map { installedPaymentProviderApp ->
    PaymentProvider(
        id = "id",
        name = "name",
        packageName = installedPaymentProviderApp.packageName,
        appVersion = installedPaymentProviderApp.version,
        colors = PaymentProvider.Colors(
            backgroundColorRGBHex = "112233",
            textColoRGBHex = "ffffff"
        ),
        icon = byteArrayOf(),
        gpcSupportedPlatforms = listOf("android"),
        openWithSupportedPlatforms = listOf("android")
    )
}

@RunWith(AndroidJUnit4::class)
class PaymentProviderAppTest {

    private var packageManager: PackageManager? = null
    private var intentQuerySlot: CapturingSlot<Intent>? = null

    @Before
    fun setup() {
        intentQuerySlot = slot()

        packageManager = mockk(relaxed = true)
        every { packageManager!!.queryIntentActivities(capture(intentQuerySlot!!), 0) } returns resolveInfosFixture

        packageInfosFixture.forEach { (packageName, packageInfo) ->
            every { packageManager!!.getPackageInfo(eq(packageName), 0) } returns packageInfo
        }
    }

    @After
    fun tearDown() {
        packageManager = null
        intentQuerySlot = null
    }

    @Test
    fun `queries with correct uri`() {
        // When
        packageManager!!.getInstalledPaymentProviderApps()

        // Then
        intentQuerySlot!!.captured.let { intent ->
            assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
            assertThat(intent.data!!.scheme).isEqualTo("ginipay")
            assertThat(intent.data!!.host).isEqualTo("payment")
            assertThat(intent.data!!.path).isEqualTo("/id")
        }
    }

    @Test
    fun `finds installed payment provider apps`() {
        // When
        val installedPaymentProviderApps = packageManager!!.getInstalledPaymentProviderApps()

        // Then
        assertThat(installedPaymentProviderApps).containsExactlyElementsIn(
            installedPaymentProviderAppsFixture
        )
    }

    @Test
    fun `returns empty list if there are no installed payment provider apps`() {
        // Given
        every { packageManager!!.queryIntentActivities(any(), 0) } returns emptyList()

        // When
        val installedPaymentProviderApps = packageManager!!.getInstalledPaymentProviderApps()

        // Then
        assertThat(installedPaymentProviderApps).isEmpty()
    }

    @Test
    fun `link installed payment provider apps with payment providers`() {
        // Given
        val paymentProviders = listOf(paymentProvidersFixture[1])

        // When
        val installedPaymentProviderApps = packageManager!!.linkInstalledPaymentProviderAppsWithPaymentProviders(paymentProviders)

        // Then
        assertThat(installedPaymentProviderApps).containsExactly(installedPaymentProviderAppsFixture[1] to paymentProvidersFixture[1])
    }

    @Test
    fun `returns empty list if there are no installed bank apps with a corresponding payment provider`() {
        // Given
        val paymentProviders = emptyList<PaymentProvider>()

        // When
        val installedPaymentProviderApps = packageManager!!.linkInstalledPaymentProviderAppsWithPaymentProviders(paymentProviders)

        // Then
        assertThat(installedPaymentProviderApps).isEmpty()
    }

    @Test
    fun `finds installed payment provider app for each payment provider`() {
        // When
        val paymentProviderApps =
            packageManager!!.getPaymentProviderApps(paymentProvidersFixture, ApplicationProvider.getApplicationContext())

        // Then
        assertThat(paymentProviderApps).hasSize(2)
        for (i in paymentProvidersFixture.indices) {
            assertThat(paymentProviderApps[i].name).isEqualTo(paymentProvidersFixture[i].name)
            assertThat(paymentProviderApps[i].icon).isNotNull()
            assertThat(paymentProviderApps[i].colors).isNotNull()
            assertThat(paymentProviderApps[i].paymentProvider).isEqualTo(paymentProvidersFixture[i])
            assertThat(paymentProviderApps[i].installedPaymentProviderApp).isNotNull()
            assertThat(paymentProviderApps[i].installedPaymentProviderApp!!.packageName).isEqualTo(
                paymentProvidersFixture[i].packageName)
            assertThat(paymentProviderApps[i].installedPaymentProviderApp!!.launchIntent).isNotNull()
        }

    }

    @Test
    fun `returns empty list if there are no payment providers`() {
        // Given
        val paymentProviders = emptyList<PaymentProvider>()

        // When
        val paymentProviderApps =
            packageManager!!.getPaymentProviderApps(paymentProviders, ApplicationProvider.getApplicationContext())

        // Then
        assertThat(paymentProviderApps).isEmpty()
    }

    @Test
    fun `creates intent for payment provider app with payment request id`() {
        // Given
        val paymentProviderApps =
            packageManager!!.getPaymentProviderApps(paymentProvidersFixture, ApplicationProvider.getApplicationContext())
        val paymentRequestId = "payment-request-1234"

        // When
        val intent = paymentProviderApps[0].getIntent(paymentRequestId)

        assertThat(intent).isNotNull()
        assertThat(intent!!.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data!!.scheme).isEqualTo("ginipay")
        assertThat(intent.data!!.host).isEqualTo("payment")
        assertThat(intent.data!!.path).isEqualTo("/$paymentRequestId")
    }
}
