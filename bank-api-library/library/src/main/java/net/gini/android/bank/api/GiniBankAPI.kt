package net.gini.android.bank.api

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.internal.KGiniCoreAPI

class GiniBankAPI(
    documentManager: BankApiDocumentManager,
    credentialsStore: CredentialsStore? = null
): KGiniCoreAPI<BankApiDocumentManager, BankApiDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)