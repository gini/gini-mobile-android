package net.gini.android.health.api

import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.internal.GiniCoreAPI
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

class GiniHealthAPI(
    documentManager: HealthApiDocumentManager,
    credentialsStore: CredentialsStore? = null
): GiniCoreAPI<HealthApiDocumentManager, HealthApiDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)