package net.gini.android.core.api.authorization

import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.SessionTokenInfo
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
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
            userService.signIn(basicHeaderMap(), userRequestModel.email ?: "", userRequestModel.password ?: "")
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun loginClient(): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.loginClient(basicHeaderMap())
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun createUser(userRequestModel: UserRequestModel, sessionToken: SessionToken): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            userService.createUser(bearerHeaderMap(sessionToken), userRequestModel)
        }
    }

    suspend fun getGiniApiSessionTokenInfo(token: String, authSessionToken: SessionToken): SessionTokenInfo = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
           userService.getGiniApiSessionTokenInfo(bearerHeaderMap(authSessionToken), token)
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun getUserInfo(uri: String, sessionToken: SessionToken): UserResponseModel = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.getUserInfo(bearerHeaderMap(sessionToken), uri)
        }
        response.body() ?: throw ApiException("Empty response body", response)
    }

    suspend fun updateEmail(userId: String, userRequestModel: UserRequestModel, sessionToken: SessionToken): Unit = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.updateEmail(bearerHeaderMap(sessionToken), userId, userRequestModel)
        }
    }

    private fun basicHeaderMap(): Map<String, String> {
        val encoded = Base64.getEncoder().encodeToString("${clientId}:${clientSecret}".toByteArray())
        return mapOf("Accept" to "application/json",
            "Authorization" to "Basic $encoded")
    }

    private fun bearerHeaderMap(sessionToken: SessionToken): Map<String, String> {
        return mapOf("Accept" to "application/json",
            "Authorization" to "BEARER ${sessionToken.accessToken}")
    }
}
