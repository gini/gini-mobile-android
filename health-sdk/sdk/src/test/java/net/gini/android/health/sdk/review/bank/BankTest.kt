package net.gini.android.health.sdk.review.bank

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
import net.gini.android.core.api.models.PaymentProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpár Szotyori on 09.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

val installedBankAppsFixture = listOf(
    InstalledBankApp(
        packageName = "net.gini.android.bank.exampleapp1",
        version = "1.2.3",
        launchIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            component = ComponentName("net.gini.android.bank.exampleapp1.screens.main", "MainScreenActivity")
        }
    ),
    InstalledBankApp(
        packageName = "net.gini.android.bank.exampleapp2",
        version = "3.4.5",
        launchIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            component = ComponentName("net.gini.android.bank.exampleapp2.screens.main", "MainScreenActivity")
        }
    )
)

val resolveInfosFixture = installedBankAppsFixture.map { installedBankApp ->
    ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = installedBankApp.packageName
            }
            packageName = installedBankApp.launchIntent.component!!.packageName
            name = installedBankApp.launchIntent.component!!.className
        }
    }
}

val packageInfosFixture = installedBankAppsFixture.map { installedBankApp ->
    installedBankApp.packageName to PackageInfo().apply {
        versionName = installedBankApp.version
    }
}

val paymentProvidersFixture = installedBankAppsFixture.map { installedBankApp ->
    PaymentProvider(
        id = "id",
        name = "name",
        packageName = installedBankApp.packageName,
        appVersion = installedBankApp.version,
        colors = PaymentProvider.Colors(
            backgroundColorRGBHex = "112233",
            textColoRGBHex = "ffffff"
        ),
        icon = byteArrayOf()
    )
}

@RunWith(AndroidJUnit4::class)
class BankTest {

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
        packageManager!!.getInstalledBankApps()

        // Then
        intentQuerySlot!!.captured.let { intent ->
            assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
            assertThat(intent.data!!.scheme).isEqualTo("ginipay")
            assertThat(intent.data!!.host).isEqualTo("payment")
            assertThat(intent.data!!.path).isEqualTo("/id")
        }
    }

    @Test
    fun `finds installed bank apps`() {
        // When
        val installedBankApps = packageManager!!.getInstalledBankApps()

        // Then
        assertThat(installedBankApps).containsExactlyElementsIn(installedBankAppsFixture)
    }

    @Test
    fun `returns empty list if there are no installed bank apps`() {
        // Given
        every { packageManager!!.queryIntentActivities(any(), 0) } returns emptyList()

        // When
        val installedBankApps = packageManager!!.getInstalledBankApps()

        // Then
        assertThat(installedBankApps).isEmpty()
    }

    @Test
    fun `filters out installed bank apps which don't have a payment provider`() {
        // Given
        val paymentProviders = listOf(paymentProvidersFixture[1])

        // When
        val installedBankApps = packageManager!!.getInstalledBankAppsWhichHavePaymentProviders(paymentProviders)

        // Then
        assertThat(installedBankApps).containsExactly(installedBankAppsFixture[1] to paymentProvidersFixture[1])
    }

    @Test
    fun `returns empty list if there are no installed bank apps with a corresponding payment provider`() {
        // Given
        val paymentProviders = emptyList<PaymentProvider>()

        // When
        val installedBankApps = packageManager!!.getInstalledBankAppsWhichHavePaymentProviders(paymentProviders)

        // Then
        assertThat(installedBankApps).isEmpty()
    }

    @Test
    fun `finds valid bank apps (are installed and have a payment provider)`() {
        // Given
        val paymentProviders = listOf(paymentProvidersFixture[1])

        // When
        val validBankApps =
            packageManager!!.getValidBankApps(paymentProviders, ApplicationProvider.getApplicationContext())

        // Then
        assertThat(validBankApps).hasSize(1)
        assertThat(validBankApps[0].name).isEqualTo(paymentProviders[0].name)
        assertThat(validBankApps[0].packageName).isEqualTo(paymentProviders[0].packageName)
        assertThat(validBankApps[0].version).isEqualTo(installedBankAppsFixture[1].version)
        assertThat(validBankApps[0].icon).isNotNull()
        assertThat(validBankApps[0].colors).isNotNull()
        assertThat(validBankApps[0].paymentProvider).isEqualTo(paymentProviders[0])
    }

    @Test
    fun `returns empty list if there are no valid bank apps`() {
        // Given
        val paymentProviders = emptyList<PaymentProvider>()

        // When
        val validBankApps =
            packageManager!!.getValidBankApps(paymentProviders, ApplicationProvider.getApplicationContext())

        // Then
        assertThat(validBankApps).isEmpty()
    }

    @Test
    fun `creates intent for bank app with payment request id`() {
        // Given
        val validBankApps =
            packageManager!!.getValidBankApps(paymentProvidersFixture, ApplicationProvider.getApplicationContext())
        val paymentRequestId = "payment-request-1234"

        // When
        val intent = validBankApps[0].getIntent(paymentRequestId)

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data!!.scheme).isEqualTo("ginipay")
        assertThat(intent.data!!.host).isEqualTo("payment")
        assertThat(intent.data!!.path).isEqualTo("/$paymentRequestId")
    }
}
