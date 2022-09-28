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
        response.first
    }

    suspend fun loginClient(): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.loginClient(basicHeaderMap())
        }
        response.first
    }

    suspend fun createUser(userRequestModel: UserRequestModel, sessionToken: SessionToken): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.createUser(bearerHeaderMap(sessionToken), userRequestModel)
        }
        response.first
    }

    suspend fun getGiniApiSessionTokenInfo(token: String): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
           userService.getGiniApiSessionTokenInfo(token)
        }
        response.first
    }

    suspend fun getUserInfo(uri: String, sessionToken: SessionToken): UserResponseModel = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.getUserInfo(bearerHeaderMap(sessionToken), uri)
        }
        response.first
    }

    suspend fun updateEmail(userId: String, userRequestModel: UserRequestModel, sessionToken: SessionToken): ResponseBody = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.updateEmail(bearerHeaderMap(sessionToken), userId, userRequestModel)
        }
        response.first
    }

    private fun basicHeaderMap(): Map<String, String> {
        val encoded = Base64.getEncoder().encodeToString("${clientId}:${clientSecret}".toByteArray())
        return mapOf("Accept" to "application/json",
            "Authorization" to "Basic $encoded")
    }

    private fun bearerHeaderMap(sessionToken: SessionToken): Map<String, String> {
        return mapOf("Accept" to "application/json",
            "Authorization" to "BEARER $sessionToken")
    }
}
