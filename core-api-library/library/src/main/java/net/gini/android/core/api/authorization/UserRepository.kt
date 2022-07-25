package net.gini.android.core.api.authorization

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.NoInternetException
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository(override val coroutineContext: CoroutineContext) : CoroutineScope, KSessionManager {
    val userRemoteSource = UserRemoteSource()

    //region Public methods
    suspend fun loginUser(userRequestModel: UserRequestModel): User? =
        when (val response = signIn(userRequestModel)) {
            is Resource.Success -> {
                response.data!!
            }

            is Resource.Error -> {
                null
            }
        }
    //endregion

    //region Private methods
    private suspend fun signIn(userRequestModel: UserRequestModel): Resource<User> =
        wrapResponseIntoResource {
            userRemoteSource.signIn(userRequestModel)
        }

    private suspend fun loginClient(): Resource<Session> =
        wrapResponseIntoResource {
            userRemoteSource.loginClient()
        }

    private suspend fun createUser(userRequestModel: UserRequestModel): Resource<User> =
        wrapResponseIntoResource {
            userRemoteSource.createUser(userRequestModel)
        }
    //endregion

    //region Overridden methods
    override suspend fun getSession(): Session {
        return Session("", Date())
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
