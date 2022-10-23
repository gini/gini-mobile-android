package net.gini.android.core.api.internal

import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * KGiniCoreAPI
 *
 * @constructor primary constructor description
 * @property documentManager The DocumentTaskManager provides high level methods to handle document related tasks easily.
 * @property credentialsStore CredentialsStore implementation which is used to store user information. Handy to get information on the "anonymous" user.
 */
abstract class GiniCoreAPI <DM : DocumentManager<DR, E>, DR : DocumentRepository<E>, E : ExtractionsContainer> constructor(
    val documentManager: DM,
    val credentialsStore: CredentialsStore
)
