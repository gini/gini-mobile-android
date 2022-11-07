package net.gini.android.bank.api

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.internal.GiniCoreAPI
import net.gini.android.core.api.internal.GiniCoreAPIBuilder

/**
 * The [GiniBankAPI] instance is the main entry point when interacting with the Gini Bank API. You must hold a reference
 * to its instance as long as you interact with the API.
 *
 * To configure and create an instance use the [GiniBankAPIBuilder].
 */
class GiniBankAPI(
    documentManager: BankApiDocumentManager,
    credentialsStore: CredentialsStore
): GiniCoreAPI<BankApiDocumentManager, BankApiDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)