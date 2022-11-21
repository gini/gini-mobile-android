package net.gini.android.core.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.apimodels.SessionToken

/**
 * Interface for managing [Session]s.
 *
 * Implement this interface and pass it to the API builder to provide your own session management.
 */
interface SessionManager {
    suspend fun getSession(): Resource<Session>
}
