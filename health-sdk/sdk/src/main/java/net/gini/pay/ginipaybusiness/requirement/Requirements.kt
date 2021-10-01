package net.gini.pay.ginipaybusiness.requirement

import android.content.pm.PackageManager
import net.gini.pay.ginipaybusiness.review.bank.getBanks

/**
 * The [Requirement] types that are checked as preconditions for Review Screen.
 */
sealed class Requirement {
    /**
     * There's no Bank app on the device which accepts a gini pay Uri.
     */
    object NoBank : Requirement()
}

internal fun internalCheckRequirements(packageManager: PackageManager): List<Requirement> = mutableListOf<Requirement>().apply {
    if (!atLeastOneBank(packageManager)) add(Requirement.NoBank)
}

private fun atLeastOneBank(packageManager: PackageManager): Boolean = packageManager.getBanks().isNotEmpty()