package net.gini.android.core.api.authorization

import android.util.Base64
import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.BasicAuthorizatonHeader
import net.gini.android.core.api.requests.BearerAuthorizatonHeader
import net.gini.android.core.api.requests.JsonAcceptHeader
import net.gini.android.core.api.requests.SafeApiRequest
import kotlin.coroutines.CoroutineContext

internal class UserRemoteSource(
    val coroutineContext: CoroutineContext,
    private val userService: UserService,
    private val clientId: String,
    private val clientSecret: String
) {

    suspend fun signIn(userRequestModel: UserRequestModel): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.signIn(basicHeaderMap(), userRequestModel.email ?: "", userRequestModel.password ?: "")
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun loginClient(): SessionToken = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.loginClient(basicHeaderMap())
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun createUser(userRequestModel: UserRequestModel, accessToken: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            userService.createUser(bearerHeaderMap(accessToken), userRequestModel)
        }
    }

    suspend fun getUserInfo(uri: String, accessToken: String): UserResponseModel = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.getUserInfo(bearerHeaderMap(accessToken), uri)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun updateEmail(userId: String, userRequestModel: UserRequestModel, accessToken: String): Unit = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            userService.updateEmail(bearerHeaderMap(accessToken), userId, userRequestModel)
        }
    }

    private fun basicHeaderMap(): Map<String, String> {
        val encoded = Base64.encodeToString("${clientId}:${clientSecret}".toByteArray(), Base64.NO_WRAP)
        return mapOf(JsonAcceptHeader().toPair(),
            BasicAuthorizatonHeader(encoded).toPair())
    }

    private fun bearerHeaderMap(accessToken: String): Map<String, String> {
        return mapOf(JsonAcceptHeader().toPair(),
            BearerAuthorizatonHeader(accessToken).toPair())
    }
}
