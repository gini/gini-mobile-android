package net.gini.android.bank.sdk.transactiondocs.internal.usecase

import kotlinx.coroutines.flow.first
import net.gini.android.bank.sdk.transactiondocs.internal.GiniTransactionDocsSettings

internal class GetTransactionDocShouldBeAutoAttachedUseCase(
    private val giniTransactionDocsSettings: GiniTransactionDocsSettings,
) {

    suspend operator fun invoke(): Boolean =
        giniTransactionDocsSettings.getAlwaysAttachSetting().first()
}
