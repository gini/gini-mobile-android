package net.gini.android.core.api.internal

import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * The [GiniCoreAPI] instance is the main entry point when interacting with the Gini API. You must hold a reference
 * to its instance as long as you interact with the Gini API.
 *
 * To configure create an instance use the [GiniCoreAPIBuilder].
 *
 * @property documentManager The [DocumentManager] provides high level methods to handle document related tasks easily.
 * @property credentialsStore [CredentialsStore] implementation which is used to store user information. Handy to get information on the "anonymous" user.
 */
abstract class GiniCoreAPI<DM : DocumentManager<DR, E>, DR : DocumentRepository<E>, E : ExtractionsContainer> constructor(
    val documentManager: DM,
    val credentialsStore: CredentialsStore
)
