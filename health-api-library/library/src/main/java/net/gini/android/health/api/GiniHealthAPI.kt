package net.gini.android.health.api

import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.internal.GiniCoreAPI
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * The [GiniHealthAPI] instance is the main entry point when interacting with the Gini Health API. You must hold a reference
 * to its instance as long as you interact with the API.
 *
 * To configure and create an instance use the [GiniHealthAPIBuilder].
 */
class GiniHealthAPI(
    documentManager: HealthApiDocumentManager,
    credentialsStore: CredentialsStore
): GiniCoreAPI<HealthApiDocumentManager, HealthApiDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)