package net.gini.android.merchant.sdk.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.merchant.sdk.api.authorization.model.Session

/**
 * Interface for managing [Session]s.
 *
 * Implement this interface and pass it to the [GiniMerchant] constructor to provide your own session management.
 */
interface SessionManager {
    suspend fun getSession(): Result<Session>
}

internal class HealthApiSessionManagerAdapter(private val sessionManager: SessionManager): net.gini.android.core.api.authorization.SessionManager {

    override suspend fun getSession(): Resource<net.gini.android.core.api.authorization.Session> {
        return try {
            val session = sessionManager.getSession().getOrThrow()
            Resource.Success(
                net.gini.android.core.api.authorization.Session(
                    session.accessToken,
                    session.expirationDate
                )
            )
        } catch (e: Exception) {
            Resource.Error(message = e.message, exception = Exception(e))
        }
    }

}