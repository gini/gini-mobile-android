package net.gini.android.core.api.authorization

import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import net.gini.android.core.api.requests.SafeApiRequest
import okhttp3.ResponseBody
import kotlin.coroutines.CoroutineContext

class UserRemoteSource(
    val coroutineContext: CoroutineContext,
    private val userService: UserService
) {

    suspend fun signIn(userRequestModel: UserRequestModel): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.signIn(mutableMapOf(), userRequestModel.username ?: "", userRequestModel.password ?: "")
        }
        response
    }

    suspend fun loginClient(): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.loginClient(mutableMapOf())
        }
        response
    }

    suspend fun createUser(userRequestModel: UserRequestModel): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.createUser(mutableMapOf(), userRequestModel)
        }
        response
    }

    suspend fun getGiniApiSessionTokenInfo(token: String): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
           userService.getGiniApiSessionTokenInfo(token)
        }
        response
    }

    suspend fun getUserInfo(uri: String): UserResponseModel = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.getUserInfo(mutableMapOf(), uri)
        }
        response
    }

    suspend fun updateEmail(userId: String, userRequestModel: UserRequestModel): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.updateEmail(userId, userRequestModel)
        }
        response
    }


}
