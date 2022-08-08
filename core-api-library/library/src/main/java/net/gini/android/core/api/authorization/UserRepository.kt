package net.gini.android.core.api.authorization

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.requests.ApiException
import okhttp3.ResponseBody
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class UserRepository(
    override val coroutineContext: CoroutineContext,
    private var session: SessionToken? = null,
    private val userRemoteSource: UserRemoteSource
    ) : CoroutineScope {
    //region Public methods
    suspend fun loginUser(userRequestModel: UserRequestModel): Resource<SessionToken> =
        wrapResponseIntoResource {
            userRemoteSource.signIn(userRequestModel)
        }

    suspend fun loginClient(): Flow<Resource<SessionToken>> =
        flow {
            emit(wrapResponseIntoResource {
                userRemoteSource.loginClient()
            })
        }

    suspend fun createUser(userRequestModel: UserRequestModel): Flow<Resource<ResponseBody>> =
        flow {
            emit (wrapResponseIntoResource {
                userRemoteSource.createUser(userRequestModel)
            })
        }

    suspend fun loginClientForSession(): Flow<SessionToken?> =
        flow {
            val result = loginClient()
            if (result.first() is Resource.Success) {
                session = result.first().data!!
                emit(session)
            }
        }

    suspend fun getUserRepositorySession(): Flow<SessionToken?> =
        flow {
            if (session?.hasExpired() == false || session != null) {
                emit(session)
            } else {
                emit(loginClientForSession().firstOrNull())
            }
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
