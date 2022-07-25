package net.gini.android.core.api.requests

import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.util.concurrent.CancellationException

private const val ERROR_MESSAGE_KEY = "message"
private const val ERROR_CODE_KEY = "code"

object SafeApiRequest {
    @Throws(ApiException::class)
    suspend fun <T : Any?> apiRequest(call: suspend () -> Response<T>): T {
        try {
            val response = call.invoke()
            if (response.isSuccessful) {
                return response.body()!!
            }

            val error = response.errorBody()?.string()
            val message = StringBuilder()
            var errorCode: Int = 0
            error?.let {
                try {
                    message.append(JSONObject(it).getString(ERROR_MESSAGE_KEY))
                    errorCode = JSONObject(it).getString(ERROR_CODE_KEY).toInt()
                } catch (e: JSONException) {
                }
            }

            throw ApiException(message.toString())
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> throw e
                else -> throw ApiException(e.message ?: "")
            }
        }
    }
}
