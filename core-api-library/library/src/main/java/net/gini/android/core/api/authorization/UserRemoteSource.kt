package net.gini.android.core.api.authorization

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.gini.android.core.api.RetrofitHelper
import net.gini.android.core.api.requests.SafeApiRequest

class UserRemoteSource {

    suspend fun signIn(userRequestModel: UserRequestModel): User = withContext(Dispatchers.IO) {
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

    suspend fun createUser(userRequestModel: UserRequestModel): User = withContext(Dispatchers.IO) {
        val response = SafeApiRequest.apiRequest {
            RetrofitHelper.provideUserService().createUser(userRequestModel)
        }
        response
    }
}
