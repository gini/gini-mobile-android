package net.gini.android.bank.api

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.internal.GiniCoreAPI

class GiniBankAPI(
    documentManager: BankApiDocumentManager,
    credentialsStore: CredentialsStore
): GiniCoreAPI<BankApiDocumentManager, BankApiDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)