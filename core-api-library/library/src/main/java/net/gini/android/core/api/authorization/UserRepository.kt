package net.gini.android.core.api.authorization

import kotlinx.coroutines.CoroutineScope
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import kotlin.coroutines.CoroutineContext

open class UserRepository(
    override val coroutineContext: CoroutineContext,
    private val userRemoteSource: UserRemoteSource
    ) : CoroutineScope {

    private var session: SessionToken? = null

    //region Public methods
    suspend fun loginUser(userRequestModel: UserRequestModel): Resource<SessionToken?> =
        wrapInResource {
            userRemoteSource.signIn(userRequestModel)
        }

    private suspend fun loginClient(): Resource<SessionToken> =
        wrapInResource {
            userRemoteSource.loginClient()
        }

    suspend fun createUser(userRequestModel: UserRequestModel): Resource<Unit> =
        when (val token = getUserRepositorySession()) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(token)
            is Resource.Success -> wrapInResource {
                userRemoteSource.createUser(userRequestModel, token.data)
            }
        }

    private suspend fun getUserRepositorySession(): Resource<SessionToken> =
        session?.let { session ->
            if (!session.hasExpired()) {
                Resource.Success(session)
            } else {
                loginClient()
            }
        } ?: loginClient()

    suspend fun updateEmail(newEmail: String, oldEmail: String, session: SessionToken): Resource<Unit> =
        when (val authToken = getUserRepositorySession()) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(authToken)
            is Resource.Success -> wrapInResource {
                val userId = userRemoteSource.getGiniApiSessionTokenInfo(session.accessToken, authToken.data).userName
                userRemoteSource.updateEmail(userId, UserRequestModel(email = newEmail, oldEmail = oldEmail), authToken.data)
            }
        }
    //endregion
}
