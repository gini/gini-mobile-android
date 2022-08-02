package net.gini.android.core.api.authorization

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.NoInternetException
import kotlin.coroutines.CoroutineContext

class UserRepository(
    override val coroutineContext: CoroutineContext,
    private var session: Session? = null
    ) : CoroutineScope {
    val userRemoteSource = UserRemoteSource()

    //region Public methods
    suspend fun loginUser(userRequestModel: UserRequestModel): Resource<UserResponseModel> =
        wrapResponseIntoResource {
            userRemoteSource.signIn(userRequestModel)
        }

    suspend fun loginClient(): Flow<Resource<Session>> =
        flow {
            emit(wrapResponseIntoResource {
                userRemoteSource.loginClient()
            })
        }

    suspend fun createUser(userRequestModel: UserRequestModel): Flow<Resource<UserResponseModel>> =
        flow {
            emit (wrapResponseIntoResource {
                userRemoteSource.createUser(userRequestModel)
            })
        }

    suspend fun loginClientForSession(): Flow<Session?> =
        flow {
            val result = loginClient()
            if (result.first() is Resource.Success) {
                session = result.first().data!!
                emit(session)
            }
        }

    suspend fun getUserRepositorySession(): Flow<Session?> =
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
            } catch (e: NoInternetException) {
                Resource.Error(e.message ?: "")
            }
    }
}
