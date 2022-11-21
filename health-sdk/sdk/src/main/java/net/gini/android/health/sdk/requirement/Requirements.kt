package net.gini.android.health.sdk.requirement

import android.content.pm.PackageManager
import net.gini.android.core.api.Resource
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.bank.getInstalledBankApps
import net.gini.android.health.sdk.review.bank.getInstalledBankAppsWhichHavePaymentProviders

/**
 * The [Requirement] types that are checked as preconditions for Review Screen.
 */
sealed class Requirement {
    /**
     * There's no Bank app on the device which accepts a gini pay Uri.
     */
    object NoBank : Requirement()
}

internal interface RequirementCheckSync {
    fun check(): Requirement?
}

internal interface RequirementCheck {
    suspend fun check(): Requirement?
}

internal class RequirementsChecker(
    private val checks: List<RequirementCheck>,
    private val checksSync: List<RequirementCheckSync>,
) {

    suspend fun checkRequirements(): List<Requirement> =
        (checks.mapNotNull { it.check() } + checksSync.mapNotNull { it.check() })
            .distinct()

    companion object {
        fun withDefaultRequirements(giniHealth: GiniHealth, packageManager: PackageManager) = RequirementsChecker(
            checks = listOf(
                AtLeastOneInstalledBankAppHasPaymentProviderRequirement(giniHealth, packageManager)
            ),
            checksSync = listOf(
                AtLeastOneInstalledBankAppRequirement(packageManager)
            )
        )
    }
}

internal class AtLeastOneInstalledBankAppRequirement(private val packageManager: PackageManager) :
    RequirementCheckSync {

    override fun check(): Requirement? =
        if (packageManager.getInstalledBankApps().isEmpty()) Requirement.NoBank else null

}

internal class AtLeastOneInstalledBankAppHasPaymentProviderRequirement(
    private val giniHealth: GiniHealth,
    private val packageManager: PackageManager,
) : RequirementCheck {

    override suspend fun check(): Requirement? {
        return when (val paymentProvidersResource = giniHealth.giniHealthAPI.documentManager.getPaymentProviders()) {
            is Resource.Cancelled -> null
            is Resource.Error -> Requirement.NoBank
            is Resource.Success -> {
                val availableBankApps =
                    packageManager.getInstalledBankAppsWhichHavePaymentProviders(paymentProvidersResource.data)
                if (availableBankApps.isEmpty()) {
                    Requirement.NoBank
                } else {
                    null
                }
            }
        }
    }

}