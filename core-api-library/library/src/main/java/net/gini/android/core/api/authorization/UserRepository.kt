package net.gini.android.core.api.authorization

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.requests.ApiException
import okhttp3.ResponseBody
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

open class UserRepository(
    override val coroutineContext: CoroutineContext,
    private val userRemoteSource: UserRemoteSource
    ) : CoroutineScope {

    private var session: SessionToken? = null

    //region Public methods
    suspend fun loginUser(userRequestModel: UserRequestModel): Resource<SessionToken?> =
        wrapResponseIntoResource {
            userRemoteSource.signIn(userRequestModel)
        }

    suspend fun loginClient(): Resource<SessionToken> =
        wrapResponseIntoResource {
            userRemoteSource.loginClient()
        }

    suspend fun createUser(userRequestModel: UserRequestModel): Resource<ResponseBody> =
        wrapResponseIntoResource {
            val token = getUserRepositorySession() ?: throw ApiException()
            userRemoteSource.createUser(userRequestModel, token)
        }

    suspend fun getUserRepositorySession(): SessionToken? =
            if (session?.hasExpired() == false || session != null) {
                session
            } else {
                loginClient().data
            }

    suspend fun updateEmail(newEmail: String, oldEmail: String, session: SessionToken): Resource<ResponseBody>? =
        getUserRepositorySession()?.accessToken?.let { token ->
            val userId = userRemoteSource.getGiniApiSessionTokenInfo(token).userName
            wrapResponseIntoResource {
                userRemoteSource.updateEmail(userId ?: "", UserRequestModel(newEmail = newEmail, oldEmail = oldEmail), session)
            }

        }

    fun foo(): Int {
        throw Exception("Foo exception")
    }
    //endregion

    companion object {
        private suspend fun <T> wrapResponseIntoResource(request: suspend () -> T) =
            try {
                Resource.Success(request())
            } catch (e: ApiException) {
                Resource.Error(e.message ?: "")
            } catch (e: CancellationException) {
                Resource.Cancelled()
            }
    }
}
