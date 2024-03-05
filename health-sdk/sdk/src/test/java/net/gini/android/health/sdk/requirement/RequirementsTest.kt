package net.gini.android.health.sdk.requirement

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.packageInfosFixture
import net.gini.android.health.sdk.paymentprovider.paymentProvidersFixture
import net.gini.android.health.sdk.paymentprovider.resolveInfosFixture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpár Szotyori on 09.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RequirementsTest {

    private var packageManager: PackageManager? = null
    private var giniHealth: GiniHealth? = null
    private var giniHealthAPI: GiniHealthAPI? = null
    private var documentManager: HealthApiDocumentManager? = null

    @Before
    fun setup() {
        packageManager = mockk(relaxed = true)
        every { packageManager!!.queryIntentActivities(any(), 0) } returns resolveInfosFixture

        packageInfosFixture.forEach { (packageName, packageInfo) ->
            every { packageManager!!.getPackageInfo(eq(packageName), 0) } returns packageInfo
        }

        giniHealth = mockk()
        giniHealthAPI = mockk()
        documentManager = mockk()
        every { giniHealth!!.giniHealthAPI } returns giniHealthAPI!!
        every { giniHealthAPI!!.documentManager } returns documentManager!!
        coEvery { documentManager!!.getPaymentProviders() } returns Resource.Success(paymentProvidersFixture)
    }

    @After
    fun tearDown() {
        packageManager = null
        giniHealth = null
        giniHealthAPI = null
        documentManager = null
    }

    @Test
    fun `runs both async and sync checks`() = runTest {
        // Given
        var asyncChecks = false
        var syncChecks = false
        val requirementsChecker = RequirementsChecker(
            checks = listOf(
                object : RequirementCheck {
                    override suspend fun check(): Requirement? {
                        asyncChecks = true
                        return null
                    }
                }
            ),
            checksSync = listOf(
                object : RequirementCheckSync {
                    override fun check(): Requirement? {
                        syncChecks = true
                        return null
                    }
                }
            )
        )

        // When
        requirementsChecker.checkRequirements()

        // Then
        assertThat(asyncChecks).isTrue()
        assertThat(syncChecks).isTrue()
    }

    @Test
    fun `returns empty list if requirements are met`() = runTest {
        // Given
        val requirementsChecker = RequirementsChecker(
            checks = listOf(
                object : RequirementCheck {
                    override suspend fun check(): Requirement? {
                        return null
                    }
                }
            ),
            checksSync = listOf(
                object : RequirementCheckSync {
                    override fun check(): Requirement? {
                        return null
                    }
                }
            )
        )

        // When
        val requirements = requirementsChecker.checkRequirements()

        // Then
        assertThat(requirements).isEmpty()
    }

    @Test
    fun `each requirement may appear only once in the result list`() = runTest {
        // Given
        val requirementsChecker = RequirementsChecker(
            checks = listOf(
                object : RequirementCheck {
                    override suspend fun check(): Requirement? {
                        return Requirement.NoBank
                    }
                }
            ),
            checksSync = listOf(
                object : RequirementCheckSync {
                    override fun check(): Requirement? {
                        return Requirement.NoBank
                    }
                }
            )
        )

        // When
        val requirements = requirementsChecker.checkRequirements()

        // Then
        assertThat(requirements).containsExactly(Requirement.NoBank)
    }

    @Test
    fun `checks that at least one bank app is installed`() = runTest {
        // When
        val requirements =
            RequirementsChecker.withDefaultRequirements(giniHealth!!, packageManager!!).checkRequirements()

        // Then
        assertThat(requirements).isEmpty()
    }

    @Test
    fun `check fails if no bank app is installed`() = runTest {
        // Given
        every { packageManager!!.queryIntentActivities(any(), 0) } returns emptyList()

        // When
        val requirements =
            RequirementsChecker.withDefaultRequirements(giniHealth!!, packageManager!!).checkRequirements()

        // Then
        assertThat(requirements).isNotEmpty()
    }

    @Test
    fun `checks that at least one bank app is installed and has a corresponding payment provider`() = runTest {
        // When
        val requirements =
            RequirementsChecker.withDefaultRequirements(giniHealth!!, packageManager!!).checkRequirements()

        // Then
        assertThat(requirements).isEmpty()
    }

    @Test
    fun `check fails if no installed bank app has a corresponding payment provider`() = runTest {
        // Given
        every { packageManager!!.queryIntentActivities(any(), 0) } returns emptyList()

        coEvery { documentManager!!.getPaymentProviders() } returns Resource.Success(paymentProvidersFixture.subList(0, 1))

        // When
        val requirements =
            RequirementsChecker.withDefaultRequirements(giniHealth!!, packageManager!!).checkRequirements()

        // Then
        assertThat(requirements).isNotEmpty()
    }
}