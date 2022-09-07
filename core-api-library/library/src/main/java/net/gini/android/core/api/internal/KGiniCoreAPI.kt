package net.gini.android.core.api.internal

import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.models.ExtractionsContainer

abstract class KGiniCoreAPI <DM : DocumentManager<DR, E>, DR : DocumentRepository<E>, E : ExtractionsContainer> constructor(

    // TODO: add documentation
    val documentManager: DM,
    private val credentialsStore: CredentialsStore? = null
) {

    /**
     * Get the instance of the DocumentManager. The DocumentTaskManager provides high level methods to handle
     * document related tasks easily.
     *
     */

    /**
     * Get the instance of the CredentialsStore implementation which is used to store user information. Handy to get
     * information on the "anonymous" user.
     */
    open fun getCredentialsStore(): CredentialsStore? {
        return credentialsStore
    }
}