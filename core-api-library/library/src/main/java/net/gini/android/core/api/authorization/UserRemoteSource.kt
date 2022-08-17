package net.gini.android.core.api.authorization

import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import net.gini.android.core.api.requests.SafeApiRequest
import okhttp3.ResponseBody
import java.util.*
import kotlin.coroutines.CoroutineContext

class UserRemoteSource(
    val coroutineContext: CoroutineContext,
    private val userService: UserService,
    private val clientId: String,
    private val clientSecret: String
) {

    suspend fun signIn(userRequestModel: UserRequestModel): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.signIn(basicHeaderMap(), userRequestModel.username ?: "", userRequestModel.password ?: "")
        }
        response
    }

    suspend fun loginClient(): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.loginClient(basicHeaderMap())
        }
        response
    }

    suspend fun createUser(userRequestModel: UserRequestModel): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.createUser(bearerHeaderMap(), userRequestModel)
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
            userService.getUserInfo(bearerHeaderMap(), uri)
        }
        response
    }

    suspend fun updateEmail(userId: String, userRequestModel: UserRequestModel, sessionToken: SessionToken): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.updateEmail(bearerHeaderMap(), userId, userRequestModel)
        }
        response
    }

    private fun basicHeaderMap(): Map<String, String> {
        val encoded = Base64.getEncoder().encodeToString("${clientId}:${clientSecret}".toByteArray())
        return mapOf("Accept" to "application/json",
            "Authorization" to "Basic $encoded")
    }

    private fun bearerHeaderMap(): Map<String, String> {

        // TODO: pass session into class
        val sessionToken = ""
        return mapOf("Accept" to "application/json",
            "Authorization" to "BEARER $sessionToken")
    }
}
