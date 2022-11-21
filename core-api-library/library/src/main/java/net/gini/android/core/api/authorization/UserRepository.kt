package net.gini.android.core.api.authorization

import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.apimodels.UserRequestModel

internal class UserRepository(
    private val userRemoteSource: UserRemoteSource
    ) {

    private var session: Session? = null

    suspend fun loginUser(userRequestModel: UserRequestModel): Resource<Session> =
        wrapInResource {
            Session.fromAPIResponse(userRemoteSource.signIn(userRequestModel))
        }

    private suspend fun loginClient(): Resource<Session> =
        wrapInResource {
            val newSession = Session.fromAPIResponse(userRemoteSource.loginClient())
            session = newSession
            newSession
        }

    suspend fun createUser(userRequestModel: UserRequestModel): Resource<Unit> =
        when (val token = getUserRepositorySession()) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(token)
            is Resource.Success -> wrapInResource {
                userRemoteSource.createUser(userRequestModel, token.data.accessToken)
            }
        }

    private suspend fun getUserRepositorySession(): Resource<Session> =
        session?.let { session ->
            if (!session.hasExpired()) {
                Resource.Success(session)
            } else {
                loginClient()
            }
        } ?: loginClient()

    suspend fun updateEmail(newEmail: String, oldEmail: String, accessToken: String): Resource<Unit> =
        when (val authToken = getUserRepositorySession()) {
            is Resource.Cancelled -> Resource.Cancelled()
            is Resource.Error -> Resource.Error(authToken)
            is Resource.Success -> wrapInResource {
                val userId = userRemoteSource.getGiniApiSessionTokenInfo(accessToken, authToken.data.accessToken).userName
                userRemoteSource.updateEmail(userId, UserRequestModel(email = newEmail, oldEmail = oldEmail), authToken.data.accessToken)
            }
        }
}
