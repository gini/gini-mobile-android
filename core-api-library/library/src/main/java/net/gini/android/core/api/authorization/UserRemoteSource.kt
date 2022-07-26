package net.gini.android.core.api.authorization

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.gini.android.core.api.RetrofitHelper
import net.gini.android.core.api.authorization.apimodels.SessionResponseModel
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import net.gini.android.core.api.requests.SafeApiRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

class UserRemoteSource {

    suspend fun signIn(userRequestModel: UserRequestModel): UserResponseModel = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().signIn(userRequestModel.username ?: "", userRequestModel.password ?: "")
        }
        response
    }

    suspend fun loginClient(): Session = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().loginClient()
        }
        response
    }

    suspend fun createUser(userRequestModel: UserRequestModel): UserResponseModel = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().createUser(userRequestModel)
        }
        response
    }

    suspend fun getGiniApiSessionTokenInfo(token: String): SessionResponseModel = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().getGiniApiSessionTokenInfo(token)
        }
        response
    }

    suspend fun getUserInfo(uri: String): UserResponseModel = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().getUserInfo(uri)
        }
        response
    }

    suspend fun updateEmail(userId: String, userRequestModel: UserRequestModel): ResponseBody = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().updateEmail(userId, userRequestModel)
        }
        response
    }
}
